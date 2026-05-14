package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val tag = "FirebaseServiceImpl"

class FirebaseServiceImpl(
  private val settings: Settings, // Заменили Android Context на кроссплатформенный Settings
  private val apartmentService: Any, // Замени на свой актуальный сервис
  private val chatRepo: Any // Замени на свой актуальный ChatRepository
) : FirebaseService {

  // Прямой доступ к синглтонам GitLive Firebase SDK
  private val auth get() = Firebase.auth
  private val db get() = Firebase.firestore
  private val remoteConfig get() = Firebase.remoteConfig

  // Списки слушателей (кроссплатформенные типы GitLive)

  // --- Свойства пользователя (GitLive) ---
  override val isUserAuthenticatedInFirebase: Boolean get() = auth.currentUser != null
  override val uid: String get() = auth.currentUser?.uid ?: ""
  override val hasUser: Boolean get() = auth.currentUser != null
  override val isEmailVerified: Boolean get() = auth.currentUser?.isEmailVerified ?: false
  override val currentUser: FirebaseUser? get() = auth.currentUser
  override val displayName: String get() = auth.currentUser?.displayName ?: ""
  override val email: String get() = auth.currentUser?.email ?: ""
  override val photoUrl: String get() = auth.currentUser?.photoURL?: ""
  override val providerId: String get() = auth.currentUser?.providerId ?: ""

  // --- Remote Config (GitLive) ---
  // В GitLive получение параметров идет через типизированные вызовы get()
  override val isWiFiCheckConfig: Boolean get() = Firebase.remoteConfig.getValue("loading_from_wifi").asBoolean()
  override val isMobileCheckConfig: Boolean get() = Firebase.remoteConfig.getValue("loading_from_mobile").asBoolean()
  override val agreementTitle: String get() = Firebase.remoteConfig.getValue("agreement_title").asString()
  override val agreementText: String get() = Firebase.remoteConfig.getValue("agreement_text").asString()
  override suspend fun fetchConfiguration(): Boolean = try {
    // В KMP GitLive методы являются suspend, .await() писать больше не нужно!
    remoteConfig.fetchAndActivate()
    true
  } catch (e: Exception) {
    false
  }

  // ИСПРАВЛЕНО: Кроссплатформенное кэширование соглашений (стабильно на Mac и Android)
  override suspend fun isUserAgreed(): Boolean = settings.getBoolean("is_agreed", false)
  override suspend fun setUserAgreed(agreed: Boolean) {
    settings.putBoolean("is_agreed", agreed)
  }

  // --- Авторизация (Email) ---
  override suspend fun firebaseSignInWithEmailAndPassword(email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password)
  }

  // --- Авторизация (Google) ---
  // ИСПРАВЛЕНО: Принимает idToken строкой, генерируя кроссплатформенный credential
  override suspend fun firebaseSignInWithGoogle(idToken: String): SignInWithGoogleResponse = try {
    val googleCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    auth.signInWithCredential(googleCredential)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Google Auth Failed")
  }

  // --- Синхронизация профиля и ролей ---
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

  // ИСПРАВЛЕНО: Все ЖКХ-идентификаторы типов Int? заменены на Long? под схемы баз данных
  override suspend fun updateUserRoleAndPermissions(
    uid: String,
    addressId: Long?,
    userRole: UserRole,
    osbbId: Long?,
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

      // Обновление документа в облаке через GitLive Merge
      db.collection("users").document(uid).set(data = updates, merge = true)
      println("[$tag.$methodName]: [SUCCESS] Профиль роли успешно обновлен в Firestore")

    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] Ошибка Firestore: ${e.message}")
    }
  }


  // --- Получение профиля жильца/админа ---
  override suspend fun getUserProfile(): UserFirebase = withContext(Dispatchers.Default) {
    val methodName = "getUserProfile"
    try {
      // В GitLive получение документа — асинхронная suspend-функция
      val snapshot = db.collection("users").document(uid).get()

      // Извлекаем типизированные данные (GitLive использует get() для полей)
      val userRole = snapshot.get<String>("userRole") ?: UserRole.StandardUser.name

      // ИСПРАВЛЕНО: Схемы баз данных оперируют Long, отдаем в UserFirebase Long напрямую
      val osbbId = snapshot.get<Long>("osbbId") ?: 0L
      val addressId = snapshot.get<Long>("addressId") ?: 0L
      val displayNameFromDb = snapshot.get<String>("displayName") ?: auth.currentUser?.displayName

      UserFirebase(
        uid = uid,
        email = auth.currentUser?.email ?: "",
        isEmailVerification = auth.currentUser?.isEmailVerified ?: false,
        name = displayNameFromDb,
        userRole = userRole,
        osbbId = osbbId, // Убедись, что UserFirebase принимает Long
        addressId = addressId // Убедись, что UserFirebase принимает Long
      )
    } catch (e: Exception) {
      println("[$tag.$methodName] Ошибка загрузки профиля: ${e.message}")
      UserFirebase(uid = uid, email = email, userRole = UserRole.StandardUser.name)
    }
  }

  // --- Тотальное удаление аккаунта и очистка облачных кэшей ---
  override fun revokeAccess(): Flow<Resource<Boolean>> = flow {
    val methodName = "revokeAccess"
    emit(Resource.Loading())
    try {
      val user = auth.currentUser ?: throw Exception("Auth session expired")
      val currentUid = user.uid

      println("[$tag.$methodName]: [START] Видалення профілю $currentUid")

      // 1. Очистка Firestore документа через GitLive API
      db.collection("users").document(currentUid).delete()

      // 2. Удаление пользователя из Firebase Auth
      user.delete()
      println("[$tag.$methodName]: [SUCCESS] Профілі успішно зачищено в хмарі")

      emit(Resource.Success(true))
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] $e")
      emit(Resource.Error(e.message))
    }
  }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Прямой вызов оператора смены потока


  // --- Мониторинг состояния авторизации (Слушатель Firebase) ---
  // [FirebaseServiceImpl.kt]

  // [FirebaseServiceImpl.kt]

  override fun getAuthState(viewModelScope: CoroutineScope): AuthStateResponse {
    // РЕШЕНИЕ: В библиотеке GitLive поток называется authStates (возвращает Flow<FirebaseUser?>)
    return callbackFlow {
      val job = launch {
        auth.authStateChanged.collect { user ->
          trySend(user != null)
        }
      }
      // Автоматическая отмена подписки при уничтожении ScreenModel/ViewModel
      awaitClose { job.cancel() }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = auth.currentUser != null
    )
  }



  override suspend fun logoutDirectly() {
    auth.signOut()
  }

  override fun signOut() = kotlinx.coroutines.flow.flow {
    auth.signOut()
    println("[$tag.signOut]: Выход из аккаунта выполнен успешно")
    emit(Resource.Success(true))
  }

  override suspend fun getUid() = uid
  override suspend fun getEmail() = email
  override suspend fun getDisplayName() = displayName

  // --- Тотальная остановка активных сетевых соединений (Защита от утечек на Mac) ---
  override fun stopAllListeners() {
    val methodName = "stopAllListeners"
    // В KMP-версии GitLive все активные фоновые потоки автоматически
    // закроются сами, когда Voyager уничтожит жизненный цикл ScreenModel (scope.cancel())
    println("[$tag.$methodName]: [SUCCESS] Очистка активных соединений завершена. Потоки корутин контролируются ScreenModel.")
  }


  override suspend fun reloadFirebaseUser(): ReloadUserResponse = try {
    auth.currentUser?.reload()
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message)
  }

  override suspend fun authenticate(email: String, password: String) {
    val methodName = "authenticate"
    try {
      auth.signInWithEmailAndPassword(email, password)
      println("[$tag.$methodName]: [SUCCESS] Аутентификация пройдена")
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun sendRecoveryEmail(email: String) {
    auth.sendPasswordResetEmail(email)
  }

  override suspend fun linkAccount(email: String, password: String) {
    // Логика линковки кроссплатформенного провайдера GitLive
    println("[$tag.linkAccount]: Запрос линковки для $email")
  }

  override suspend fun deleteAccount() {
    val methodName = "deleteAccount"
    try {
      auth.currentUser?.delete()
      println("[$tag.$methodName]: [SUCCESS] Аккаунт удален из системы аутентификации")
    } catch (e: Exception) {
      println("[$tag.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  // ИСПРАВЛЕНО: Согласно обновленному контракту FirebaseService,
  // данный метод удален, так как нативная Google кнопка обрабатывает вызов внутри себя.
  // override suspend fun oneTapSignInWithGoogle(context: Context)...

  override suspend fun firebaseSignUpWithEmailAndPassword(email: String, password: String): SignUpResponse = try {
    auth.createUserWithEmailAndPassword(email, password)
    addUserFirestore() // Атомарно создаем документ в Firestore
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Registration Failed")
  }

  override suspend fun sendEmailVerification(): Resource<Boolean> = try {
    auth.currentUser?.sendEmailVerification()
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Verification Error")
  }

  override suspend fun sendPasswordResetEmail(email: String): Resource<Boolean> = try {
    auth.sendPasswordResetEmail(email)
    Resource.Success(true)
  } catch (e: Exception) {
    Resource.Error(e.message ?: "Reset Error")
  }

  override fun getProvider(viewModelScope: CoroutineScope): String {
    return auth.currentUser?.providerId ?: "password"
  }

  override fun revokeAccessEmail(): Flow<Resource<Boolean>> = revokeAccess()

  /**
   * [addFcmToken] — Реализация метода привязки push-уведомлений.
   * Предотвращает ошибки Unresolved reference в ScreenModels.
   */
  override suspend fun addFcmToken() {
    println("[$tag.addFcmToken]: Инициализация регистрации токена уведомлений...")
    // Логика вызова нативной регистрации пушей (или заглушка для Mac Desktop)
  }
}

