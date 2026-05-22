package com.ykis.ykismobkmp.ui.screens.auth
import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.BaseScreenModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val className = "SignInScreenModel"

/**
 * [SignInScreenModel] — Вьюмодель экрана входа пользователей ИС ЮКИС г. Южный.
 * ИСПРАВЛЕНО: Сквозные префиксы логирования приведены к единому стандарту [YkisLogKMP].
 * Намертво зафиксирован для полной замены.
 */
class SignInScreenModel(
  private val firebaseService: FirebaseService,
  logService: LogService
) : BaseScreenModel(logService) {

  // Реактивное состояние полей ввода (Email, Пароль) через StateFlow для стабильности на Mac
  private val _AuthUiState = MutableStateFlow(AuthUiState())
  val AuthUiState: StateFlow<AuthUiState> = _AuthUiState.asStateFlow()

  // Реактивные состояния ответов для UI-лоадеров
  private val _signInWithGoogleResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val signInWithGoogleResponse: StateFlow<Resource<Boolean>> = _signInWithGoogleResponse.asStateFlow()

  private val _signInResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val signInResponse: StateFlow<Resource<Boolean>> = _signInResponse.asStateFlow()

  private val isEmailVerified: Boolean
    get() = Firebase.auth.currentUser?.isEmailVerified ?: false

  init {
    println("[YkisLogKMP.$className.init]: Экран входа инициализирован на платформе")
  }

  fun onEmailChange(newValue: String) {
    _AuthUiState.update { it.copy(email = newValue) }
  }

  fun onPasswordChange(newValue: String) {
    _AuthUiState.update { it.copy(password = newValue) }
  }

  // --- ОБЫЧНЫЙ ВХОД ПО EMAIL ---
  fun onSignInClick(onSuccessNavigate: () -> Unit) {
    val methodName = "onSignInClick"
    val currentEmail = _AuthUiState.value.email
    val currentPassword = _AuthUiState.value.password

    if (!currentEmail.isValidEmailKmp()) {
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

      // Прямой кроссплатформенный вызов GitLive Firebase Auth
      Firebase.auth.signInWithEmailAndPassword(
        email = currentEmail,
        password = currentPassword
      )

      firebaseService.addFcmToken()

      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Авторизация успешна. Verified: $isEmailVerified")
      _signInResponse.value = Resource.Success(true)

      onSuccessNavigate()
    }
  }

  // --- ВОССТАНОВЛЕНИЕ ПАРОЛЯ ---
  fun onForgotPasswordClick() {
    val methodName = "onForgotPasswordClick"
    val currentEmail = _AuthUiState.value.email
    if (!currentEmail.isValidEmailKmp()) {
      SnackbarManager.showMessage("Некоректний формат email")
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [RECOVERY] Запрос восстановления на почту $currentEmail")
      firebaseService.sendRecoveryEmail(currentEmail)
      SnackbarManager.showMessage("Лист для відновлення паролю надіслано")
    }
  }

  // --- КРОСС ПЛАТФОРМЕННЫЙ GOOGLE AUTH (GitLive Firebase) ---
  private suspend fun signInAndLinkWithGoogle(idToken: String) {
    val methodName = "signInAndLinkWithGoogle"
    val firebaseCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    val currentUser = Firebase.auth.currentUser

    if (currentUser == null) {
      println("[YkisLogKMP.$className.$methodName]: [NEW_USER] Обычная авторизация в Firebase")
      Firebase.auth.signInWithCredential(firebaseCredential)
    } else {
      println("[YkisLogKMP.$className.$methodName]: [LINK] Привязка провайдера Google к текущему аккаунту")
      currentUser.linkWithCredential(firebaseCredential)
    }
  }

  /**
   * [onSignUpWithGoogle] — Вызывается при получении ID Токена от нативной кнопки GoogleAuthButton.
   */
  fun onSignUpWithGoogle(idToken: String, onFinishedNavigate: () -> Unit) {
    val methodName = "onSignUpWithGoogle"

    screenModelScope.launch {
      try {
        _signInWithGoogleResponse.value = Resource.Loading()
        println("[YkisLogKMP.$className.$methodName]: [START] Фоновый лоадер запущен. Токен получен.")

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Шаг 1: Авторизация Firebase...")
        signInAndLinkWithGoogle(idToken)

        println("[YkisLogKMP.$className.$methodName]: [PROCESS] Шаг 2: Синхронизация БД и профиля...")
        val dbResult = firebaseService.addUserFirestore()

        if (dbResult is Resource.Error) {
          println("[YkisLogKMP.$className.$methodName]: [ERROR] Ошибка при сохранении профиля в Firestore")
          _signInWithGoogleResponse.value = dbResult
          return@launch
        }

        println("[YkisLogKMP.$className.$methodName]: [FCM] Привязка токена для нового Google-аккаунта")
        firebaseService.addFcmToken()

        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Все проверки пройдены. Вызов навигации.")
        _signInWithGoogleResponse.value = Resource.Success(true)

        onFinishedNavigate()

      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [CRITICAL] Ошибка рантайма: ${e.message}")
        _signInWithGoogleResponse.value = Resource.Error(message = e.message ?: "Невідома помилка")
        SnackbarManager.showMessage("Помилка входу Google: ${e.message}")
      }
    }
  }
}

// Простая КМР проверка валидности Email
private fun String.isValidEmailKmp(): Boolean {
  val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
  return this.isNotBlank() && emailRegex.matches(this)
}

