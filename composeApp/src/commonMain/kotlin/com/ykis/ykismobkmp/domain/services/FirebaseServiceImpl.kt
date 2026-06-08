package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.Constants.TERMS_ACCEPTED_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val className = "FirebaseServiceImpl"

/**
 * [FirebaseServiceImpl] — Очищенная реализация сервиса авторизации.
 * УБРАНЫ: ApartmentService и ChatRepository для разрыва круговых зависимостей.
 */
class FirebaseServiceImpl(
  private val settings: Settings
) : FirebaseService {

  // РЕШЕНИЕ: Разрываем круговую зависимость через ленивый инжект внутри методов
  private val apartmentService: com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService get() = org.koin.mp.KoinPlatform.getKoin().get()

  private val auth get() = Firebase.auth
  private val db get() = Firebase.firestore
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
    val googleCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    auth.signInWithCredential(googleCredential)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: "Google Auth Failed")
  }

  override suspend fun addUserFirestore(): addUserFirestoreResponse {
    try {
      val currentUser = auth.currentUser ?: return Resource.Error(message = "No user")
      val currentUid = currentUser.uid
      val userEmail = currentUser.email?.takeIf { it.isNotBlank() } ?: currentUser.phoneNumber ?: ""
      
      // ИСПРАВЛЕНО: Если пользователь уже есть — полностью выходим, ничего не меняя
      val userDoc = db.collection("users").document(currentUid).get()
      if (userDoc.exists) {
          println("[FirebaseServiceImpl]: Профіль $userEmail вже існує. Пропуск створення.")
          return Resource.Success(true)
      }

      println("[FirebaseServiceImpl]: Створення НОВОГО профілю для $userEmail")
      val userMap = mapOf(
        "uid" to currentUid,
        "email" to userEmail,
        "displayName" to (currentUser.displayName ?: "Мешканець"),
        "userRole" to "STANDARD_USER",
        "osbbId" to 0L,
        "addressId" to 0L
      )

      db.collection("users").document(currentUid).set(data = userMap, merge = true)
      
      // Синхронизация с MySQL Южного (только для новых пользователей)
      try {
          apartmentService.saveUserUid(currentUid, userEmail).filter { it !is Resource.Loading }.first()
      } catch (e: Exception) {
          println("[FirebaseServiceImpl]: MySQL sync failed: ${e.message}")
      }
      
      return Resource.Success(true)
    } catch (e: Exception) {
      return Resource.Error(message = e.message ?: "Process error")
    }
  }

  override suspend fun updateUserRoleAndPermissions(uid: String, addressId: Long?, userRole: UserRole, osbbId: Long?, displayName: String?, fio: String?) {
    try {
      val updates = mutableMapOf<String, Any>("userRole" to userRole.getSerialName(), "osbbId" to (osbbId ?: 0L))
      displayName?.let { updates["displayName"] = it }
      addressId?.let { updates["addressId"] = it }
      fio?.let { updates["fio"] = it }
      db.collection("users").document(uid).set(data = updates, merge = true)
    } catch (e: Exception) { }
  }

  override suspend fun getUserProfile(): UserFirebase = withContext(Dispatchers.Default) {
    try {
      val snapshot = db.collection("users").document(uid).get()
      UserFirebase(
        uid = uid,
        email = auth.currentUser?.email ?: "",
        isEmailVerification = auth.currentUser?.isEmailVerified ?: false,
        name = snapshot.get<String>("displayName") ?: auth.currentUser?.displayName,
        userRole = snapshot.get<String>("userRole") ?: UserRole.StandardUser.name,
        osbbId = snapshot.get<Long>("osbbId") ?: snapshot.get<Long>("osbb") ?: 0L,
        addressId = snapshot.get<Long>("addressId") ?: 0L,
        fio = try { snapshot.get<String>("fio") ?: "" } catch (e: Exception) { "" }
      )
    } catch (e: Exception) {
      UserFirebase(uid = uid, email = email, isEmailVerification = false, name = "", userRole = UserRole.StandardUser.name, osbbId = 0L, addressId = 0L)
    }
  }

  override suspend fun revokeAccess(): Resource<Boolean> = withContext(Dispatchers.Default) {
    try {
      val user = auth.currentUser ?: throw Exception("Auth session expired")
      val currentUid = user.uid
      user.delete()
      db.collection("users").document(currentUid).delete()
      Resource.Success(true)
    } catch (e: Exception) {
      Resource.Error(message = e.message ?: "Unknown deletion error")
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
      // ИСПРАВЛЕНО: Теперь этот метод вызывает закрытие во всех моделях через Koin
      try {
          val chatModel: com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel = org.koin.mp.KoinPlatform.getKoin().get()
          chatModel.stopAllListeners()
          
          val announcementModel: com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel = org.koin.mp.KoinPlatform.getKoin().get()
          announcementModel.stopAllListeners()
          
          println("[FirebaseServiceImpl]: Всі слухачі КМР-моделей успішно зупинені.")
      } catch (e: Exception) {
          println("[FirebaseServiceImpl_WARN]: Помилка при зупинці слухачів: ${e.message}")
      }
  }

  override suspend fun reloadFirebaseUser(): Resource<Boolean> = try {
    val user = auth.currentUser
    if (user != null) {
      user.reload()
      Resource.Success(auth.currentUser?.isEmailVerified == true)
    } else {
      Resource.Error(message = "Користувач не знайдений")
    }
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: "Помилка оновлення сесії")
  }

  override suspend fun authenticate(email: String, password: String) { auth.signInWithEmailAndPassword(email, password) }
  override suspend fun sendRecoveryEmail(email: String) { auth.sendPasswordResetEmail(email) }
  override suspend fun linkAccount(email: String, password: String) { }
  override suspend fun deleteAccount() { auth.currentUser?.delete() }

  override suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse = try {
    auth.createUserWithEmailAndPassword(email, password)
    addUserFirestore()
    
    // ИСПРАВЛЕНО: Только одна попытка отправки
    try {
        println("[FirebaseServiceImpl]: Запит на відправку верифікації для $email...")
        auth.currentUser?.sendEmailVerification()
        println("[FirebaseServiceImpl]: Верифікація успішно ініційована.")
    } catch (e: Exception) {
        println("[FirebaseServiceImpl_ERROR]: Помилка відправки листа: ${e.message}")
    }

    Resource.Success(true)
  } catch (e: Exception) {
    println("[FirebaseServiceImpl_ERROR]: Помилка реєстрації: ${e.message}")
    Resource.Error(message = e.message ?: "Registration Failed")
  }

  override suspend fun sendEmailVerification(): SendEmailVerificationResponse = try {
    auth.currentUser?.let { 
        println("[FirebaseServiceImpl]: Ручний перезапит верифікації...")
        it.reload()
        it.sendEmailVerification()
        Resource.Success(true) 
    } ?: Resource.Error(message = "No user")
  } catch (e: Exception) {
    println("[FirebaseServiceImpl_ERROR]: ${e.message}")
    Resource.Error(message = e.message ?: "Error")
  }

  override suspend fun sendSmsCode(phoneNumber: String, platformActivity: Any?): Resource<String> = performPlatformSendSms(auth, phoneNumber, platformActivity)
  override suspend fun signInWithSmsCode(verificationId: String, smsCode: String): Resource<Boolean> = performPlatformSignInWithSms(auth, verificationId, smsCode)

  override suspend fun sendPasswordResetEmail(email: String): SendPasswordResetEmailResponse = try {
    auth.sendPasswordResetEmail(email)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: "Error")
  }

  override fun getProvider(viewModelScope: CoroutineScope): String = auth.currentUser?.providerId ?: "password"

  override suspend fun addFcmToken() {
    try {
      val token = getPlatformFcmToken() ?: return
      val updates = mapOf("fcmTokens" to dev.gitlive.firebase.firestore.FieldValue.arrayUnion(token))
      db.collection("users").document(uid).update(updates)
    } catch (e: Exception) { }
  }

  override suspend fun removeFcmToken() {
    try {
      val token = getPlatformFcmToken() ?: return
      val updates = mapOf("fcmTokens" to dev.gitlive.firebase.firestore.FieldValue.arrayRemove(token))
      db.collection("users").document(uid).update(updates)
    } catch (e: Exception) { }
  }

  override fun clearNotifications(chatId: String?) { performPlatformClearNotifications(chatId) }
}

expect suspend fun getPlatformFcmToken(): String?
expect fun performPlatformClearNotifications(chatId: String?)
expect suspend fun performPlatformSendSms(auth: dev.gitlive.firebase.auth.FirebaseAuth, phoneNumber: String, platformActivity: Any?): Resource<String>
expect suspend fun performPlatformSignInWithSms(auth: dev.gitlive.firebase.auth.FirebaseAuth, verificationId: String, smsCode: String): Resource<Boolean>
