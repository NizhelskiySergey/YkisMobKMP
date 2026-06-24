package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.Constants.TERMS_ACCEPTED_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.performPlatformSignInWithApple
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

private const val className = "FirebaseServiceImpl"

class FirebaseServiceImpl(
  private val settings: Settings
) : FirebaseService {

  private val koin get() = org.koin.mp.KoinPlatform.getKoin()
  
  private val apartmentService: com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService get() = koin.get()
  private val auth: FirebaseAuth get() = try { koin.get() } catch (e: Exception) { Firebase.auth }
  private val db: FirebaseFirestore get() = try { koin.get() } catch (e: Exception) { Firebase.firestore }
  private val remoteConfig get() = Firebase.remoteConfig

  override val isUserAuthenticatedInFirebase: Boolean get() = auth.currentUser != null
  override val uid: String get() = auth.currentUser?.uid ?: ""
  override val hasUser: Boolean get() = auth.currentUser != null
  override val isEmailVerified: Boolean? get() = auth.currentUser?.isEmailVerified
  override val currentUser: FirebaseUser? get() = auth.currentUser
  override val displayName: String get() = auth.currentUser?.displayName ?: "Користувач ЮКІС"
  
  override val email: String get() {
    val user = auth.currentUser
    return when {
      !user?.email.isNullOrBlank() -> user?.email ?: ""
      !user?.phoneNumber.isNullOrBlank() -> user?.phoneNumber ?: ""
      else -> ""
    }
  }

  override val photoUrl: String get() = auth.currentUser?.photoURL ?: ""
  override val providerId: String get() = auth.currentUser?.providerId ?: ""
  override val isWiFiCheckConfig: Boolean get() = try { remoteConfig.getValue("loading_from_wifi").asBoolean() } catch (e: Exception) { true }
  override val isMobileCheckConfig: Boolean get() = try { remoteConfig.getValue("loading_from_mobile").asBoolean() } catch (e: Exception) { true }
  override val agreementTitle: String get() = try { remoteConfig.getValue("agreement_title").asString() } catch (e: Exception) { "Угода" }
  override val agreementText: String get() = try { remoteConfig.getValue("agreement_text").asString() } catch (e: Exception) { "" }

  override suspend fun getUid() = uid
  override suspend fun getEmail() = email
  override suspend fun getDisplayName() = displayName

  override suspend fun fetchConfiguration(): Boolean = try {
    remoteConfig.fetchAndActivate()
    true
  } catch (e: Exception) {
    false
  }

  override suspend fun isUserAgreed(): Boolean = settings.getBoolean(TERMS_ACCEPTED_KEY, false)
  override suspend fun setUserAgreed(agreed: Boolean) { settings.putBoolean(TERMS_ACCEPTED_KEY, agreed) }

  override suspend fun firebaseSignInWithEmailAndPassword(email: String, password: String) {
      auth.signInWithEmailAndPassword(email, password)
  }

  override suspend fun firebaseSignInWithGoogle(idToken: String): SignInWithGoogleResponse = try {
    val platformName = com.ykis.ykismobkmp.getPlatform().name
    val isAndroid = platformName.contains("Android", true)
    val tokenForAuth: String? = if (isAndroid) null else ""
    
    val credential = GoogleAuthProvider.credential(idToken, tokenForAuth)
    auth.signInWithCredential(credential)
    
    Resource.Success(true)
  } catch (e: Exception) {
    println("[YkisLogKMP.$className.firebaseSignInWithGoogle]: [ERROR] ${e.message}")
    Resource.Error(message = getString(Res.string.error_google_auth))
  }

  override suspend fun firebaseSignInWithApple(idToken: String, rawNonce: String?): Resource<Boolean> {
      return performPlatformSignInWithApple(auth, idToken, rawNonce)
  }

  override suspend fun addUserFirestore(): addUserFirestoreResponse {
    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)
    try {
      val currentUser = auth.currentUser ?: return Resource.Error(message = "No user")
      val currentUid = currentUser.uid
      val userEmail = currentUser.email?.takeIf { it.isNotBlank() } ?: currentUser.phoneNumber ?: ""

      println("[FirebaseServiceImpl]: [START] Синхронізація профілю для $userEmail")

      val userMap = mutableMapOf<String, Any?>(
        "uid" to currentUid,
        "email" to userEmail,
        "displayName" to (currentUser.displayName ?: "Мешканець"),
        "userRole" to "StandardUser",
        "osbbId" to (if (isWeb) 0.0 else 0L),
        "addressId" to (if (isWeb) 0.0 else 0L)
      )

      if (!isWeb) {
        userMap["fcmTokens"] = emptyList<String>()
      }

      println("[FirebaseServiceImpl]: [FIRESTORE_WAIT] Спроба запису (Timeout 15s)...")

      // ИСПРАВЛЕНО: Увеличиваем таймаут до 15 секунд для стабильности на Android
      withTimeout(15000) {
        db.collection("users").document(currentUid).set(data = userMap, merge = true)
      }

      println("[FirebaseServiceImpl]: [FIRESTORE_OK] Дані збережені")

      try {
        apartmentService.saveUserUid(currentUid, userEmail).filter { it !is Resource.Loading }.first()
        println("[FirebaseServiceImpl]: [MySQL_OK] UID передано")
      } catch (e: Exception) {
        println("[FirebaseServiceImpl]: [MySQL_WARN] MySQL: ${e.message}")
      }

      return Resource.Success(true)
    } catch (e: Exception) {
      println("[FirebaseServiceImpl_ERROR]: $e")
      return Resource.Error(message = e.message ?: "Process error")
    }
  }

  override suspend fun updateUserRoleAndPermissions(uid: String, addressId: Long?, userRole: UserRole, osbbId: Long?, displayName: String?, fio: String?, osbb: String?) {
    try {
      val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)
      val updates = mutableMapOf<String, Any>(
        "userRole" to userRole.getSerialName(),
        "osbbId" to (if (isWeb) (osbbId ?: 0L).toDouble() else (osbbId ?: 0L))
      )
      displayName?.let { updates["displayName"] = it }
      addressId?.let { updates["addressId"] = if (isWeb) it.toDouble() else it }
      fio?.let { updates["fio"] = it }
      osbb?.let { updates["osbb"] = it }

      withTimeout(10000) {
        db.collection("users").document(uid).set(data = updates, merge = true)
      }
    } catch (e: Exception) { }
  }

  override suspend fun getUserProfile(): UserFirebase = withContext(Dispatchers.Default) {
    try {
      // ИСПРАВЛЕНО: Добавляем таймаут и на чтение профиля
      val snapshot = withTimeout(10000) {
        db.collection("users").document(uid).get()
      }

      UserFirebase(
        uid = uid,
        email = auth.currentUser?.email ?: "",
        isEmailVerification = auth.currentUser?.isEmailVerified ?: false,
        name = snapshot.get<String>("displayName") ?: auth.currentUser?.displayName,
        userRole = snapshot.get<String>("userRole") ?: "StandardUser",
        osbbId = snapshot.get<Long>("osbbId") ?: 0L,
        addressId = snapshot.get<Long>("addressId") ?: 0L,
        fio = try { snapshot.get<String>("fio") ?: "" } catch (e: Exception) { "" },
        osbb = try { snapshot.get<String>("osbb") ?: "" } catch (e: Exception) { "" }
      )
    } catch (e: Exception) {
      println("[FirebaseServiceImpl]: Помилка читання профілю, fallback до StandardUser: ${e.message}")
      UserFirebase(uid = uid, email = email, userRole = "StandardUser")
    }
  }

  override suspend fun revokeAccess(): Resource<Boolean> = withContext(Dispatchers.Default) {
    try {
      val user = auth.currentUser ?: throw Exception(getString(Res.string.error_auth_session_expired))
      val currentUid = user.uid
      if (currentUid.isBlank()) throw Exception(getString(Res.string.error_empty_uid))
      user.delete()
      db.collection("users").document(currentUid).delete()
      Resource.Success(true)
    } catch (e: Exception) {
      Resource.Error(message = e.message ?: getString(Res.string.error_unknown_deletion))
    }
  }

  override fun getAuthState(viewModelScope: CoroutineScope): AuthStateResponse {
    return callbackFlow {
      val job = launch { auth.authStateChanged.collect { user -> trySend(user != null) } }
      awaitClose { job.cancel() }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = auth.currentUser != null)
  }

  override suspend fun signOut() {
    try {
      stopAllListeners()
      auth.signOut() 
    } catch (e: Exception) { }
  }

  override fun stopAllListeners() {
      try {
          val chatModel: com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel = koin.get()
          chatModel.stopAllListeners()
          
          val announcementModel: com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel = koin.get()
          announcementModel.stopAllListeners()

          val apartmentModel: com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel = koin.get()
          apartmentModel.clearAllData()
      } catch (e: Exception) { }
  }

  override suspend fun reloadFirebaseUser(): Resource<Boolean> = try {
    val user = auth.currentUser
    if (user != null) {
      user.reload()
      Resource.Success(auth.currentUser?.isEmailVerified == true)
    } else {
      Resource.Error(message = getString(Res.string.no_user_identifier))
    }
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: getString(Res.string.error_session_refresh_failed))
  }

  override suspend fun authenticate(email: String, password: String) { auth.signInWithEmailAndPassword(email, password) }
  override suspend fun sendRecoveryEmail(email: String) { auth.sendPasswordResetEmail(email) }
  override suspend fun linkAccount(email: String, password: String) { }
  override suspend fun deleteAccount() { auth.currentUser?.delete() }

  override suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse = try {
    auth.createUserWithEmailAndPassword(email, password)
    addUserFirestore()
    try {
        auth.currentUser?.sendEmailVerification()
    } catch (e: Exception) { }
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = getString(Res.string.error_registration_failed))
  }

  override suspend fun sendEmailVerification(): SendEmailVerificationResponse = try {
    auth.currentUser?.let { 
        it.reload()
        it.sendEmailVerification()
        Resource.Success(true) 
    } ?: Resource.Error(message = getString(Res.string.no_user_identifier))
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: getString(Res.string.error_process))
  }

  override suspend fun sendSmsCode(phoneNumber: String, platformActivity: Any?): Resource<String> = performPlatformSendSms(auth, phoneNumber, platformActivity)
  override suspend fun signInWithSmsCode(verificationId: String, smsCode: String): Resource<Boolean> = performPlatformSignInWithSms(auth, verificationId, smsCode)

  override suspend fun sendPasswordResetEmail(email: String): SendPasswordResetEmailResponse = try {
    auth.sendPasswordResetEmail(email)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = getString(Res.string.error_process))
  }

  override fun getProvider(viewModelScope: CoroutineScope): String = auth.currentUser?.providerId ?: "password"

  override suspend fun addFcmToken() {
    try {
      val currentUid = uid
      if (currentUid.isBlank()) return
      val token = getPlatformFcmToken() ?: return
      
      println("[YkisLogKMP.$className.addFcmToken]: Реєстрація токена FCM: ${token.take(10)}...")
      
      // На Вебі використовуємо set з merge, щоб створити поле fcmTokens, якщо його немає
      val updates = mapOf("fcmTokens" to dev.gitlive.firebase.firestore.FieldValue.arrayUnion(token))
      
      withTimeout(10000) {
          db.collection("users").document(currentUid).set(data = updates, merge = true)
      }
      println("[YkisLogKMP.$className.addFcmToken]: Токен успішно збережено")
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.addFcmToken_ERROR]: ${e.message}")
    }
  }

  override suspend fun removeFcmToken() {
    try {
      val currentUid = uid
      if (currentUid.isBlank()) return
      val token = getPlatformFcmToken() ?: return
      val updates = mapOf("fcmTokens" to dev.gitlive.firebase.firestore.FieldValue.arrayRemove(token))
      db.collection("users").document(currentUid).update(updates)
    } catch (e: Exception) { }
  }

  override fun clearNotifications(chatId: String?) { performPlatformClearNotifications(chatId) }
}

expect suspend fun getPlatformFcmToken(): String?
expect fun performPlatformClearNotifications(chatId: String?)
expect suspend fun performPlatformSendSms(auth: FirebaseAuth, phoneNumber: String, platformActivity: Any?): Resource<String>
expect suspend fun performPlatformSignInWithSms(auth: FirebaseAuth, verificationId: String, smsCode: String): Resource<Boolean>
