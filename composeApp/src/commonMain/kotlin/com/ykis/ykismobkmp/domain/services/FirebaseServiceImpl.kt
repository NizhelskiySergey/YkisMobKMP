package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.Constants.TERMS_ACCEPTED_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.performPlatformSignInWithApple
import com.ykis.ykismobkmp.core.utils.performPlatformSendSms
import com.ykis.ykismobkmp.core.utils.performPlatformSignInWithSms
import com.ykis.ykismobkmp.core.utils.getPlatformFcmToken
import com.ykis.ykismobkmp.core.utils.performPlatformClearNotifications
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.getString
import com.ykis.ykismobkmp.*

private const val className = "FirebaseServiceImpl"

class FirebaseServiceImpl(
  private val settings: Settings,
) : FirebaseService {

  private val koin get() = org.koin.mp.KoinPlatform.getKoin()
  
  private val apartmentService: com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService get() = koin.get()
  private val auth: FirebaseAuth get() = try { koin.get() } catch (_: Exception) { Firebase.auth }
  private val db: FirebaseFirestore get() = try { koin.get() } catch (_: Exception) { Firebase.firestore }
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
      user?.email?.isNotBlank() == true -> user.email ?: ""
      user?.phoneNumber?.isNotBlank() == true -> user.phoneNumber ?: ""
      else -> ""
    }
  }

  override val photoUrl: String get() = auth.currentUser?.photoURL ?: ""
  override val providerId: String get() = auth.currentUser?.providerId ?: ""
  override val isWiFiCheckConfig: Boolean get() = try { remoteConfig.getValue("loading_from_wifi").asBoolean() } catch (e: Exception) { true }
  override val isMobileCheckConfig: Boolean get() = try { remoteConfig.getValue("loading_from_mobile").asBoolean() } catch (e: Exception) { true }
  override val agreementTitle: String get() = try { remoteConfig.getValue("agreement_title").asString() } catch (e: Exception) { "Угода" }
  override val agreementText: String get() = try { 
    val raw = remoteConfig.getValue("agreement_text").asString()
    if (raw.trim().startsWith("[")) {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val lines = json.decodeFromString<List<String>>(raw)
        lines.joinToString("\n")
    } else {
        raw
    }
  } catch (e: Exception) { "" }

  override suspend fun getUid() = uid
  override suspend fun getEmail() = email
  override suspend fun getDisplayName() = displayName

  override suspend fun fetchConfiguration(): Boolean = try {
    try {
        remoteConfig.fetch(0.seconds)
        remoteConfig.activate()
    } catch (e: Exception) {
        remoteConfig.fetchAndActivate()
    }
    true
  } catch (e: Exception) {
    false
  }

  override suspend fun isUserAgreed(): Boolean = settings.getBoolean(TERMS_ACCEPTED_KEY, defaultValue = false)
  override suspend fun setUserAgreed(agreed: Boolean) { settings.putBoolean(TERMS_ACCEPTED_KEY, agreed) }

  override suspend fun firebaseSignInWithEmailAndPassword(email: String, password: String) {
      try {
          println("[YkisLogKMP.$className.firebaseSignInWithEmailAndPassword]: Спроба входу для $email")
          auth.signInWithEmailAndPassword(email, password)
          println("[YkisLogKMP.$className.firebaseSignInWithEmailAndPassword]: Успішний вхід")
      } catch (e: Exception) {
          println("[YkisLogKMP.$className.firebaseSignInWithEmailAndPassword]: [ERROR] Помилка входу: ${e.message}")
          throw e
      }
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

  override suspend fun firebaseSignInWithApple(idToken: String, rawNonce: String?, authCode: String?): Resource<Boolean> {
      return performPlatformSignInWithApple(auth, idToken, rawNonce, authCode)
  }

  override suspend fun addUserFirestore(manualUid: String?): addUserFirestoreResponse {
    val platform = com.ykis.ykismobkmp.getPlatform().name
    val isWeb = platform.contains("Web", true)
    val isIos = platform.contains("iOS", true) || platform.contains("iPad", true)
    
    var currentUid = manualUid ?: uid

    if ((isWeb || isIos) && currentUid.isBlank()) {
        println("[YkisLogKMP.$className.addUserFirestore]: Очікування синхронізації UID для $platform...")
        var attempts = 0
        while (currentUid.isBlank() && attempts < 20) {
            delay(250)
            currentUid = uid
            attempts++
        }
    }

    if (currentUid.isBlank()) {
        println("[YkisLogKMP.$className.addUserFirestore]: [ERROR] UID порожній")
        return Resource.Error(message = "No UID")
    }

    try {
      val currentUser = auth.currentUser
      val userEmail = currentUser?.email ?: currentUser?.phoneNumber ?: ""

      println("[YkisLogKMP.$className.addUserFirestore]: [START] Синхронізація для $userEmail (UID: $currentUid)")

      val userMap = mutableMapOf<String, Any>(
        "uid" to currentUid,
        "email" to userEmail,
        "displayName" to (currentUser?.displayName ?: "Користувач"),
        "userRole" to "STANDARD_USER",
        "osbbId" to (if (isWeb) 0.0 else 0L),
        "addressId" to (if (isWeb) 0.0 else 0L)
      )

      db.collection("users").document(currentUid).set(data = userMap, merge = true)

      @OptIn(DelicateCoroutinesApi::class)
      GlobalScope.launch {
          try {
              apartmentService.saveUserUid(currentUid, userEmail).filter { it !is Resource.Loading }.first()
          } catch (e: Exception) {
              println("[YkisLogKMP.$className.addUserFirestore]: [WARN] MySQL sync failed: ${e.message}")
          }
      }

      return Resource.Success(true)
    } catch (e: Exception) {
      return Resource.Error(message = e.message ?: "Process error")
    }
  }

  override suspend fun updateUserRoleAndPermissions(uid: String, addressId: Long?, userRole: UserRole, osbbId: Long?, displayName: String?, fio: String?, osbb: String?) {
    if (uid.isBlank()) return
    try {
      val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)
      val updates = mutableMapOf<String, Any>(
        "userRole" to userRole.getSerialName()
      )
      
      addressId?.let { updates["addressId"] = if (isWeb) it.toDouble() else it }
      osbbId?.let { updates["osbbId"] = if (isWeb) it.toDouble() else it }
      displayName?.let { updates["displayName"] = it }
      fio?.let { updates["fio"] = it }
      osbb?.let { updates["osbb"] = it }

      db.collection("users").document(uid).set(data = updates, merge = true)
    } catch (e: Exception) { }
  }

  override suspend fun getUserProfile(): UserFirebase = withContext(Dispatchers.Default) {
    val currentUid = uid
    if (currentUid.isBlank()) {
        return@withContext UserFirebase(uid = "", email = "", userRole = "STANDARD_USER")
    }
    try {
      val snapshot = db.collection("users").document(currentUid).get()
      val data = snapshot.data<UserFirebase>()
      data.copy(uid = currentUid)
    } catch (e: Exception) {
      UserFirebase(uid = currentUid, email = email, userRole = "STANDARD_USER")
    }
  }

  override suspend fun revokeAccess(): Resource<Boolean> = withContext(Dispatchers.Default) {
    try {
      val user = auth.currentUser ?: throw Exception(getString(Res.string.error_auth_session_expired))
      val currentUid = user.uid
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
  override suspend fun deleteAccount() { auth.currentUser?.delete() }

  override suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse = try {
    auth.createUserWithEmailAndPassword(email, password)
    addUserFirestore()
    auth.currentUser?.sendEmailVerification()
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

  override suspend fun sendSmsCode(phoneNumber: String, platformActivity: Any?): Resource<String> {
      val cleanPhone = phoneNumber.filter { it.isDigit() }
      val fullFormattedPhoneNumber = when {
          phoneNumber.startsWith("+") -> phoneNumber
          cleanPhone.startsWith("380") -> "+$cleanPhone"
          cleanPhone.startsWith("0") -> "+380${cleanPhone.drop(1)}"
          else -> "+380$cleanPhone"
      }
      return performPlatformSendSms(auth, fullFormattedPhoneNumber, platformActivity)
  }

  override suspend fun signInWithSmsCode(verificationId: String, smsCode: String): Resource<String> = performPlatformSignInWithSms(auth, verificationId, smsCode)

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
      
      val updates = mutableMapOf<String, Any>()
      updates["fcmTokens"] = dev.gitlive.firebase.firestore.FieldValue.arrayUnion(token)
      
      db.collection("users").document(currentUid).set(data = updates, merge = true)
    } catch (e: Exception) { }
  }

  override suspend fun getManualText(role: UserRole): String = try {
    val lang = settings.getString("app_language", "uk")
    val baseKey = if (role == UserRole.StandardUser) "manual_resident" else "manual_admin"
    val fullKey = "${baseKey}_$lang"
    
    val rawValue = remoteConfig.getValue(fullKey).asString()
    val finalRaw = rawValue.ifBlank { remoteConfig.getValue(baseKey).asString() }
    
    if (finalRaw.trim().startsWith("[")) {
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
            val lines = json.decodeFromString<List<String>>(finalRaw)
            lines.joinToString("\n")
        } catch (e: Exception) {
            finalRaw
        }
    } else {
        finalRaw
    }
  } catch (e: Exception) {
    ""
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

  override suspend fun fetchAppUpdateConfig(): com.ykis.ykismobkmp.domain.entity.AppUpdateConfig? {
    return try {
        val doc = db.collection("config").document("version").get()
        if (doc.exists) {
            // Читаємо поля по одному. Це найнадійніший спосіб у KMP без проблем із серіалізацією Map
            val latest = try { doc.get<String?>("latestVersion") ?: "" } catch (e: Exception) { "" }
            val android = try { doc.get<String?>("androidUrl") ?: "" } catch (e: Exception) { "" }
            val ios = try { doc.get<String?>("iosUrl") ?: "" } catch (e: Exception) { "" }
            val web = try { doc.get<String?>("webUrl") ?: "" } catch (e: Exception) { "" }
            val critical = try { doc.get<Boolean?>("isCritical") ?: false } catch (e: Exception) { false }

            println("[YkisLogKMP.Firebase]: Config Loaded -> latest='$latest', android='$android'")

            com.ykis.ykismobkmp.domain.entity.AppUpdateConfig(
                latestVersion = latest,
                androidUrl = android,
                iosUrl = ios,
                webUrl = web,
                isCritical = critical
            )
        } else {
            println("[YkisLogKMP.Firebase]: Document config/version not found")
            null
        }
    } catch (e: Exception) {
        println("[YkisLogKMP.Firebase_ERROR]: ${e.message}")
        null
    }
  }
}
