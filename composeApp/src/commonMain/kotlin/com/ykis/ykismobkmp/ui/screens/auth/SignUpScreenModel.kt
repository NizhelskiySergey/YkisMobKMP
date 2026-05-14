package com.ykis.ykismobkmp.ui.screens.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import cafe.adriel.voyager.core.model.screenModelScope // Кроссплатформенный Scope вместо viewModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.ui.BaseScreenModel

private const val tag = "SignUpScreenModel"

/**
 * [SignUpScreenModel] — Кроссплатформенная модель экрана регистрации ЮКИС.
 * Унаследована от BaseScreenModel и полностью готова к сборке под Mac Desktop, Android и iOS.
 */
class SignUpScreenModel(
  private val firebaseService: FirebaseService,
  logService: LogService // ИСПРАВЛЕНО: Убран private val, теперь это просто аргумент для super()
) : BaseScreenModel(logService) { // ИСПРАВЛЕНО: Наследуемся от BaseScreenModel вместо BaseViewModel

  // ИСПРАВЛЕНО: Все начальные стейты ответов инициализированы с явным указанием Generic-типа <Boolean>
  private val _reloadUserResponse = MutableStateFlow<
    Resource<Boolean>>(Resource.Success<Boolean>(false))
  val reloadUserResponse: StateFlow<Resource<Boolean>> = _reloadUserResponse.asStateFlow()

  private val _signUpResponse = MutableStateFlow<Resource<Boolean>?>(null)
  val signUpResponse: StateFlow<Resource<Boolean>?> = _signUpResponse.asStateFlow()

  private val _sendEmailVerificationResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success<Boolean>(false))
  val sendEmailVerificationResponse: StateFlow<Resource<Boolean>> = _sendEmailVerificationResponse.asStateFlow()

  // ИСПРАВЛЕНО: Стейт полей ввода переведен на MutableStateFlow для 100% стабильности потоков на Mac JVM
  private val _authUiState = MutableStateFlow(AuthUiState())
  val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

  private val email: String get() = _authUiState.value.email
  private val password: String get() = _authUiState.value.password
  private val repeatPassword: String get() = _authUiState.value.repeatPassword

  val displayEmail: String
    get() = email.ifBlank { firebaseService.currentUser?.email ?: "" }

  val isEmailVerified: Boolean
    get() = firebaseService.currentUser?.isEmailVerified ?: false

  init {
    println("[$tag]: [INIT_START] Экран регистрации инициализирован в общем KMP-слое")
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

  /**
   * [isInputValid] — Кроссплатформенная валидация полей ввода без использования Android-логов.
   */
  private fun isInputValid(): Boolean {
    val methodName = "validate"
    if (!email.isValidEmail()) {
      println("[$tag.$methodName]: Некоректний email: $email")
      SnackbarManager.showMessage("Некоректний формат email")
      return false
    }
    if (!password.isValidPassword()) {
      println("[$tag.$methodName]: Пароль не пройшов перевірку складності")
      SnackbarManager.showMessage("Пароль занадто простий")
      return false
    }
    if (password != repeatPassword) {
      println("[$tag.$methodName]: Паролі не збігаються")
      SnackbarManager.showMessage("Введені паролі не збігаються")
      return false
    }
    return true
  }

  /**
   * [signUpWithEmailAndPassword] — Создание аккаунта в GitLive Firebase Auth.
   */
  fun signUpWithEmailAndPassword(onSuccess: () -> Unit) {
    val methodName = "signUpWithEmailAndPassword"
    if (!isInputValid()) return

    // СБРОС СТЕЙТА перед стартом, чтобы LaunchedEffect в UI среагировал корректно
    _signUpResponse.value = null

    // Используем встроенный в BaseScreenModel безопасный launchCatching
    launchCatching {
      println("[$tag.$methodName]: [START] Реєстрація аккаунту для: $email")
      _signUpResponse.value = Resource.Loading()

      // Вызываем очищенный КМР-метод из нашего FirebaseService
      val result = firebaseService.firebaseSignUpWithEmailAndPassword(email, password)
      _signUpResponse.value = result

      if (result is Resource.Success) {
        println("[$tag.$methodName]: [SUCCESS] Користувача створено. Надсилання email верифікації...")
        firebaseService.sendEmailVerification()

        // Вызываем привязку токена через наш очищенный FirebaseService
        firebaseService.addFcmToken()
        onSuccess()
      } else if (result is Resource.Error) {
        println("[$tag.$methodName]: [ERROR] Реєстрація відхилена: ${result.message}")
      }
    }
  }

  /**
   * [repeatEmailVerified] — Повторный запрос ссылки подтверждения на почту.
   */
  fun repeatEmailVerified() {
    val methodName = "repeatEmailVerified"
    val userEmail = firebaseService.currentUser?.email

    screenModelScope.launch {
      try {
        println("[$tag.$methodName]: [REQUEST] Надсилання листа на $userEmail")
        _sendEmailVerificationResponse.value = Resource.Loading()

        val result = firebaseService.sendEmailVerification()
        _sendEmailVerificationResponse.value = result

        if (result is Resource.Success) {
          println("[$tag.$methodName]: [SUCCESS] Лист успішно надіслано")
          SnackbarManager.showMessage("Лист для підтвердження надіслано на вашу пошту")
        } else if (result is Resource.Error) {
          val errorMsg = result.message ?: "Не вдалося відпустити лист"
          println("[$tag.$methodName]: [ERROR] Причина відмови Firebase: $errorMsg")
          SnackbarManager.showMessage(errorMsg)
        }
      } catch (e: Exception) {
        println("[$tag.$methodName]: [CRITICAL_ERROR] ${e.message}")
        _sendEmailVerificationResponse.value = Resource.Error(e.message ?: "Помилка")
      }
    }
  }

  /**
   * [reloadUser] — Проверка прохождения верификации по ссылке из почты.
   */
  fun reloadUser(onSuccess: () -> Unit) {
    val methodName = "reloadUser"
    launchCatching {
      println("[$tag.$methodName]: [START] Перевірка підтвердження пошти користувачем...")
      _reloadUserResponse.value = Resource.Loading()

      val result = firebaseService.reloadFirebaseUser()
      _reloadUserResponse.value = result

      if (result is Resource.Success) {
        val verified = firebaseService.currentUser?.isEmailVerified == true
        println("[$tag.$methodName]: [RESULT] Статус верифікації пошти в облаці: $verified")

        if (verified) {
          firebaseService.addFcmToken()
          onSuccess()
        } else {
          SnackbarManager.showMessage("Пошта ще не підтверджена. Перевірте вашу скриньку.")
        }
      }
    }
  }
}

// Кроссплатформенные extension-методы проверок строк, изолированные от Android SDK
private fun String.isValidEmail(): Boolean {
  val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
  return this.isNotBlank() && emailRegex.matches(this)
}

private fun String.isValidPassword(): Boolean = this.isNotBlank() && this.length >= 6
