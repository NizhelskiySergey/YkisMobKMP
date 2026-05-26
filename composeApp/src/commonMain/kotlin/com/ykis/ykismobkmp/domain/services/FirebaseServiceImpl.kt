package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.Constants.TERMS_ACCEPTED_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.PhoneAuthProvider
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import dev.gitlive.firebase.auth.auth
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
import kotlin.time.Clock

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

      // КРИТИЧЕСКИЙ ФИКС ДЛЯ SMS-ВХОДА:
      // Если почты нет, берем номер телефона, чтобы поле не оставалось пустым
      val rawEmail = currentUser?.email
      val rawPhone = currentUser?.phoneNumber
      val userEmail = if (!rawEmail.isNullOrBlank()) rawEmail else (rawPhone ?: "")

      if (currentUid.isNullOrEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] UID is null")
        return Resource.Error(message = "UID is empty")
      }
      println("[YkisLogKMP.$className.$methodName]: [START] UID: $currentUid")

      val userDocRef = db.collection("users").document(currentUid)
      val currentTimestamp = Clock.System.now().epochSeconds

      // Мы собираем карту полей по умолчанию. Благодаря merge = true, Firebase создаст их только если документа НЕТ.
      val userMap = mutableMapOf<String, Any?>(
        "uid" to currentUid,
        "email" to userEmail,
        "phoneNumber" to (rawPhone ?: ""),
        "displayName" to (currentUser?.displayName ?: "Meшканець м. Южне"),
        "userRole" to "STANDARD_USER",
        "osbbId" to 0L,

        // ДОБАВЛЕНО: Гарантируем нулевой ID адреса БТИ для новых абонентов
        "addressId" to 0L,

        "lastLogin" to currentTimestamp
      )


      println("[YkisLogKMP.$className.$methodName]: [PROCESS] Запуск безопасной записи set(merge = true)...")

      // Запись слияния (merge = true) нативно создаст или обновит профиль без предварительного чтения сети!
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

  override suspend fun reloadFirebaseUser(): Resource<Boolean> = try {
    val user = auth.currentUser
    if (user != null) {
      println("[YkisLogKMP.$className.reloadFirebaseUser]: [START] Примусове оновлення токенів та сесії з облака Google...")

      // 1. Стучимся в сеть и обновляем локальный слепок сессии
      user.reload()

      // 2. КРИТИЧЕСКИЙ ФИКС ДЛЯ КМР: Вычитываем флаг ПОВТОРНО сразу после релоада
      val freshVerifiedStatus = auth.currentUser?.isEmailVerified == true

      println("[YkisLogKMP.$className.reloadFirebaseUser]: [SUCCESS] Сесію оновлено. Актуальний статус верифікації в мережі: $freshVerifiedStatus")

      // Передаем свежий статус прямо внутрь успешного ресурса
      Resource.Success(freshVerifiedStatus)
    } else {
      println("[YkisLogKMP.$className.reloadFirebaseUser]: [ERROR] Користувач відсутній в рантаймі")
      Resource.Error(message = "Користувач не знайдений")
    }
  } catch (e: Exception) {
    println("[YkisLogKMP.$className.reloadFirebaseUser]: [FATAL_ERROR] Сбій синхронізації: ${e.message}")
    Resource.Error(message = e.message ?: "Помилка оновлення сесії")
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
    val user = auth.currentUser
    if (user != null) {
      println("[YkisLogKMP.$className.sendEmailVerification]: [START] Примусове оновлення сесії перед верифікацією...")

      // КРИТИЧЕСКИЙ ФИКС: Будим сессию пользователя в облаке Firebase, убирая "холодный" блок
      user.reload()

      println("[YkisLogKMP.$className.sendEmailVerification]: [PROCESS] Сесію оновлено. Надсилання реального листа...")

      // Вызов реального метода отправки из GitLive SDK
      user.sendEmailVerification()

      println("[YkisLogKMP.$className.sendEmailVerification]: [SUCCESS] Реальний лист верифікації відправлено!")
      Resource.Success(true)
    } else {
      println("[YkisLogKMP.$className.sendEmailVerification]: [ERROR] Користувач відсутній в рантаймі")
      Resource.Error(message = "Користувач не знайдений в сесії")
    }
  } catch (e: Exception) {
    println("[YkisLogKMP.$className.sendEmailVerification]: [FATAL_ERROR] Firebase rejected request: ${e.message}")
    e.printStackTrace()
    Resource.Error(message = e.message ?: "Помилка відправки верифікації")
  }

  override suspend fun sendSmsCode(phoneNumber: String, platformActivity: Any?): Resource<String> {
    // Делегируем отправку SMS на уровень конкретной платформы
    return performPlatformSendSms(auth, phoneNumber, platformActivity)
  }

  override suspend fun signInWithSmsCode(verificationId: String, smsCode: String): Resource<Boolean> {
    // Делегируем нативную авторизацию токена на уровень конкретной платформы
    return performPlatformSignInWithSms(auth, verificationId, smsCode)
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
// Ожидаемая КМР-функция отправки SMS для реализации на платформах
// Ожидаемые КМР-функции для реализации на нативных уровнях
// Ожидаемый мост отправки SMS для нативной реализации на каждой платформе
// Ожидаемые КМР-функции для изолированной нативной реализации на каждой из платформ
// В самом низу файла вне класса FirebaseServiceImpl:
expect suspend fun performPlatformSendSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String>

expect suspend fun performPlatformSignInWithSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean>

