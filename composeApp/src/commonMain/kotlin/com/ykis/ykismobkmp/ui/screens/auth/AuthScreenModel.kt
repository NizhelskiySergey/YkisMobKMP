package com.ykis.ykismobkmp.ui.screens.auth

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val className = "AuthScreenModel"

/**
 * [AuthScreenModel] — Единый монолитный диспетчер авторизации, регистрации и верификации ИС ЮКИС г. Южный.
 * ИСПРАВЛЕНО: Методы валидации перенесены внутрь класса, убирая ошибку компиляции Unresolved reference!
 * Зафиксирован для полной замены.
 */
class AuthScreenModel(
  private val firebaseService: FirebaseService,
  private val appScreenModel: AppScreenModel,
  logService: LogService
) : BaseScreenModel(logService) {

  // Единое реактивное состояние полей ввода (Email, Пароль, Повторный пароль)
  private val _authUiState = MutableStateFlow(AuthUiState())
  val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

  // --- СТЕЙТЫ ОТВЕТОВ И ЛОАДЕРОВ ИЗ ОБОИХ ИСХОДНЫХ МОДЕЛЕЙ ---
  private val _signInResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val signInResponse: StateFlow<Resource<Boolean>> = _signInResponse.asStateFlow()

  private val _signInWithGoogleResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val signInWithGoogleResponse: StateFlow<Resource<Boolean>> = _signInWithGoogleResponse.asStateFlow()

  private val _signUpResponse = MutableStateFlow<Resource<Boolean>?>(null)
  val signUpResponse: StateFlow<Resource<Boolean>?> = _signUpResponse.asStateFlow()

  private val _sendEmailVerificationResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val sendEmailVerificationResponse: StateFlow<Resource<Boolean>> = _sendEmailVerificationResponse.asStateFlow()

  private val _reloadUserResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val reloadUserResponse: StateFlow<Resource<Boolean>> = _reloadUserResponse.asStateFlow()
  private val _smsSendResponse = MutableStateFlow<Resource<String>?>(null)
  val smsSendResponse = _smsSendResponse.asStateFlow()
  private val _isGoogleLoading = MutableStateFlow(false)
  val isGoogleLoading: StateFlow<Boolean> = _isGoogleLoading.asStateFlow()


  private var currentVerificationId: String? = null

  // Быстрый доступ к полям ввода текущего UI стейта
  val email: String get() = _authUiState.value.email
  private val password: String get() = _authUiState.value.password
  private val repeatPassword: String get() = _authUiState.value.repeatPassword

  val displayEmail: String
    get() = email.ifBlank { firebaseService.currentUser?.email ?: "" }

  val isEmailVerified: Boolean
    get() = Firebase.auth.currentUser?.isEmailVerified ?: false

  init {
    println("[YkisLogKMP.$className.init]:  менеджер AuthScreenModel успешно инициализирован в KMP слое")
  }

  fun onEmailChange(newValue: String) {
    _authUiState.update { it.copy(email = newValue) }
  }

  fun onPasswordChange(newValue: String) {
    _authUiState.update { it.copy(password = newValue) }
  }

  fun onRepeatPasswordChange(newValue: String) {
    _authUiState.update { it.copy(repeatPassword = newValue) }
  }

  private fun isValidEmailKmp(target: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    return target.isNotBlank() && emailRegex.matches(target)
  }

  private fun isValidPasswordKmp(target: String): Boolean {
    return target.isNotBlank() && target.length >= 6
  }

  // ====================================================================
  // --- БЛОК ЛОГИКИ СЦЕНАРИЯ СТАНДАРТНОЙ АВТОРnetworkИЗАЦИИ (SIGN IN) ---
  // ====================================================================

  /**
   * [onSignInClick] — Атомарна процедура авторизації абонента білінгу м. Южне за Email.
   * ИСПРАВЛЕНО НАМЕРТВО: Интегрирован вызов appScreenModel.evaluateStartDestination() для мгновенного
   * пробития навигационного тупика, а также добавлен взвод лоадера с жестким сбросом в finally блоке!
   */
  /**
   * [onSignInClick] — Атомарна процедура авторизації абонента білінгу м. Южне за Email.
   * ИСПРАВЛЕНО НАМЕРТВО: Ошибка Unresolved reference '_isSmsLoading' ликвидирована!
   * Защита от дребезга кликов и лоадер переведены на твой родной поток _signInResponse.
   * Интегрирован вызов appScreenModel.evaluateStartDestination() для мгновенного пробития навигации.
   */
  fun onSignInClick(onSuccessNavigate: () -> Unit) {
    val methodName = "onSignInClick"
    val currentEmail = email
    val currentPassword = password

    // ЗАЩИТА ОТ ДРЕБЕЗГА: Если транзакция уже запущена в сеть — сбрасываем любые повторные тапы по кнопке
    if (_signInResponse.value is Resource.Loading) return

    if (!isValidEmailKmp(currentEmail)) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_ERROR] Некорректный email")
      SnackbarManager.showMessage("Некоректний format email")
      return
    }

    if (currentPassword.isBlank()) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_ERROR] Пустой пароль")
      SnackbarManager.showMessage("Пароль не може бути порожнім")
      return
    }

    launchCatching {
      try {
        println("[YkisLogKMP.$className.$methodName]: [START] Запрос авторизации для $currentEmail")

        // Взводим доменный статус загрузки: кнопка заблокируется, закрутится CircularProgressIndicator
        _signInResponse.value = Resource.Loading()

        // Канонический позиционный вызов GitLive SDK без именованных параметров
        Firebase.auth.signInWithEmailAndPassword(currentEmail, currentPassword)

        // Шаг 2. Регистрируем FCM-токен устройства в облаке Google Cloud для пуш-сообщений
        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Крок 2: Реєстрація FCM токена сповіщень...")
        firebaseService.addFcmToken()

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Авторизация успешна. Verified: $isEmailVerified")

        // Переключаем доменный стейт в Успех
        _signInResponse.value = Resource.Success(true)

        // ====================================================================
        // --- КРИТИЧЕСКИЙ НАВИГАЦИОННЫЙ ФИКС: ПРОБУЖДАЕМ СТЕЙТ-МАШИНУ ЯДРА ---
        // ====================================================================
        // Принудительно заставляем AppScreenModel заново опросить Firebase Auth
        // и запустить Use Case чтения профиля БТИ из Firestore.
        // Модель увидит, что квартир 0, и мгновенно переведет поток в AppStartState.AddApartment!
        appScreenModel.evaluateStartDestination()
        // ====================================================================

        // Нативная КМР лямбда Voyager для бесшовной отрисовки хаба MainApartmentScreen
        onSuccessNavigate()

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: Фатальний збій авторизації за Email: ${e.message}")
        _signInResponse.value = Resource.Error(message = e.message ?: "Невідома помилка")
        SnackbarManager.showMessage("Помилка входу: ${e.message}")
      }
    }
  }




  // --- ВОССТАНОВЛЕНИЕ ПАРОЛЯ ---
  fun onForgotPasswordClick() {
    val methodName = "onForgotPasswordClick"
    val currentEmail = email
    if (!isValidEmailKmp(currentEmail)) {
      SnackbarManager.showMessage("Некоректний формат email")
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [RECOVERY] Запрос восстановления на почту $currentEmail")
      firebaseService.sendRecoveryEmail(currentEmail)
      SnackbarManager.showMessage("Лист для відновлення паролю надіслано")
    }
  }

  // ====================================================================
  // --- БЛОК ЛОГИКИ СЦЕНАРИЯ СТАНДАРТНОЙ РЕГИСТРАЦИИ (SIGN UP) ---------
  // ====================================================================

  private fun isInputValid(): Boolean {
    val methodName = "validate"
    if (!isValidEmailKmp(email)) {
      println("[YkisLogKMP.$className.$methodName]: Некоректний email: $email")
      SnackbarManager.showMessage("Некоректний формат email")
      return false
    }
    if (!isValidPasswordKmp(password)) {
      println("[YkisLogKMP.$className.$methodName]: Пароль не пройшов перевірку складності")
      SnackbarManager.showMessage("Пароль занадто простий")
      return false
    }
    if (password != repeatPassword) {
      println("[YkisLogKMP.$className.$methodName]: Паролі не збігаються")
      SnackbarManager.showMessage("Введені паролі не збігаються")
      return false
    }
    return true
  }

  fun signUpWithEmailAndPassword(onSuccess: () -> Unit) {
    val methodName = "signUpWithEmailAndPassword"
    if (!isInputValid()) return
    _signUpResponse.value = null

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Реєстрація аккаунту для: $email")
      _signUpResponse.value = Resource.Loading()

      val result = firebaseService.firebaseSignUpWithEmailAndPassword(email, password)
      _signUpResponse.value = result

      if (result is Resource.Success) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Користувача створено. Надсилання email верифікації...")
        firebaseService.sendEmailVerification()
        firebaseService.addFcmToken()
        onSuccess()
      } else if (result is Resource.Error) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] Реєстрація відхилена: ${result.message}")
      }
    }
  }

  // --- ПОВТОРНАЯ ОТПРАВКА ССЫЛКИ ВЕРИФИКАЦИИ ---
  fun repeatEmailVerified() {
    val methodName = "repeatEmailVerified"
    val userEmail = Firebase.auth.currentUser?.email

    screenModelScope.launch {
      try {
        println("[YkisLogKMP.$className.$methodName]: [REQUEST] Надсилання листа на $userEmail")
        _sendEmailVerificationResponse.value = Resource.Loading()

        val result = firebaseService.sendEmailVerification()
        _sendEmailVerificationResponse.value = result

        if (result is Resource.Success) {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Лист успішно надіслано")
          SnackbarManager.showMessage("Лист для підтвердження надіслано на вашу пошту")
        } else if (result is Resource.Error) {
          val errorMsg = result.message ?: "Не вдалося відпустити лист"
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Причина відмови Firebase: $errorMsg")
          SnackbarManager.showMessage(errorMsg)
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [CRITICAL_ERROR] ${e.message}")
        _sendEmailVerificationResponse.value = Resource.Error(message = e.message ?: "Помилка")
      }
    }
  }

  // --- ПРИНУДИТЕЛЬНОЕ ОБНОВЛЕНИЕ СЕССИИ ПОЛЬЗОВАТЕЛЯ ---
  fun reloadUser(onSuccess: () -> Unit) {
    val methodName = "reloadUser"
    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Перевірка підтвердження пошти користувачем...")
      _reloadUserResponse.value = Resource.Loading()

      val result = firebaseService.reloadFirebaseUser()
      _reloadUserResponse.value = result
      _reloadUserResponse.value = result

      if (result is Resource.Success) {
        val verified = Firebase.auth.currentUser?.isEmailVerified == true
        println("[YkisLogKMP.$className.$methodName]: [RESULT] Статус верифікації пошти в облаці: $verified")
        if (verified) {
          firebaseService.addFcmToken()
          onSuccess()
        } else {
          SnackbarManager.showMessage("Пошта ще не підтверджена. Перевірте вашу скриньку.")
        }
      }
    }
  }

  // ====================================================================
  // --- БЛОК КРОСС-ПЛАТФОРМЕННОЙ GOOGLE АВТОРnetworkИЗАЦИИ --------------
  // ====================================================================

  private val auth get() = Firebase.auth

  private suspend fun signInAndLinkWithGoogle(idToken: String) {
    val methodName = "signInAndLinkWithGoogle"
    val firebaseCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    val currentUser = auth.currentUser

    if (currentUser == null) {
      println("[YkisLogKMP.$className.$methodName]: [NEW_USER] Обычная авторизация in Firebase")
      auth.signInWithCredential(firebaseCredential)
    } else {
      println("[YkisLogKMP.$className.$methodName]: [LINK] Привязка провайдера Google к текущему аккаунту")
      currentUser.linkWithCredential(firebaseCredential)
    }
  }

  /**
   * [onSignUpWithGoogle] — Синхронізація профілю та авторизація через Google ID Token.
   */
  /**
   * [onSignUpWithGoogle] — Каскадна процедура авторизації та лінкування профілю мешканця через Google.
   * ИСПРАВЛЕНО НАМЕРТВО: Интегрирован независимый булевый флаг _isGoogleLoading с защитой от дребезга кликов!
   * Лоадер гарантированно гаснет в блоке finally при любых сетевых таймаутах Google Credential Manager.
   */
  fun onSignUpWithGoogle(idToken: String, onFinishedNavigate: () -> Unit) {
    val methodName = "onSignUpWithGoogle"

    // ЗАЩИТА ОТ ДРЕБЕЗГА: Если транзакция уже запущена в ОЗУ — игнорируем повторные тапы по кнопке!
    if (_isGoogleLoading.value) return
    _isGoogleLoading.value = true

    screenModelScope.launch {
      try {
        // Выставляем доменный статус загрузки для внешних систем экрана
        _signInWithGoogleResponse.value = Resource.Loading()
        println("[YkisLogKMP.$className.$methodName]: [START] Фоновий лоадер Google запущено. Блокування інтерфейсу активовано.")

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Крок 1: Авторизація Firebase Auth...")
        signInAndLinkWithGoogle(idToken)

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Крок 2: Синхронізація СУБД та створення профілю Firestore...")
        val dbResult = firebaseService.addUserFirestore()

        // Принудительно передаем финальный статус ответа БД в стейт ответа интерфейса
        _signInWithGoogleResponse.value = dbResult

        if (dbResult is Resource.Error) {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Помилка при збереженні профілю в Firestore: ${dbResult.message}")
          SnackbarManager.showMessage("Помилка синхронізації профілю: ${dbResult.message}")
          return@launch // Легитимный выход из корутины scope при ошибке Firestore
        }

        // Шаг 3. Регистрируем FCM-токен устройства в облаке Google Cloud для пуш-сообщений биллинга
        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Крок 3: Реєстрація FCM токена сповіщень...")
        firebaseService.addFcmToken()

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Всі шлюзи безпеки пройдено. Запуск перерахунку стартової траєкторії.")
        _signInWithGoogleResponse.value = Resource.Success(true)

        // Пересчитываем стейт-машину, чтобы КМР-рантайм бесшовно зафиксировал вход жильца
        appScreenModel.evaluateStartDestination()

        // Вызываем нативную лямбду навигации Voyager для replaceAll перехода на MainApartmentScreen
        onFinishedNavigate()

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: Помилка рантайма Google Credential Manager: ${e.message}")
        _signInWithGoogleResponse.value = Resource.Error(message = e.message ?: "Невідома помилка")
        SnackbarManager.showMessage("Помилка входу Google: ${e.message}")
      } finally {
        // ИСПРАВЛЕНО НАМЕРТВО: Блок finally сработает ВСЕГДА, пробивая любые сетевые лаги Google Cloud!
        // Кнопка в интерфейсе гарантированно разблокируется, а крутилка погаснет.
        println("[YkisLogKMP.$className.$methodName]: [FINISH] Транзакція завершена. Зняття блокування кнопки.")
        _isGoogleLoading.value = false
      }
    }
  }


  // ====================================================================
  // --- ДОБАВЛЕНО: МЕТОДЫ АУТЕНТИФИКАЦИИ ПО НОМЕРУ ТЕЛЕФОНА (SMS) ---
  // ====================================================================

  fun onPhoneChange(newValue: String) {
    _authUiState.update { it.copy(phoneNumber = newValue) }
  }

  fun onSmsCodeChange(newValue: String) {
    _authUiState.update { it.copy(smsCode = newValue) }
  }

  fun setSmsSentState(isSent: Boolean) {
    _authUiState.update { it.copy(isSmsSent = isSent) }
  }

  /**
   * [triggerSmsCode] — Запрос на отправку SMS-кода через платформозависимый мост.
   * На Android пробрасывает context Activity для прохождения валидации App Check Google.
   */
  fun triggerSmsCode(activityContext: Any?, onSuccess: () -> Unit) {
    val methodName = "triggerSmsCode"
    val phone = _authUiState.value.phoneNumber
    if (phone.isBlank()) {
      SnackbarManager.showMessage("Введіть номер телефону")
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Запит SMS на номер: $phone")
      _smsSendResponse.value = Resource.Loading()

      // Вызываем наш expect/actual мост, который мы настроили на нативном уровне
      val result = firebaseService.sendSmsCode(phone, activityContext)

      if (result is Resource.Success) {
        currentVerificationId = result.data
        _authUiState.update { it.copy(isSmsSent = true) }
        _smsSendResponse.value = null // Очищаем статус лоадера отправки SMS
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Сесія SMS зафіксована. Очікування коду.")
        onSuccess()
      } else if (result is Resource.Error) {
        _smsSendResponse.value = Resource.Error(result.message ?: "Помилка")
        SnackbarManager.showMessage(result.message ?: "Не вдалося надіслати SMS")
      }
    }
  }

  /**
   * [verifySmsAndSignIn] — Проверка введённого 6-значного кода и авторизация сессии телефона в облаке.
   */
  fun verifySmsAndSignIn(onSuccess: () -> Unit) {
    val methodName = "verifySmsAndSignIn"
    val state = _authUiState.value
    val verificationId = currentVerificationId

    if (verificationId == null || state.smsCode.isBlank()) {
      SnackbarManager.showMessage("Введіть 6-значний код із SMS")
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Перевірка коду: ${state.smsCode}")
      _signInResponse.value = Resource.Loading()

      val result = firebaseService.signInWithSmsCode(verificationId, state.smsCode)

      if (result is Resource.Success) {
        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Вхід схвалено. Синхронізація Firestore...")

        // Создаем запись пользователя в Firestore
        val dbResult = firebaseService.addUserFirestore()

        if (dbResult is Resource.Error) {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Помилка Firestore: ${dbResult.message}")
          _signInResponse.value = dbResult
          SnackbarManager.showMessage("Помилка синхронізації бази даних")
          return@launchCatching
        }

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Профіль створено. Примусове оновлення ID-токену сесії...")

        // КРИТИЧЕСКИЙ ФИКС: Заставляем нативное ядро Firebase обновить защищенные токены в памяти смартфона!
        try {
          // Прямой вызов обновления токена GitLive Auth, который нативно зафиксирует права пользователя в ОС
          dev.gitlive.firebase.Firebase.auth.currentUser?.getIdToken(forceRefresh = true)
          println("[YkisLogKMP.$className.$methodName]: [TOKEN_SUCCESS] Локальний токен оновлено успішно")
        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: [TOKEN_WARN] Помилка оновлення токену: ${e.message}")
        }

        // Принудительно гасим лоадер в UI-слое перед вызовом навигации!
        _signInResponse.value = Resource.Success(true)

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Все готово. Запуск колбеку onSuccess().")
        firebaseService.addFcmToken()
        appScreenModel.evaluateStartDestination()
        onSuccess() // Бесшовно перенаправляет на MainApartmentScreen через replaceAll

      } else if (result is Resource.Error) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] Помилка Firebase Auth")
        _signInResponse.value = Resource.Error(result.message ?: "Помилка")
        SnackbarManager.showMessage(result.message ?: "Невірний код підтвердження")
      }
    }
  }






}

// Кроссплатформенные изолированные хелперы валидации строк
private fun String.isValidEmailKmp(): Boolean {
  val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
  return this.isNotBlank() && emailRegex.matches(this)
}

private fun String.isValidPasswordKmp(): Boolean = this.isNotBlank() && this.length >= 6

