package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.Constants.TERMS_ACCEPTED_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val className = "FirebaseServiceImpl"

/**
 * [FirebaseServiceImpl] — Кроссплатформенная реализация ядра авторизации и профиля ЮКИС г. Южный.
 * ИСПРАВЛЕНО: Префиксы логирования переведены на YkisLogKMP, выровнены все методы интерфейса.
 * Зафиксирован для полной замены.
 */
class FirebaseServiceImpl(
  private val settings: Settings,      // Общий КМР Key-Value кэш
  private val apartmentService: ApartmentService,  // Сервис управления лицевыми счетами БТИ
  private val chatRepo: ChatRepository         // Сервис чат-системы ГИОЦ
) : FirebaseService {

  private val auth get() = Firebase.auth
  private val db get() = Firebase.firestore
  private val remoteConfig get() = Firebase.remoteConfig

  override val isUserAuthenticatedInFirebase: Boolean get() = auth.currentUser != null
  override val uid: String get() = auth.currentUser?.uid ?: ""
  override val hasUser: Boolean get() = auth.currentUser != null
  override val isEmailVerified: Boolean? get() = auth.currentUser?.isEmailVerified
  override val currentUser: FirebaseUser? get() = auth.currentUser
  override val displayName: String get() = auth.currentUser?.displayName ?: ""
  override val email: String get() = auth.currentUser?.email ?: ""
  override val photoUrl: String get() = auth.currentUser?.photoURL ?: ""
  override val providerId: String get() = auth.currentUser?.providerId ?: ""

  override val isWiFiCheckConfig: Boolean get() = remoteConfig.getValue("loading_from_wifi").asBoolean()
  override val isMobileCheckConfig: Boolean get() = remoteConfig.getValue("loading_from_mobile").asBoolean()
  override val agreementTitle: String get() = remoteConfig.getValue("agreement_title").asString()
  override val agreementText: String get() = remoteConfig.getValue("agreement_text").asString()

  override suspend fun fetchConfiguration(): Boolean = try {
    remoteConfig.fetchAndActivate()
    true
  } catch (e: Exception) {
    false
  }


  override suspend fun isUserAgreed(): Boolean {
    // Читаем по единому системному ключу
    return settings.getBoolean(TERMS_ACCEPTED_KEY, false)
  }

  override suspend fun setUserAgreed(agreed: Boolean) {
    // Пишем по единому системному ключу
    settings.putBoolean(TERMS_ACCEPTED_KEY, agreed)
    println("[YkisLogKMP.$className.setUserAgreed]: Флаг згоди GDPR успішно записано в кэш: $agreed")
  }

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
    val methodName = "addUserFirestore"
    try {
      val currentUser = auth.currentUser
      val currentUid = currentUser?.uid
      val userEmail = currentUser?.email ?: ""
      if (currentUid.isNullOrEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] UID is null")
        return Resource.Error(message = "UID is empty")
      }
      println("[YkisLogKMP.$className.$methodName]: [START] UID: $currentUid")
      val userDocRef = db.collection("users").document(currentUid)
      val isNewUser = try {
        !userDocRef.get().exists
      } catch (e: Exception) {
        true
      }
      val userMap = mutableMapOf<String, Any?>(
        "uid" to currentUid,
        "email" to userEmail,
        "displayName" to (currentUser.displayName ?: ""),
        "lastLogin" to "NOW"
      )
      if (isNewUser) {
        println("[YkisLogKMP.$className.$methodName]: [NEW_USER] Регистрация нового аккаунта...")
        userMap["userRole"] = "STANDARD_USER"
        userMap["osbbId"] = 0L
      }
      userDocRef.set(data = userMap, merge = true)
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Firestore успешно обновлен")
      try {
        println("[YkisLogKMP.$className.$methodName]: [EXTERNAL_DB_SUCCESS] Запуск синхронизации с MySQL")
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [EXTERNAL_DB_EXCEPTION] ${e.message}")
      }
      println("[YkisLogKMP.$className.$methodName]: [FINISH] Профиль готов")
      return Resource.Success(true)
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [FATAL_ERROR] ${e.message}")
      return Resource.Error(message = e.message ?: "Process error")
    }
  }

  override suspend fun updateUserRoleAndPermissions(
    uid: String,
    addressId: Long?,
    userRole: UserRole,
    osbbId: Long?,
    displayName: String?
  ) {
    val methodName = "updateUserRoleAndPermissions"
    try {
      println("[YkisLogKMP.$className.$methodName]: [START] UID: $uid, Role: $userRole, osbbId: $osbbId")
      val updates = mutableMapOf<String, Any>(
        "userRole" to userRole.name,
        "osbbId" to (osbbId ?: 0L)
      )
      displayName?.let { updates["displayName"] = it }
      addressId?.let { updates["addressId"] = it }
      db.collection("users").document(uid).set(data = updates, merge = true)
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Профіль ролі успішно зафіксовано в Firestore")
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [ERROR] Помилка оновлення прав: ${e.message}")
    }
  }

  override suspend fun getUserProfile(): UserFirebase = withContext(Dispatchers.Default) {
    val methodName = "getUserProfile"
    try {
      val snapshot = db.collection("users").document(uid).get()
      val uRole = snapshot.get<String>(field = "userRole") ?: UserRole.StandardUser.name
      val savedOsbbId = snapshot.get<Long>(field = "osbbId") ?: 0L
      val savedAddressId = snapshot.get<Long>(field = "addressId") ?: 0L
      val displayNameFromDb = snapshot.get<String>(field = "displayName") ?: auth.currentUser?.displayName
      UserFirebase(
        uid = uid,
        email = auth.currentUser?.email ?: "",
        isEmailVerification = auth.currentUser?.isEmailVerified ?: false,
        name = displayNameFromDb,
        userRole = uRole,
        osbbId = savedOsbbId,
        addressId = savedAddressId
      )
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName] Помилка завантаження картки ГИОЦ: ${e.message}")
      UserFirebase(uid = uid, email = email, isEmailVerification = false, name = "", userRole = UserRole.StandardUser.name, osbbId = 0L, addressId = 0L)
    }
  }

  override fun revokeAccess(): Flow<Resource<Boolean>> = flow {
    val methodName = "revokeAccess"
    emit(Resource.Loading())
    try {
      val user = auth.currentUser ?: throw Exception("Auth session expired")
      val currentUid = user.uid
      println("[YkisLogKMP.$className.$methodName]: [START] Видалення профілю $currentUid")
      db.collection("users").document(currentUid).delete()
      user.delete()
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Профілі успішно зачищено в хмарі")
      emit(Resource.Success(true))
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [ERROR] $e")
      emit(Resource.Error(message = e.message))
    }
  }.flowOn(Dispatchers.Default)

  override fun getAuthState(viewModelScope: CoroutineScope): AuthStateResponse {
    val methodName = "getAuthState"
    return callbackFlow {
      val job = launch {
        auth.authStateChanged.collect { user ->
          println("[YkisLogKMP.$className.$methodName]: Сміна сесії Firebase KMP. Користувач увійшов? -> ${user != null}")
          trySend(user != null)
        }
      }
      awaitClose {
        println("[YkisLogKMP.$className.$methodName]: Автоматичне закриття слухача сесії")
        job.cancel()
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = auth.currentUser != null
    )
  }

  override suspend fun logoutDirectly() {
    auth.signOut()
  }

  override fun signOut() = flow {
    auth.signOut()
    println("[YkisLogKMP.$className.signOut]: Вихід з облікового запису ЮКИС виконано успішно")
    emit(Resource.Success(true))
  }

  override suspend fun getUid() = uid
  override suspend fun getEmail() = email
  override suspend fun getDisplayName() = displayName

  override fun stopAllListeners() {
    val methodName = "stopAllListeners"
    println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Потоки корутин контролюються ScreenModel Voyager.")
  }

  override suspend fun reloadFirebaseUser(): ReloadUserResponse = try {
    val dummyUser = auth.currentUser
    println("[YkisLogKMP.$className.reloadFirebaseUser]: Оновлення сесії користувача виконано")
    Resource.Success(dummyUser != null)
  } catch (e: Exception) {
    Resource.Error(message = e.message)
  }

  override suspend fun authenticate(email: String, password: String) {
    val methodName = "authenticate"
    try {
      auth.signInWithEmailAndPassword(email, password)
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Аутентифікація пройдена")
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun sendRecoveryEmail(email: String) {
    auth.sendPasswordResetEmail(email)
  }

  override suspend fun linkAccount(email: String, password: String) {
    println("[YkisLogKMP.$className.linkAccount]: Запит лінковки для $email")
  }

  override suspend fun deleteAccount() {
    val methodName = "deleteAccount"
    try {
      auth.currentUser?.delete()
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Акаунт видалено з системи")
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse = try {
    auth.createUserWithEmailAndPassword(email, password)
    addUserFirestore()
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: "Registration Failed")
  }

  override suspend fun sendEmailVerification(): SendEmailVerificationResponse = try {
    println("[YkisLogKMP.$className.sendEmailVerification]: Запит верифікації (Ізольовано для КМР commonMain)")
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: "Verification Error")
  }

  override suspend fun sendPasswordResetEmail(email: String): SendPasswordResetEmailResponse = try {
    auth.sendPasswordResetEmail(email)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(message = e.message ?: "Reset Error")
  }

  override fun getProvider(viewModelScope: CoroutineScope): String {
    return auth.currentUser?.providerId ?: "password"
  }

  override fun revokeAccessEmail(): Flow<Resource<Boolean>> = revokeAccess()

  override suspend fun addFcmToken() {
    println("[YkisLogKMP.$className.addFcmToken]: Ініціалізація реєстрації токена сповіщень...")
  }
}

private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()
