package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.EmailField
import com.ykis.ykismobkmp.ui.components.LogoImage
import com.ykis.ykismobkmp.ui.components.PasswordField
import com.ykis.ykismobkmp.ui.components.RepeatPasswordField
import com.ykis.ykismobkmp.ui.screens.auth.AuthUiState
import org.koin.compose.koinInject

// Фиксируем константу тега для логирования, убирая Unresolved reference 'tag'
private const val tag = "SignUpScreen"

/**
 * [SignUpScreen] — Кроссплатформенный экран регистрации ЮКИС на базе Voyager Screen.
 * Стабилен на Mac Desktop (JVM) и мобильном Android/iOS.
 */
class SignUpScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current

    // Внедряем нашу кроссплатформенную ScreenModel (Регистрация)
    val screenModel = koinInject<SignUpScreenModel>()

    // Исправлено именование переменной состояния (с маленькой буквы для кодстайла)
    val authUiState by screenModel.authUiState.collectAsState()
    val signUpResponse by screenModel.signUpResponse.collectAsState()

    // Стримим и обрабатываем ответы сервера реактивно
    LaunchedEffect(signUpResponse) {
      when (val response = signUpResponse) {
        is Resource.Success -> {
          println("[$tag]: [SUCCESS] Реєстрація успішна. Перезапуск графа авторизації.")
          // Согласно твоему сценарию: после успешной регистрации сбрасываем стек.
          // FirebaseService обновит currentUser, и RootNavGraph сразу перенаправит юзера на AddApartmentScreen
          navigator.popUntilRoot()
        }
        is Resource.Error -> {
          println("[$tag]: [ERROR] ${response.message}")
          val errorMessage = response.message ?: "Помилка реєстрації"
          SnackbarManager.showMessage(errorMessage)
        }
        is Resource.Loading -> {
          println("[$tag]: [LOADING] Надсилання даних на сервери Firebase...")
        }
        else -> {}
      }
    }

    SignUpScreenStateless(
      authUiState = authUiState,
      navigateBack = { navigator.pop() },
      onEmailChange = screenModel::onEmailChange,
      onPasswordChange = screenModel::onPasswordChange,
      onRepeatPasswordChange = screenModel::onRepeatPasswordChange,
      onSignUpClick = {
        keyboard?.hide()
        screenModel.signUpWithEmailAndPassword {
          println("[$tag]: [ACTION] Метод реєстрації в ScreenModel запущено")
        }
      },
      isLoading = signUpResponse is Resource.Loading
    )
  }
}

@Composable
fun SignUpScreenStateless(
  modifier: Modifier = Modifier,
  authUiState: AuthUiState, // Ссылка на стейт полей ввода
  navigateBack: () -> Unit,
  onEmailChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onRepeatPasswordChange: (String) -> Unit,
  onSignUpClick: () -> Unit,
  isLoading: Boolean
) {
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
      DefaultAppBar(
        title = "Реєстрація",
        canNavigateBack = true,
        onBackClick = {
          println("[$tag.Stateless]: [BACK_CLICK] Повернення на екран входу")
          navigateBack()
        }
      )
      Column(
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(16.dp))
        LogoImage()
        Spacer(modifier = Modifier.height(16.dp))

        EmailField(authUiState.email, onEmailChange, modifier)
        Spacer(modifier = Modifier.height(8.dp))

        PasswordField(authUiState.password, onPasswordChange)
        Spacer(modifier = Modifier.height(8.dp))

        RepeatPasswordField(authUiState.repeatPassword, onRepeatPasswordChange, modifier)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            println("[$tag.Stateless]: [SUBMIT_CLICK] Спроба створення акаунту для: ${authUiState.email}")
            onSignUpClick()
          },
          enabled = !isLoading
        ) {
          AnimatedContent(targetState = isLoading, label = "loading_anim") { loading ->
            if (loading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = "Зареєструватися",
                style = MaterialTheme.typography.titleMedium
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}


