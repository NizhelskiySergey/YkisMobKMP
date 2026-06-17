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
import org.jetbrains.compose.resources.StringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "AuthScreenModel"

/**
 * [AuthScreenModel] — Единый монолитный диспетчер авторизации, регистрации и верификации ИС ЮКИС г. Южный.
 * ИСПРАВЛЕНО: Методы валидации перенесены внутрь класса, убирая ошибку компиляции Unresolved reference!
 * Зафиксирован для полной замены.
 */
class AuthScreenModel(
  private val firebaseService: FirebaseService,
  private val appScreenModel: AppScreenModel,
  logService: LogService,
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

  private val _reloadUserResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val reloadUserResponse: StateFlow<Resource<Boolean>> = _reloadUserResponse.asStateFlow()
  private val _smsSendResponse = MutableStateFlow<Resource<String>?>(null)
  val smsSendResponse = _smsSendResponse.asStateFlow()
  private val _isGoogleLoading = MutableStateFlow(false)
  val isGoogleLoading: StateFlow<Boolean> = _isGoogleLoading.asStateFlow()

  fun setGoogleLoading(isLoading: Boolean) {
    _isGoogleLoading.value = isLoading
  }


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
    println("[YkisLogKMP.$className.init]: менеджер AuthScreenModel успешно инициализирован в KMP слое")
  }

  fun onEmailChange(newValue: String) {
    _authUiState.update { it.copy(email = newValue.trim()) }
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
    // Минимум 6 символов, хотя бы одна цифра и одна буква
    val passwordRegex = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$".toRegex()
    return target.isNotBlank() && passwordRegex.matches(target)
  }

  private fun mapFirebaseError(message: String?): StringResource {
    if (message == null) return Res.string.error_unknown
    return when {
      message.contains("user-not-found", true) -> Res.string.error_user_not_found
      message.contains("wrong-password", true) -> Res.string.error_wrong_password
      message.contains("email-already-in-use", true) -> Res.string.error_email_already_in_use
      message.contains("invalid-email", true) -> Res.string.error_invalid_email
      message.contains("network-request-failed", true) -> Res.string.error_network_request_failed
      message.contains("too-many-requests", true) -> Res.string.error_too_many_requests
      else -> Res.string.error_unknown
    }
  }

  fun onSignInClick(onSuccessNavigate: () -> Unit) {
    val methodName = "onSignInClick"
    val currentEmail = email
    val currentPassword = password

    // ЗАЩИТА ОТ ДРЕБЕЗГА: Если запрос уже в процессе — игнорируем
    if (_signInResponse.value is Resource.Loading) return

    if (!isValidEmailKmp(currentEmail)) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_ERROR] Некорректный email")
      SnackbarManager.showMessage(Res.string.email_error)
      return
    }

    if (currentPassword.isBlank()) {
      println("[YkisLogKMP.$className.$methodName]: [VALIDATION_ERROR] Пустой пароль")
      SnackbarManager.showMessage(Res.string.error_empty_password)
      return
    }

    launchCatching {
      try {
        println("[YkisLogKMP.$className.$methodName]: [START] Запрос авторизации для $currentEmail")

        // Взводим доменный статус загрузки: кнопка заблокируется, закрутится CircularProgressIndicator
        _signInResponse.value = Resource.Loading()

        // Канонический позиционный вызов GitLive SDK
        Firebase.auth.signInWithEmailAndPassword(currentEmail, currentPassword)

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Авторизація пройдена. Запуск фонових процесів...")

        // КРИТИЧНИЙ ФІКС: Віддаємо успіх ВІДРАЗУ
        _signInResponse.value = Resource.Success(true)

        // Фонова синхронізація (не блокує перехід)
        screenModelScope.launch {
            println("[YkisLogKMP.$className.$methodName]: [BACKGROUND] Синхронізація профілю та FCM...")
            firebaseService.addUserFirestore()
            firebaseService.addFcmToken()
        }

        appScreenModel.evaluateStartDestination()
        onSuccessNavigate()

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: Фатальный сбой авторизации по Email: ${e.message}")
        val friendlyErrorRes = mapFirebaseError(e.message)
        _signInResponse.value = Resource.Error(messageRes = friendlyErrorRes)
        SnackbarManager.showMessage(friendlyErrorRes)
      }
    }
  }
  // --- ВОССТАНОВЛЕНИЕ ПАРОЛЯ ---
  fun onForgotPasswordClick() {
    val methodName = "onForgotPasswordClick"
    val currentEmail = email
    if (!isValidEmailKmp(currentEmail)) {
      SnackbarManager.showMessage(Res.string.email_error)
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [RECOVERY] Запрос восстановления на почту $currentEmail")
      firebaseService.sendRecoveryEmail(currentEmail)
      SnackbarManager.showMessage(Res.string.recovery_email_sent)
    }
  }

  // ====================================================================
  // --- БЛОК ЛОГИКИ СЦЕНАРИЯ СТАНДАРТНОЙ РЕГИСТРАЦИИ (SIGN UP) ---------
  // ====================================================================

  private fun isInputValid(): Boolean {
    val methodName = "validate"
    if (!isValidEmailKmp(email)) {
      println("[YkisLogKMP.$className.$methodName]: Некорректный email: $email")
      SnackbarManager.showMessage(Res.string.email_error)
      return false
    }
    if (!isValidPasswordKmp(password)) {
      println("[YkisLogKMP.$className.$methodName]: Пароль не прошел проверку сложности")
      SnackbarManager.showMessage(Res.string.error_invalid_password_format)
      return false
    }
    if (password != repeatPassword) {
      println("[YkisLogKMP.$className.$methodName]: Пароли не совпадают")
      SnackbarManager.showMessage(Res.string.error_passwords_mismatch)
      return false
    }
    return true
  }

  fun signUpWithEmailAndPassword(onSuccess: () -> Unit) {
    val methodName = "signUpWithEmailAndPassword"
    if (!isInputValid()) return
    
    // Защита от дребезга
    if (_signUpResponse.value is Resource.Loading) return
    
    _signUpResponse.value = null

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Регистрация аккаунта для: $email")
      _signUpResponse.value = Resource.Loading()

      val result = firebaseService.firebaseSignUpWithEmailAndPassword(email, password)
      
      if (result is Resource.Success) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Користувач створений. Лист верифікації має бути надісланий автоматично.")
        _signUpResponse.value = Resource.Success(true)
        onSuccess()
      } else if (result is Resource.Error) {
        val friendlyErrorRes = mapFirebaseError(result.message)
        println("[YkisLogKMP.$className.$methodName]: [ERROR] Регистрация отклонена: ${result.message}")
        _signUpResponse.value = Resource.Error(messageRes = friendlyErrorRes)
        SnackbarManager.showMessage(friendlyErrorRes)
      }
    }
  }
  // --- ПОВТОРНАЯ ОТПРАВКА ССЫЛКИ ВЕРИФИКАЦИИ ---
  fun repeatEmailVerified() {
    val methodName = "repeatEmailVerified"
    val userEmail = Firebase.auth.currentUser?.email

    screenModelScope.launch {
      try {
        println("[YkisLogKMP.$className.$methodName]: [REQUEST] Отправка письма на $userEmail")
        _sendEmailVerificationResponse.value = Resource.Loading()

        val result = firebaseService.sendEmailVerification()
        _sendEmailVerificationResponse.value = result

        if (result is Resource.Success) {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Письмо успешно отправлено")
          SnackbarManager.showMessage(Res.string.verify_email_message)
        } else if (result is Resource.Error) {
          val errorMsg = result.message ?: "Не удалось отправить письмо"
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Причина отказа Firebase: $errorMsg")
          SnackbarManager.showMessage(Res.string.generic_error)
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [CRITICAL_ERROR] ${e.message}")
        _sendEmailVerificationResponse.value = Resource.Error(messageRes = Res.string.error_unknown)
      }
    }
  }

  // --- ПРИНУДИТЕЛЬНОЕ ОБНОВЛЕНИЕ СЕССИИ ПОЛЬЗОВАТЕЛЯ ---
  fun reloadUser(onSuccess: () -> Unit) {
    val methodName = "reloadUser"
    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Проверка подтверждения почты пользователем...")
      _reloadUserResponse.value = Resource.Loading()

      val result = firebaseService.reloadFirebaseUser()
      _reloadUserResponse.value = result

      if (result is Resource.Success) {
        val verified = Firebase.auth.currentUser?.isEmailVerified == true
        println("[YkisLogKMP.$className.$methodName]: [RESULT] Статус верификации почты в облаке: $verified")
        if (verified) {
          firebaseService.addFcmToken()
          // ИСПРАВЛЕНО: Принудительно обновляем глобальный стейт перед переходом
          appScreenModel.evaluateStartDestination()
          onSuccess()
        }
      }
    }
  }

  // ====================================================================
  // --- БЛОК КРОСС-ПЛАТФОРМЕННОЙ GOOGLE АВТОРИЗАЦИИ --------------------
  // ====================================================================

  private val auth get() = Firebase.auth

  private suspend fun signInAndLinkWithGoogle(idToken: String) {
    val methodName = "signInAndLinkWithGoogle"
    val firebaseCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    val currentUser = auth.currentUser

    if (currentUser == null) {
      println("[YkisLogKMP.$className.$methodName]: [NEW_USER] Обычная авторизация в Firebase")
      auth.signInWithCredential(firebaseCredential)
    } else {
      println("[YkisLogKMP.$className.$methodName]: [LINK] Привязка провайдера Google к текущему аккаунту")
      currentUser.linkWithCredential(firebaseCredential)
    }
  }
  fun onSignUpWithGoogle(idToken: String, onFinishedNavigate: () -> Unit) {
    val methodName = "onSignUpWithGoogle"

    screenModelScope.launch {
      try {
        // Устанавливаем статус загрузки (если он еще не был установлен кнопкой)
        _isGoogleLoading.value = true

        // Выставляем доменный статус загрузки для внешних систем экрана
        _signInWithGoogleResponse.value = Resource.Loading()
        println("[YkisLogKMP.$className.$methodName]: [START] Фоновый лоадер Google запущен. Блокировка интерфейса активирована.")

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Шаг 1: Авторизация Firebase Auth...")
        signInAndLinkWithGoogle(idToken)

        // КРИТИЧНИЙ ФІКС: Віддаємо успіх Auth ВІДРАЗУ, не чекаючи синхронізації БД
        _signInWithGoogleResponse.value = Resource.Success(true)
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Авторизація пройдена. Перехід до додатку...")

        // Запускаємо фонову синхронізацію без блокування UI
        screenModelScope.launch {
            println("[YkisLogKMP.$className.$methodName]: [BACKGROUND] Запуск синхронізації профілю та FCM...")
            firebaseService.addUserFirestore()
            firebaseService.addFcmToken()
        }

        // Миттєво оновлюємо навігаційний стейт та переходимо
        appScreenModel.evaluateStartDestination()
        onFinishedNavigate()

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: Ошибка рантайма Google Credential Manager: ${e.message}")
        _signInWithGoogleResponse.value = Resource.Error(messageRes = Res.string.error_unknown)
        SnackbarManager.showMessage(Res.string.error_unknown)
      } finally {
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
    val formattedPhone = if (phone.startsWith("+")) phone else "+380$phone"

    // ЗАЩИТА ОТ ДРЕБЕЗГА
    if (_smsSendResponse.value is Resource.Loading) return

    if (phone.isBlank()) {
      SnackbarManager.showMessage(Res.string.empty_phone)
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Запрос SMS на номер: $formattedPhone")
      _smsSendResponse.value = Resource.Loading()

      // Вызываем наш expect/actual мост, который мы настроили на нативном уровне
      val result = firebaseService.sendSmsCode(phone, activityContext)

      if (result is Resource.Success) {
        currentVerificationId = result.data
        _authUiState.update { it.copy(isSmsSent = true) }
        _smsSendResponse.value = null // Очищаем статус лоадера отправки SMS
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Сессия SMS зафиксирована. Ожидание кода.")
        onSuccess()
      } else if (result is Resource.Error) {
        _smsSendResponse.value = Resource.Error(messageRes = Res.string.error_sms_failed)
        SnackbarManager.showMessage(Res.string.error_sms_failed)
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

    // Защита от дребезга
    if (_signInResponse.value is Resource.Loading) return

    if ((verificationId == null) || state.smsCode.isBlank()) {
      SnackbarManager.showMessage(Res.string.error_invalid_sms_code)
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [START] Проверка кода: ${state.smsCode}")
      _signInResponse.value = Resource.Loading()

      val result = firebaseService.signInWithSmsCode(verificationId, state.smsCode)

      if (result is Resource.Success) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Вхід схвалено. Запуск фонових процесів...")

        // КРИТИЧНИЙ ФІКС: Віддаємо успіх ВІДРАЗУ
        _signInResponse.value = Resource.Success(true)

        // Фонова синхронізація (не блокує перехід)
        screenModelScope.launch {
            println("[YkisLogKMP.$className.$methodName]: [BACKGROUND] Синхронізація Firestore та FCM...")
            firebaseService.addUserFirestore()
            firebaseService.addFcmToken()
            
            // Оновлення токена для надійності
            try {
              Firebase.auth.currentUser?.getIdToken(forceRefresh = true)
            } catch (e: Exception) { }
        }

        appScreenModel.evaluateStartDestination()
        onSuccess() // Бесшовно перенаправляет на MainApartmentScreen через replaceAll

      } else if (result is Resource.Error) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] Ошибка Firebase Auth")
        val friendlyErrorRes = if (result.message?.contains("invalid-verification-code", true) == true) 
          Res.string.error_invalid_sms_code else Res.string.error_unknown
        _signInResponse.value = Resource.Error(messageRes = friendlyErrorRes)
        SnackbarManager.showMessage(friendlyErrorRes)
      }
    }
  }






} // Конец AuthScreenModel
