package com.ykis.ykismobkmp.ui.screens.auth


import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.screens.auth.AuthUiState
import com.ykis.ykismobkmp.ui.BaseScreenModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val tag = "SignInScreenModel"

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
    get() = firebaseService.currentUser?.isEmailVerified ?: false

  init {
    println("[$tag]: [INIT_START] Экран входа инициализирован на платформе")
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

    if (!currentEmail.isValidEmail()) {
      println("[$tag.$methodName]: [VALIDATION_ERROR] Некорректный email")
      SnackbarManager.showMessage("Некоректний формат email")
      return
    }

    if (currentPassword.isBlank()) {
      println("[$tag.$methodName]: [VALIDATION_ERROR] Пустой пароль")
      SnackbarManager.showMessage("Пароль не може бути порожнім")
      return
    }

    // Используем встроенный в BaseScreenModel безопасный launchCatching
    // Внутри SignInScreenModel.kt -> fun onSignInClick

    launchCatching {
      println("[$tag.$methodName]: [START] Запрос авторизации для $currentEmail")
      _signInResponse.value = Resource.Loading()

      // РЕШЕНИЕ: Прямой кроссплатформенный вызов GitLive Firebase Auth
      dev.gitlive.firebase.Firebase.auth.signInWithEmailAndPassword(
        email = currentEmail,
        password = currentPassword
      )

      firebaseService.addFcmToken()

      println("[$tag.$methodName]: [SUCCESS] Авторизация успешна. Verified: $isEmailVerified")
      _signInResponse.value = Resource.Success(true)

      onSuccessNavigate()
    }

  }

  // --- ВОССТАНОВЛЕНИЕ ПАРОЛЯ ---
  fun onForgotPasswordClick() {
    val currentEmail = _AuthUiState.value.email
    if (!currentEmail.isValidEmail()) {
      SnackbarManager.showMessage("Некоректний формат email")
      return
    }

    launchCatching {
      println("[$tag]: [RECOVERY] Запрос восстановления на почту $currentEmail")
      firebaseService.sendRecoveryEmail(currentEmail)
      SnackbarManager.showMessage("Лист для відновлення паролю надіслано")
    }
  }

  // --- КРОСС ПЛАТФОРМЕННЫЙ GOOGLE AUTH (GitLive Firebase) ---
  private suspend fun signInAndLinkWithGoogle(idToken: String) {
    // ИСПРАВЛЕНО: Кроссплатформенный вызов GitLive SDK, стабильный на Mac и Android
    val firebaseCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
    val currentUser = Firebase.auth.currentUser

    if (currentUser == null) {
      println("[$tag.linkGoogle]: [NEW_USER] Обычная авторизация в Firebase")
      Firebase.auth.signInWithCredential(firebaseCredential)
    } else {
      println("[$tag.linkGoogle]: [LINK] Привязка провайдера Google к текущему аккаунту")
      currentUser.linkWithCredential(firebaseCredential)
    }
  }

  /**
   * [onSignUpWithGoogle] — Вызывается при получении ID Токена от нативной кнопки GoogleAuthButton.
   * Полностью очищен от Android-типов данных.
   */
  fun onSignUpWithGoogle(idToken: String, onFinishedNavigate: () -> Unit) {
    val methodName = "onGoogleLogin"

    // ИСПРАВЛЕНО: Используем screenModelScope вместо viewModelScope для Voyager
    screenModelScope.launch {
      try {
        // 1. ВКЛЮЧАЕМ ЛОАДЕР ДЛЯ UI
        _signInWithGoogleResponse.value = Resource.Loading()
        println("[$tag.$methodName]: [START] Фоновый лоадер запущен. Токен получен.")

        // 2. Входим/Линкуем в Firebase через общий KMP токен строкой
        println("[$tag.$methodName]: [PROCESS] Шаг 1: Авторизация Firebase...")
        signInAndLinkWithGoogle(idToken)

        // 3. Сохраняем и синхронизируем профиль в Firestore
        println("[$tag.$methodName]: [PROCESS] Шаг 2: Синхронизация БД и профиля...")
        val dbResult = firebaseService.addUserFirestore()

        if (dbResult is Resource.Error) {
          println("[$tag.$methodName]: [ERROR] Ошибка при сохранении профиля в Firestore")
          _signInWithGoogleResponse.value = dbResult
          return@launch
        }

        // 4. Привязка токена push-уведомлений FCM
        println("[$tag.$methodName]: [FCM] Привязка токена для нового Google-аккаунта")
        firebaseService.addFcmToken()

        // 5. ПОЛНЫЙ УСПЕХ СБОРКИ
        println("[$tag.$methodName]: [SUCCESS] Все проверки пройдены. Вызов навигации.")
        _signInWithGoogleResponse.value = Resource.Success(true)

        // Лямбда выполнит navigator.popUntilRoot() в UI
        onFinishedNavigate()

      } catch (e: Exception) {
        println("[$tag.$methodName]: [CRITICAL] Ошибка рантайма: ${e.message}")
        _signInWithGoogleResponse.value = Resource.Error(e.localizedMessage ?: "Невідома помилка")
        SnackbarManager.showMessage("Помилка входу Google: ${e.localizedMessage}")
      }
    }
  }
}

// Простая KMP проверка валидности Email
private fun String.isValidEmail(): Boolean {
  val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
  return this.isNotBlank() && emailRegex.matches(this)
}

