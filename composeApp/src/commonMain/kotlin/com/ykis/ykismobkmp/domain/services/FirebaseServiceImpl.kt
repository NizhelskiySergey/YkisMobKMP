package com.ykis.ykismobkmp.domain.services

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.Constants.TERMS_ACCEPTED_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock


private const val className = "FirebaseServiceImpl"
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
  override val displayName: String get() = auth.currentUser?.displayName ?: "Користувач ЮКІС"
  
  // ИСПРАВЛЕНО: Если почта пустая (вход по телефону), возвращаем номер телефона как идентификатор
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
  override val isWiFiCheckConfig: Boolean get() = remoteConfig.getValue("loading_from_wifi").asBoolean()
  override val isMobileCheckConfig: Boolean get() = remoteConfig.getValue("loading_from_mobile").asBoolean()
  override val agreementTitle: String get() = remoteConfig.getValue("agreement_title").asString()
  override val agreementText: String get() = remoteConfig.getValue("agreement_text").asString()

  // Список активных КМР-слушателей Cloud Firestore (Профиль БТИ, Квартиры)
  private val firestoreJobs = mutableListOf<Job>()
  // Список фоновых корутин-потоков, слушающих Realtime Database (Чаты, Бейджи)
  private val rtdbJobs = mutableListOf<Job>()

  override suspend fun getUid() = uid
  override suspend fun getEmail() = email
  override suspend fun getDisplayName() = displayName

  // Список фоновых корутин-потоков (Jobs), слушающих Realtime Database (чаты, бейджи)
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

  /**
   * [getSaveUserUidResult] — Вспомогательный suspend-метод синхронизации учетной записи с MySQL базой данных.
   */
  private suspend fun getSaveUserUidResult(uid: String, email: String) =
    apartmentService.saveUserUid(uid, email).filter { it !is Resource.Loading }.first()

  /**
   * [addUserFirestore] — Системный КМР-метод инициализации профиля пользователя в Firestore и MySQL биллинга.
   * ПОЯСНЕНИЕ: Метод атомарно создает документ пользователя слиянием (merge = true), а затем
   * пробрасывает транзакцию синхронизации UID в удаленную базу данных г. Южного.
   */
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

      if (currentUid.isNullOrBlank()) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] UID пустой, отмена регистрации.")
        return Resource.Error(message = "UID is empty")
      }
      println("[YkisLogKMP.$className.$methodName]: [START] Инициализация профиля для UID: $currentUid")

      val userDocRef = db.collection("users").document(currentUid)
      val currentTimestamp = Clock.System.now().epochSeconds

      // ОПТИМИЗАЦИЯ: Сначала проверяем, существует ли профиль, чтобы не перезаписать роль админа
      val existingDoc = try {
        userDocRef.get()
      } catch (e: Exception) {
        null
      }

      val existingRole = existingDoc?.get("userRole") as? String

      // Собираем карту полей по умолчанию для бесконфликтного слияния данных
      val userMap = mutableMapOf<String, Any?>(
        "uid" to currentUid,
        "email" to userEmail,
        "phoneNumber" to (rawPhone ?: ""),
        "lastLogin" to currentTimestamp
      )

      // Если роли нет (новый юзер) — ставим стандартную. Если есть — НЕ ТРОГАЕМ!
      if (existingRole.isNullOrBlank()) {
        userMap["userRole"] = "STANDARD_USER"
        userMap["osbbId"] = 0L
        userMap["addressId"] = 0L
        userMap["displayName"] = currentUser?.displayName ?: "Мешканець"
      }

      println("[YkisLogKMP.$className.$methodName]: [PROCESS] Запуск безопасной записи set(merge = true) в Firestore...")
      userDocRef.set(data = userMap, merge = true)
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Профиль успешно зафиксирован в Firestore.")

      // ИСПРАВЛЕНО НАМЕРТВО: Интегрирован вызов внешней suspend-функции синхронизации с MySQL биллинга!
      // Блок изолирован через runCatching, чтобы сбои Ktor-сети Южного не ломали локальную сессию Firebase!
      runCatching {
        println("[YkisLogKMP.$className.$methodName]: [EXTERNAL_DB_SYNC] Запуск Ktor-синхронизации saveUserUid с MySQL...")

        val mysqlResult = getSaveUserUidResult(uid = currentUid, email = userEmail)

        when (mysqlResult) {
          is Resource.Success -> {
            println("[YkisLogKMP.$className.$methodName]: [EXTERNAL_DB_SUCCESS] Успешная синхронизация с MySQL базой данных ЮКИС.")
          }
          is Resource.Error -> {
             if (mysqlResult.message == "UserUIdExist") {
                println("[YkisLogKMP.$className.$methodName]: [EXTERNAL_DB_HIT] UID уже привязан в MySQL. Продолжаем вход.")
             } else {
                println("[YkisLogKMP.$className.$methodName]: [EXTERNAL_DB_ERROR] MySQL отклонил UID: ${mysqlResult.message}")
                throw Exception(mysqlResult.message)
             }
          }
          else -> { /* Роли лоадеров отсечены фильтром */ }
        }
      }.onFailure { e ->
        println("[YkisLogKMP.$className.$methodName]: Критический сбой сетевого шлюза MySQL saveUserUid: ${e.message}")
        return Resource.Error(message = "Ошибка синхронизации с городским сервером. Попробуйте войти повторно.")
      }

      println("[YkisLogKMP.$className.$methodName]: [FINISH] Контур создания профиля успешно завершен.")
      return Resource.Success(true)

    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [FATAL_ERROR] Непредвиденная ошибка рантайма: ${e.message}")
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
        "userRole" to userRole.getSerialName(), // Используем стабильный строковый идентификатор
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

  override suspend fun revokeAccess(): Resource<Boolean> {
    val methodName = "revokeAccess"
    return withContext(Dispatchers.Default) {
      try {
        // 1. Проверяем валидность текущей КМР-сессии Auth
        val user = auth.currentUser ?: throw Exception("Auth session expired")
        val currentUid = user.uid
        println("[YkisLogKMP.$className.$methodName]: [START] Запуск процедури деструкції профілю UID: \"$currentUid\"")

        // 2. КРИТИЧЕСКИЙ ШАГ БЕЗОПАСНОСТИ: Первым делом пробуем удалить Auth-аккаунт в облаке Google!
        // Если сессия устарела, Firebase Auth SDK мгновенно выбросит исключение ДО того,
        // как код успеет безвозвратно стереть данные жильца из Cloud Firestore.
        try {
          user.delete()
          println("[YkisLogKMP.$className.$methodName]: [AUTH_DELETED] Аккаунт авторизації успішно стерто з серверів Google.")
        } catch (authException: Exception) {
          val errMessage = authException.message ?: ""

          // Проверяем нативный строковый маркер безопасности Google ("recent login required")
          if (errMessage.contains("recent", ignoreCase = true) || errMessage.contains("credentials", ignoreCase = true)) {
            println("[YkisLogKMP.$className.$methodName]: [REAUTHENTICATION_REQUIRED] Виявлено застарілу сесію! Зупинка каскаду.")
            // Возвращаем кастомное сообщение ошибки, которое вьюмодель распарсит как требование перезахода
            return@withContext Resource.Error(message = "CREDENTIALS_TOO_OLD")
          }
          throw authException // Если ошибка другая (например, таймаут сети) — пробрасываем её дальше
        }

        // 3. СЕССИЯ ПОДТВЕРЖДЕНА И СВЕЖАЯ — Теперь безопасно вырезаем документы пользователя из Cloud Firestore!
        db.collection("users").document(currentUid).delete()
        println("[YkisLogKMP.$className.$methodName]: [FIRESTORE_DELETED] Документи користувача успішно вилучені з баз даних.")

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Повне каскадне видалення профілю у хмарі завершено успішно.")
        Resource.Success(true)

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: Фатальний збій деструкції аккаунта: ${e.message}")
        Resource.Error(message = e.message ?: "Unknown deletion error")
      }
    }
  }


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


  override suspend fun signOut() {
    val methodName = "signOut"
    try {
      println("[YkisLogKMP.$className.$methodName]: [START] Запит до GitLive SDK на анулювання токена сесії...")

      // Нативно удаляем сессионный токен из защищенного хранилища Keystore смартфона!
      auth.signOut()

      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Вихід з облікового запису ЮКИС виконано успішно. Сесію закрито.")
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.${methodName}_CRITICAL_ERROR]: Помилка під час нативного логауту Firebase Auth: ${e.message}")
    }
  }

  override fun stopAllListeners() {
    val methodName = "stopAllListeners"
    println("[YkisLogKMP.FirebaseService.$methodName]: [START] Повне очищення всіх активних з'єднань мережі KMP.")

    try {
      // 1. Очищення КМР-слухачів Cloud Firestore (Характеристики квартир БТИ, профиль)
      firestoreJobs.forEach { job ->
        try {
          if (job.isActive) {
            job.cancel() // Принудительно закрываем корутинный канал, GitLive нативно сотрет SnapshotListener!
          }
        } catch (e: Exception) {
          println("[YkisLogKMP.FirebaseService.$methodName.Firestore_ERR]: Помилка зупинки потоку Cloud Firestore: ${e.message}")
        }
      }
      firestoreJobs.clear()
      println("[YkisLogKMP.FirebaseService.$methodName]: Слухачі Cloud Firestore успішно видалені з пам'яті КМР.")

      // 2. Очищення Realtime Database (Кімнати обговорення чатів, лічильники непрочитаних ГИОЦ)
      rtdbJobs.forEach { job ->
        try {
          if (job.isActive) {
            job.cancel() // Намертво рвем асинхронную корутину связи с чатами Google RTDB
          }
        } catch (e: Exception) {
          println("[YkisLogKMP.FirebaseService.$methodName.RTDB_ERR]: Помилка закриття каналу Realtime Database: ${e.message}")
        }
      }
      rtdbJobs.clear()
      println("[YkisLogKMP.FirebaseService.$methodName]: Потоки Realtime Database успішно зупинені та видалені з пам'яті КМР.")

    } catch (e: Exception) {
      println("[YkisLogKMP.FirebaseService.$methodName]: Помилка під час каскадної зупинки слухачів Firebase: ${e.message}")
    }
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


  override suspend fun addFcmToken() {
    val methodName = "addFcmToken"
    try {
      val token = getPlatformFcmToken() ?: return
      val currentUid = auth.currentUser?.uid ?: return
      
      val userDocRef = db.collection("users").document(currentUid)
      
      // ИСПРАВЛЕНО: Ключ изменен на 'fcmTokens' для синхронизации с маппером UserEntity
      val updates = mapOf("fcmTokens" to dev.gitlive.firebase.firestore.FieldValue.arrayUnion(token))
      userDocRef.update(updates)
      
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] FCM токен успешно добавлен: ${token.take(10)}...")
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [ERROR] Ошибка регистрации токена: ${e.message}")
    }
  }
}

expect suspend fun getPlatformFcmToken(): String?

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

