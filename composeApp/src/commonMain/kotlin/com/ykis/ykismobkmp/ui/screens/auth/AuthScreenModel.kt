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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "AuthScreenModel"

class AuthScreenModel(
  private val firebaseService: FirebaseService,
  private val appScreenModel: AppScreenModel,
  logService: LogService,
) : BaseScreenModel(logService) {

  private val _authUiState = MutableStateFlow(AuthUiState())
  val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

  private val _signInResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val signInResponse: StateFlow<Resource<Boolean>> = _signInResponse.asStateFlow()

  private val _signInWithGoogleResponse = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
  val signInWithGoogleResponse: StateFlow<Resource<Boolean>> = _signInWithGoogleResponse.asStateFlow()

  private val _signUpResponse = MutableStateFlow<Resource<Boolean>?>(null)
  val signUpResponse: StateFlow<Resource<Boolean>?> = _signUpResponse.asStateFlow()

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

  val email: String get() = _authUiState.value.email
  private val password: String get() = _authUiState.value.password

  val displayEmail: String
    get() = email.ifBlank { firebaseService.currentUser?.email ?: "" }

  init {
    println("[YkisLogKMP.$className.init]: менеджер AuthScreenModel успішно ініціалізований")
  }

  fun onEmailChange(newValue: String) { _authUiState.update { it.copy(email = newValue.trim()) } }
  fun onPasswordChange(newValue: String) { _authUiState.update { it.copy(password = newValue) } }
  fun onRepeatPasswordChange(newValue: String) { _authUiState.update { it.copy(repeatPassword = newValue) } }

  private fun isValidEmailKmp(target: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    return target.isNotBlank() && emailRegex.matches(target)
  }

  private fun mapFirebaseError(message: String?): StringResource {
    if (message == null) return Res.string.error_unknown
    return when {
      message.contains("user-not-found", true) -> Res.string.error_user_not_found
      message.contains("wrong-password", true) -> Res.string.error_wrong_password
      else -> Res.string.error_unknown
    }
  }

  fun onSignInClick(onSuccessNavigate: () -> Unit) {
    if (_signInResponse.value is Resource.Loading) return
    if (!isValidEmailKmp(email)) { SnackbarManager.showMessage(Res.string.email_error); return }
    screenModelScope.launch {
      try {
        _signInResponse.value = Resource.Loading()
        firebaseService.firebaseSignInWithEmailAndPassword(email, password)
        _signInResponse.value = Resource.Success(true)
        firebaseService.addFcmToken()
        appScreenModel.evaluateStartDestination()
        onSuccessNavigate()
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.onSignInClick]: [ERROR] Спіймано виключення: ${e.message}")
        _signInResponse.value = Resource.Error(message = e.message ?: "Firebase error")
        // Виводимо текст самої помилки, щоб побачити код
        SnackbarManager.showMessage("Помилка: ${e.message}")
      }
    }
  }

  fun signUpWithEmailAndPassword(onSuccess: () -> Unit) {
    if (!isValidEmailKmp(email) || password.length < 6) return
    screenModelScope.launch {
      try {
        _signUpResponse.value = Resource.Loading()
        val result = firebaseService.firebaseSignUpWithEmailAndPassword(email, password)
        _signUpResponse.value = result
        if (result is Resource.Success) onSuccess()
      } catch (e: Exception) { _signUpResponse.value = Resource.Error(message = e.message) }
    }
  }

  fun onForgotPasswordClick() {
    screenModelScope.launch { firebaseService.sendPasswordResetEmail(email); SnackbarManager.showMessage(Res.string.recovery_email_sent) }
  }

  fun reloadUser(onSuccess: () -> Unit) {
    screenModelScope.launch {
      _reloadUserResponse.value = Resource.Loading()
      val result = firebaseService.reloadFirebaseUser()
      if (result is Resource.Success && Firebase.auth.currentUser?.isEmailVerified == true) {
          firebaseService.addFcmToken()
          appScreenModel.evaluateStartDestination()
          onSuccess()
      }
      _reloadUserResponse.value = result
    }
  }

  fun onSignUpWithGoogle(idToken: String, onFinishedNavigate: () -> Unit) {
    screenModelScope.launch {
      try {
        _isGoogleLoading.value = true
        _signInWithGoogleResponse.value = Resource.Loading()
        val result = firebaseService.firebaseSignInWithGoogle(idToken)
        if (result is Resource.Success) {
          firebaseService.addUserFirestore()
          firebaseService.addFcmToken()
          appScreenModel.evaluateStartDestination()
          onFinishedNavigate()
        }
      } catch (e: Exception) { SnackbarManager.showMessage(Res.string.error_unknown) }
      finally { _isGoogleLoading.value = false; _signInWithGoogleResponse.value = Resource.Success(false) }
    }
  }

  fun onSignUpWithApple(idToken: String, onFinishedNavigate: () -> Unit) {
    screenModelScope.launch {
      try {
        _isGoogleLoading.value = true
        // Використовуємо той самий стейт для спрощення
        _signInWithGoogleResponse.value = Resource.Loading()
        val result = firebaseService.firebaseSignInWithApple(idToken)
        if (result is Resource.Success) {
          firebaseService.addUserFirestore()
          firebaseService.addFcmToken()
          appScreenModel.evaluateStartDestination()
          onFinishedNavigate()
        }
      } catch (e: Exception) { 
          println("[YkisLogKMP.$className]: Apple Auth Error: ${e.message}")
          SnackbarManager.showMessage(Res.string.error_unknown) 
      }
      finally { _isGoogleLoading.value = false; _signInWithGoogleResponse.value = Resource.Success(false) }
    }
  }

  fun repeatEmailVerified() {
    screenModelScope.launch {
      val result = firebaseService.sendEmailVerification()
      if (result is Resource.Success) SnackbarManager.showMessage(Res.string.verify_email_message)
    }
  }

  fun signOutFromVerifyScreen(onSuccess: () -> Unit) {
    screenModelScope.launch {
      firebaseService.signOut()
      appScreenModel.evaluateStartDestination()
      onSuccess()
    }
  }

  fun onPhoneChange(newValue: String) { _authUiState.update { it.copy(phoneNumber = newValue) } }
  fun onSmsCodeChange(newValue: String) { _authUiState.update { it.copy(smsCode = newValue) } }
  fun setSmsSentState(isSent: Boolean) { _authUiState.update { it.copy(isSmsSent = isSent) } }

  fun triggerSmsCode(activityContext: Any?, onCodeSent: () -> Unit) {
    screenModelScope.launch {
      _smsSendResponse.value = Resource.Loading()
      val result = firebaseService.sendSmsCode(_authUiState.value.phoneNumber, activityContext)
      if (result is Resource.Success) { currentVerificationId = result.data; setSmsSentState(true); onCodeSent() }
      _smsSendResponse.value = null
    }
  }

  fun verifySmsAndSignIn(onSuccess: () -> Unit) {
    val state = _authUiState.value
    val vId = currentVerificationId ?: return
    screenModelScope.launch {
      _signInResponse.value = Resource.Loading()
      val result = firebaseService.signInWithSmsCode(vId, state.smsCode)
      if (result is Resource.Success) {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
          firebaseService.addUserFirestore()
        }
        _signInResponse.value = Resource.Success(true)
        firebaseService.addUserFirestore(); firebaseService.addFcmToken(); appScreenModel.evaluateStartDestination(); onSuccess()
      } else { SnackbarManager.showMessage(Res.string.error_invalid_sms_code) }
    }
  }
}
