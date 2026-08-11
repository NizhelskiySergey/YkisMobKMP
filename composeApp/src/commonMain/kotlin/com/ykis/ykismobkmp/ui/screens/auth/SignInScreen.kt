package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.AppConfig
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.platformActivityContext
import com.ykis.ykismobkmp.core.utils.triggerNativeGoogleSignIn
import com.ykis.ykismobkmp.forgot_password_link
import com.ykis.ykismobkmp.getPlatform
import com.ykis.ykismobkmp.login_details
import com.ykis.ykismobkmp.no_account_text
import com.ykis.ykismobkmp.or_divider
import com.ykis.ykismobkmp.sign_in_button
import com.ykis.ykismobkmp.sign_in_with_google
import com.ykis.ykismobkmp.sign_up_link
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.EmailField
import com.ykis.ykismobkmp.ui.components.LogoImage
import com.ykis.ykismobkmp.ui.components.PasswordField
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.navigation.SignUpScreenDest
import com.ykis.ykismobkmp.ui.navigation.VerifyEmailScreenDest
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val className = "SignInScreen"

@Composable
fun AppleAuthButton(isLoading: Boolean, onStart: () -> Unit, onDataReceived: (String, String?, String?) -> Unit) {
    val platform = getPlatform().name
    println("[YkisLogKMP.AppleAuthButton]: Поточна платформа: $platform")

    val isApplePlatform = platform.contains("iOS", true) || 
                          platform.contains("iPad", true) || 
                          platform.contains("Darwin", true) || 
                          platform.contains("Mac", true) ||
                          platform.contains("Apple", true)

    if (!isApplePlatform) return

    Button(
        onClick = {
            onStart()
            println("[YkisLogKMP.AppleAuthButton]: [START] Запуск нативної авторизації Apple")
            com.ykis.ykismobkmp.core.utils.triggerNativeAppleSignIn(
                onTokenReceived = { token, nonce, authCode ->
                    println("[YkisLogKMP.AppleAuthButton]: [SUCCESS] Apple Token, Nonce та AuthCode отримано")
                    onDataReceived(token, nonce, authCode)
                },
                onError = { error ->
                    println("[YkisLogKMP.AppleAuthButton]: [ERROR] $error")
                    if (error != "Canceled") {
                        SnackbarManager.showMessage(error)
                    }
                }
            )
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.surface,
                strokeWidth = 2.dp
            )
        } else {
            Text("Увійти через Apple", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun GoogleAuthButton(isLoading: Boolean, onStart: () -> Unit, onError: () -> Unit, onTokenReceived: (String) -> Unit) {
  val contextActivity = platformActivityContext()
  val platform = getPlatform().name
  
  val isSupported = !platform.contains("Java", true) && !platform.contains("JVM", true)

  if (!isSupported) return

  Button(
    onClick = {
      onStart()
      println("[YkisLogKMP.GoogleAuthButton]: [START] Запуск вікна Google Auth для платформи: $platform")
      
      triggerNativeGoogleSignIn(
        activityContext = contextActivity,
        onTokenReceived = { realIdToken ->
          println("[YkisLogKMP.GoogleAuthButton]: [SUCCESS] Токен отримано")
          onTokenReceived(realIdToken)
        },
        onError = { errorMsg ->
          println("[YkisLogKMP.GoogleAuthButton]: [ERROR] Сбій: $errorMsg")
          if (errorMsg != "Canceled") {
              SnackbarManager.showMessage(errorMsg)
          }
          onError()
        }
      )
    },
    enabled = !isLoading,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp
      )
    } else {
      Text(stringResource(Res.string.sign_in_with_google))
    }
  }
}

object SignInScreen : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current
    val screenModel = koinInject<AuthScreenModel>()
    val singInUiState by screenModel.authUiState.collectAsState()
    val signInResponse by screenModel.signInResponse.collectAsState()
    val isAuthLoading = signInResponse is Resource.Loading
    val isGoogleLoading by screenModel.isGoogleLoading.collectAsState()
    
    SignInScreenStateless(
      email = singInUiState.email,
      onEmailChange = screenModel::onEmailChange,
      password = singInUiState.password,
      onPasswordChange = screenModel::onPasswordChange,
      isAuthLoading = isAuthLoading,
      isGoogleLoading = isGoogleLoading,
      onSignInClick = {
        keyboard?.hide()
        screenModel.onSignInClick {
          val activeUser = Firebase.auth.currentUser
          val isVerified = activeUser?.isEmailVerified ?: false
          if (!isVerified) {
            println("[YkisLogKMP.$className.Content]: Вхід успішний, але пошта НЕ підтверджена. Навігація на VerifyEmailScreen.")
            navigator.push(VerifyEmailScreenDest)
          } else {
            println("[YkisLogKMP.$className.Content]: [SUCCESS] Вхід за Email успішний. Передано під контроль реактивного ядра.")
          }
        }
      },
      onForgotPasswordClick = { screenModel.onForgotPasswordClick() },
      onSignUpClick = {
        println("[YkisLogKMP.$className.Content]: [NAVIGATION] Перехід на екран реєстрації SignUpScreen")
        navigator.push(SignUpScreenDest)
      },
      onAppleDataReceived = { token, nonce, authCode ->
        println("[YkisLogKMP.$className.Content]: [EVENT] Отримано Apple ID Token, Nonce та AuthCode. Запуск входу...")
        screenModel.onSignUpWithApple(idToken = token, nonce = nonce, authCode = authCode) {
          println("[YkisLogKMP.$className.Content]: [SUCCESS] Успіх Apple Auth.")
        }
      },
      onGoogleTokenReceived = { idToken ->
        println("[YkisLogKMP.$className.Content]: [EVENT] Получен Google ID Token. Запуск авторизации...")
        screenModel.onSignUpWithGoogle(idToken) {
          println("[YkisLogKMP.$className.Content]: [SUCCESS] Успех Google Auth.")
        }
      },
      onGoogleStart = { screenModel.setGoogleLoading(true) },
      onGoogleError = { screenModel.setGoogleLoading(false) }
    )
  }
}

@Composable
fun SignInScreenStateless(
  modifier: Modifier = Modifier,
  email: String,
  onEmailChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  isAuthLoading: Boolean,
  onSignInClick: () -> Unit,
  onForgotPasswordClick: () -> Unit,
  onSignUpClick: () -> Unit,
  onGoogleTokenReceived: (String) -> Unit,
  onAppleDataReceived: (String, String?, String?) -> Unit,
  onGoogleStart: () -> Unit,
  onGoogleError: () -> Unit,
  isGoogleLoading: Boolean
) {
  val globalLoading = isGoogleLoading || isAuthLoading
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .widthIn(max = 460.dp),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      DefaultAppBar(title = stringResource(Res.string.login_details), canNavigateBack = false)
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(16.dp))
        LogoImage()
        Spacer(modifier = Modifier.height(32.dp))

        // Вкладки видалено, залишено тільки вхід за Email
        EmailField(
          value = email,
          onNewValue = onEmailChange,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        PasswordField(
          value = password,
          onNewValue = onPasswordChange,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.CenterEnd
        ) {
          Text(
            modifier = Modifier
              .clip(MaterialTheme.shapes.medium)
              .clickable { onForgotPasswordClick() }
              .padding(6.dp),
            text = stringResource(Res.string.forgot_password_link),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
          modifier = Modifier.fillMaxWidth(),
          enabled = !globalLoading,
          onClick = { onSignInClick() },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = stringResource(Res.string.sign_in_button), 
            style = MaterialTheme.typography.titleMedium
          )
        }
        
        Row(
          modifier = Modifier.padding(vertical = 24.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier.weight(1f).height(1.dp)
              .background(MaterialTheme.colorScheme.outlineVariant)
          )
          Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = stringResource(Res.string.or_divider),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 14.sp
          )
          Box(
            modifier = Modifier.weight(1f).height(1.dp)
              .background(MaterialTheme.colorScheme.outlineVariant)
          )
        }
        
        GoogleAuthButton(
          isLoading = isGoogleLoading,
          onStart = onGoogleStart,
          onError = onGoogleError,
          onTokenReceived = onGoogleTokenReceived
        )
        AppleAuthButton(
          isLoading = isGoogleLoading, 
          onStart = onGoogleStart,
          onDataReceived = onAppleDataReceived
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
          modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = stringResource(Res.string.no_account_text), style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            modifier = Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable { onSignUpClick() }
              .padding(4.dp),
            color = MaterialTheme.colorScheme.primary,
            text = stringResource(Res.string.sign_up_link),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}
