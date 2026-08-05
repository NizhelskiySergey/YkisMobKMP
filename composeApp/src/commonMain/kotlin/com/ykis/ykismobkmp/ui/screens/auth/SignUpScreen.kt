package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.ykis.ykismobkmp.ui.navigation.VerifyEmailScreenDest
import org.koin.compose.koinInject

import org.jetbrains.compose.resources.stringResource
import com.ykis.ykismobkmp.*

private const val className = "SignUpScreen"

/**
 * [SignUpScreen] — Кроссплатформенный экран регистрации абонента расчетного центра ЮКИС.
 */
object SignUpScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current

    // Внедряем нашу монолитную объединенную модель AuthScreenModel YkisMobKMP
    val screenModel = koinInject<AuthScreenModel>()

    val authUiState by screenModel.authUiState.collectAsState()
    val signUpResponse by screenModel.signUpResponse.collectAsState()

    // Стримим и обрабатываем ответы сервера реактивно
    LaunchedEffect(signUpResponse) {
      when (val response = signUpResponse) {
        is Resource.Success -> {
          println("[YkisLogKMP.$className.Content]: [SUCCESS] Реєстрація успішна. Перехід на екран підтвердження пошти.")

          // ИСПРАВЛЕНО: Нативно пушаем синглтон VerifyEmailScreen в стек Voyager без круглых скобок ()
          navigator.push(VerifyEmailScreenDest)
        }
        is Resource.Error -> {
          println("[YkisLogKMP.$className.Content]: [ERROR] ${response.message}")
          val errorMessage = response.message ?: "Помилка реєстрації"
          SnackbarManager.showMessage(errorMessage)
        }
        is Resource.Loading -> {
          println("[YkisLogKMP.$className.Content]: [LOADING] Надсилання даних на сервери Firebase...")
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
          println("[YkisLogKMP.$className.Content]: [ACTION] Метод реєстрації в ScreenModel успішно виконано")
        }
      },
      isLoading = signUpResponse is Resource.Loading
    )
  }
}

/**
 * [SignUpScreenStateless] — Декларативная верстка разметки полей ввода регистрации.
 */
@Composable
fun SignUpScreenStateless(
  modifier: Modifier = Modifier,
  authUiState: AuthUiState,
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
        title = stringResource(Res.string.sign_up_title),
        canNavigateBack = true,
        onBackClick = {
          println("[YkisLogKMP.$className.SignUpScreenStateless]: [BACK_CLICK] Повернення на екран входу")
          navigateBack()
        }
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(16.dp))
        LogoImage()
        Spacer(modifier = Modifier.height(16.dp))

        EmailField(
          value = authUiState.email,
          onNewValue = onEmailChange,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        PasswordField(
          value = authUiState.password,
          onNewValue = onPasswordChange,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        RepeatPasswordField(
          value = authUiState.repeatPassword,
          onNewValue = onRepeatPasswordChange,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            println("[YkisLogKMP.$className.SignUpScreenStateless]: [SUBMIT_CLICK] Спроба створення акаунту для: ${authUiState.email}")
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
                text = stringResource(Res.string.sign_up_button),
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



