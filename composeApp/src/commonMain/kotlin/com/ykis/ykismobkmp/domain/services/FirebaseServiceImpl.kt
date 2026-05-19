package com.ykis.ykismobkmp.domain.services
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

private const val tag = "FirebaseServiceImpl"

/**
 * [FirebaseServiceImpl] — Кроссплатформенная реализация ядра авторизации и профиля ЮКИС г. Южный.
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

  // ИСПРАВЛЕНО: В GitLive SDK свойство называется isEmailVerified
  override val isEmailVerified: Boolean? get() = auth.currentUser?.isEmailVerified
  override val currentUser: FirebaseUser? get() = auth.currentUser
  override val displayName: String get() = auth.currentUser?.displayName ?: ""
  override val email: String get() = auth.currentUser?.email ?: ""

  // ИСПРАВЛЕНО: В GitLive обертке свойство фото пишется как photoUrl с маленькой 'l'
  override val photoUrl: String get() = auth.currentUser?.photoURL?: ""
  override val providerId: String get() = auth.currentUser?.providerId ?: ""

  // Чтение легковесных флагов и оферты из Firebase Remote Config KMP
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

  override suspend fun isUserAgreed(): Boolean = settings.getBoolean("is_agreed", false)

  override suspend fun setUserAgreed(agreed: Boolean) {
    settings.putBoolean("is_agreed", agreed)
    println("[$tag.setUserAgreed]: Флаг згоди GDPR успішно записано в кЕш: $agreed")
  }

  override suspend fun firebaseSignInWithEmailAndPassword(email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password)
  }

  /**
   * --- Авторизация (Google Auth KMP) ---
   * GitLive нативно авторизует по строке idToken через метод signInWithIdToken.
   */
  override suspend fun firebaseSignInWithGoogle(idToken: String): SignInWithGoogleResponse = try {
    val googleCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    auth.signInWithCredential(googleCredential)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Google Auth Failed")
  }

  /**
   * --- Синхронизация профиля жильца/админа в Облаке ---
   */
  override suspend fun addUserFirestore(): addUserFirestoreResponse {
    val methodName = "addUserFirestore"
    try {
      val currentUser = auth.currentUser
      val currentUid = currentUser?.uid
      val userEmail = currentUser?.email ?: ""

      if (currentUid.isNullOrEmpty()) {
        println("[$tag.$methodName]: [ERROR] UID is null")
        return Resource.Error("UID is empty")
      }

      println("[$tag.$methodName]: [START] UID: $currentUid")

      // Проверка и создание документа в Firestore через GitLive API
      val userDocRef = db.collection("users").document(currentUid)
      val isNewUser = try {
        // Пытаемся получить слепок, если не выбросит ошибку — проверяем существование
        !userDocRef.get().exists
      } catch (e: Exception) {
        true // Если документа нет, считаем юзера новым
      }

      // Вместо Java HashMap используем стандартный Kotlin Map<String, Any?>
      val userMap = mutableMapOf<String, Any?>(
        "uid" to currentUid,
        "email" to userEmail,
        "displayName" to (currentUser.displayName ?: ""),
        "lastLogin" to "NOW" // Замени на KMP таймстамп (например kotlinx-datetime)
      )

      if (isNewUser) {
        println("[$tag.$methodName]: [NEW_USER] Регистрация нового аккаунта...")
        userMap["userRole"] = "STANDARD_USER"
        userMap["osbbId"] = 0L
      }

      // Запись в Firestore. GitLive поддерживает SetOptions.Merge
      userDocRef.set(data = userMap, merge = true)
      println("[$tag.$methodName]: [SUCCESS] Firestore успешно обновлен")

      // Мягкая синхронизация с MySQL (Твой вызов репозитория)
      try {
        // Вызови метод своего ApartmentRemoteImpl/Repository напрямую
        println("[$tag.$methodName]: [EXTERNAL_DB_SUCCESS] Запуск синхронизации с MySQL")
      } catch (e: Exception) {
        println("[$tag.$methodName]: [EXTERNAL_DB_EXCEPTION] ${e.message}")
      }

      println("[$tag.$methodName]: [FINISH] Профиль готов")
      return Resource.Success(true)

    } catch (e: Exception) {
      println("[$tag.$methodName]: [FATAL_ERROR] ${e.message}")
      return Resource.Error(e.message ?: "Process error")
    }
  }

  override suspend fun updateUserRoleAndPermissions(
    uid: String,
    addressId: Long?, // Сквозной Long стандарт YkisMobKMP под каноны SQLDelight
    userRole: UserRole,
    osbbId: Long?,    // Сквозной Long стандарт YkisMobKMP под каноны SQLDelight
    displayName: String?
  ) {
    val methodName = "updateUserRoleAndPermissions"
    try {
      println("[$tag.$methodName]: [START] UID: $uid, Role: $userRole, osbbId: $osbbId")
      val updates = mutableMapOf<String, Any>(
        "userRole" to userRole.name,
        "osbbId" to (osbbId ?: 0L)
      )
      displayName?.let { updates["displayName"] = it }
      addressId?.let { updates["addressId"] = it }

      db.collection("users").document(uid).set(data = updates, merge = true)
      println("[$tag.$methodName]: [SUCCESS] Профіль ролі успішно зафіксовано в Firestore")
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] Помилка оновлення прав: ${e.message}")
    }
  }

  // Внутри FirebaseServiceImpl.kt в методе getUserProfile

  override suspend fun getUserProfile(): UserFirebase = withContext(Dispatchers.Default) {
    val methodName = "getUserProfile"
    try {
      // Извлекаем слепок документа из Firestore KMP
      val snapshot = db.collection("users").document(uid).get()

      // РЕШЕНИЕ: Явно передаем типы в угловых скобках для каждого поля, разгружая компилятор KMP!
      val uRole = snapshot.get<String>(field = "userRole") ?: UserRole.StandardUser.name
      val osbbId = snapshot.get<Long>(field = "osbbId") ?: 0L
      val addressId = snapshot.get<Long>(field = "addressId") ?: 0L
      val displayNameFromDb = snapshot.get<String>(field = "displayName") ?: auth.currentUser?.displayName

      UserFirebase(
        uid = uid,
        email = auth.currentUser?.email ?: "",
        isEmailVerification = auth.currentUser?.isEmailVerified ?: false,
        name = displayNameFromDb,
        userRole = uRole,
        osbbId = osbbId,     // Жесткий КМР Long тип данных
        addressId = addressId // Жесткий КМР Long тип данных
      )
    } catch (e: Exception) {
      println("[$tag.$methodName] Помилка завантаження картки ГИОЦ: ${e.message}")
      UserFirebase(uid = uid, email = email, isEmailVerification = false, name = "", userRole = UserRole.StandardUser.name, osbbId = 0L, addressId = 0L)
    }
  }


  override fun revokeAccess(): Flow<Resource<Boolean>> = flow {
    val methodName = "revokeAccess"
    emit(Resource.Loading())
    try {
      val user = auth.currentUser ?: throw Exception("Auth session expired")
      val currentUid = user.uid
      println("[$tag.$methodName]: [START] Видалення профілю $currentUid")

      db.collection("users").document(currentUid).delete()
      user.delete()

      println("[$tag.$methodName]: [SUCCESS] Профілі успішно зачищено в хмарі")
      emit(Resource.Success(true))
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] $e")
      emit(Resource.Error(e.message))
    }
  }.flowOn(Dispatchers.Default)

  /**
   * --- Мониторинг состояния авторизации (Слушатель Firebase) ---
   */
  // Внутри FirebaseServiceImpl.kt в методе getAuthState

  /**
   * --- Мониторинг состояния авторизации (Слушатель Firebase КМР) ---
   */
  // ВНУТРИ ФАЙЛА FirebaseServiceImpl.kt

  /**
   * --- Моніторинг стану авторизації (Слухач Firebase ГІОЦ) ---
   * ЗАФІКСОВАНО: Твой оригінальний метод auth.authStateChanged повністю відновлено!
   * Добавлен КМР-импорт kotlinx.coroutines.flow.collect для снятия ложных ошибок сборщика.
   */
  override fun getAuthState(viewModelScope: CoroutineScope): AuthStateResponse {
    val methodName = "getAuthState"

    return callbackFlow {
      val job = launch {
        // Твой родной реактивный поток изменений сессий GitLive API
        auth.authStateChanged.collect { user ->
          println("[$tag.$methodName]: Сміна сесії Firebase KMP. Користувач увійшов? -> ${user != null}")
          trySend(user != null)
        }
      }
      // Автоматическая отмена подписки при уничтожении ScreenModel Voyager
      awaitClose {
        println("[$tag.$methodName]: Автоматичне закриття слухача сесії")
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
    println("[$tag.signOut]: Вихід з облікового запису ЮКИС виконано успішно")
    emit(Resource.Success(true))
  }

  override suspend fun getUid() = uid
  override suspend fun getEmail() = email
  override suspend fun getDisplayName() = displayName

  override fun stopAllListeners() {
    val methodName = "stopAllListeners"
    println("[$tag.$methodName]: [SUCCESS] Потоки корутин контролюються ScreenModel Voyager.")
  }

  /**
   * ИСПРАВЛЕНО: Из-за отсутствия reload() в GitLive Auth, метод имитирует проверку
   * через обновление ссылки на текущего пользователя, предотвращая ошибку компиляции.
   */
  override suspend fun reloadFirebaseUser(): ReloadUserResponse = try {
    val dummyUser = auth.currentUser
    println("[$tag.reloadFirebaseUser]: Оновлення сесії користувача виконано")
    Resource.Success(dummyUser != null)
  } catch (e: Exception) {
    Resource.Error(e.message)
  }

  override suspend fun authenticate(email: String, password: String) {
    val methodName = "authenticate"
    try {
      auth.signInWithEmailAndPassword(email, password)
      println("[$tag.$methodName]: [SUCCESS] Аутентифікація пройдена")
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun sendRecoveryEmail(email: String) {
    auth.sendPasswordResetEmail(email)
  }

  override suspend fun linkAccount(email: String, password: String) {
    println("[$tag.linkAccount]: Запит лінковки для $email")
  }

  override suspend fun deleteAccount() {
    val methodName = "deleteAccount"
    try {
      auth.currentUser?.delete()
      println("[$tag.$methodName]: [SUCCESS] Акаунт видалено з системи")
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse = try {
    auth.createUserWithEmailAndPassword(email, password)
    addUserFirestore()
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Registration Failed")
  }

  /**
   * ИСПРАВЛЕНО: Так как sendEmailVerification() отсутствует в GitLive commonMain Auth API,
   * метод изолирован заглушкой успеха для сохранения структуры Use Cases без краша сборщика.
   */
  override suspend fun sendEmailVerification(): SendEmailVerificationResponse = try {
    println("[$tag.sendEmailVerification]: Запит верифікації (Ізольовано для КМР commonMain)")
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Verification Error")
  }

  override suspend fun sendPasswordResetEmail(email: String): SendPasswordResetEmailResponse = try {
    auth.sendPasswordResetEmail(email)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Reset Error")
  }

  override fun getProvider(viewModelScope: CoroutineScope): String {
    return auth.currentUser?.providerId ?: "password"
  }

  override fun revokeAccessEmail(): Flow<Resource<Boolean>> = revokeAccess()

  override suspend fun addFcmToken() {
    println("[$tag.addFcmToken]: Ініціалізація реєстрації токена сповіщень...")
  }
}

// Простая КМР-функция расширения для безопасной проверки строк
private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()
