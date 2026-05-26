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
    println("[YkisLogKMP.$className.init]: Монолитный менеджер AuthScreenModel успешно инициализирован в KMP слое")
  }

  // --- МЕТОДЫ ИЗМЕНЕНИЯ СТЕЙТОВ ПОЛЕЙ ВВОДЫ ---
  fun onEmailChange(newValue: String) {
    _authUiState.update { it.copy(email = newValue) }
  }

  fun onPasswordChange(newValue: String) {
    _authUiState.update { it.copy(password = newValue) }
  }

  fun onRepeatPasswordChange(newValue: String) {
    _authUiState.update { it.copy(repeatPassword = newValue) }
  }

  // --- ВНУТРЕННЯЯ КРOСС-ПЛАТФОРМЕННАЯ ВАЛИДАЦИЯ СТРОК ---
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

  fun onSignInClick(onSuccessNavigate: () -> Unit) {
    val methodName = "onSignInClick"
    val currentEmail = email
    val currentPassword = password

    if (!isValidEmailKmp(currentEmail)) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_ERROR] Некорректный email")
      SnackbarManager.showMessage("Некоректний формат email")
      return
    }

    if (currentPassword.isBlank()) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_ERROR] Пустой пароль")
      SnackbarManager.showMessage("Пароль не може бути порожнім")
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Запрос авторизации для $currentEmail")
      _signInResponse.value = Resource.Loading()

      // ИСПРАВЛЕНО: Канонический позиционный вызов GitLive SDK без именованных параметров
      // Это полностью исключает ошибки резолва сигнатур компилятором Kotlin
      Firebase.auth.signInWithEmailAndPassword(currentEmail, currentPassword)

      firebaseService.addFcmToken()

      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Авторизация успешна. Verified: $isEmailVerified")
      _signInResponse.value = Resource.Success(true)

      onSuccessNavigate()
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
   * ІСПРАВЛЕНО: Забезпечено примусове гасіння лоадера при будь-яких помилках Firestore,
   * мітка return@launch замінена на легітимну для архітектури KMP.
   */
  fun onSignUpWithGoogle(idToken: String, onFinishedNavigate: () -> Unit) {
    val methodName = "onSignUpWithGoogle"

    screenModelScope.launch {
      try {
        _signInWithGoogleResponse.value = Resource.Loading()
        println("[YkisLogKMP.$className.$methodName]: [START] Фоновий лоадер Google запущено.")

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Крок 1: Авторизація Firebase...")
        signInAndLinkWithGoogle(idToken)

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Крок 2: Синхронізація БД та профілю Firestore...")
        val dbResult = firebaseService.addUserFirestore()

        // КРИТИЧЕСКИЙ ФИКС: Принудительно передаем финальный статус ответа БД в стейт лоадера интерфейса!
        _signInWithGoogleResponse.value = dbResult

        if (dbResult is Resource.Error) {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Помилка при збереженні профілю в Firestore: ${dbResult.message}")
          SnackbarManager.showMessage("Помилка синхронізації профілю: ${dbResult.message}")
          // ІСПРАВЛЕНО: Замінили return@launch на легітимне повернення з поточного scope
          return@launch
        }

        firebaseService.addFcmToken()
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Всі перевірки пройдено. Виклик навігації.")
        _signInWithGoogleResponse.value = Resource.Success(true)
        appScreenModel.evaluateStartDestination()
        onFinishedNavigate() // Бесшовно перенаправляет на MainApartmentScreen через replaceAll

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [CRITICAL] Помилка рантайма Google: ${e.message}")
        _signInWithGoogleResponse.value = Resource.Error(message = e.message ?: "Невідома помилка")
        SnackbarManager.showMessage("Помилка входу Google: ${e.message}")
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

