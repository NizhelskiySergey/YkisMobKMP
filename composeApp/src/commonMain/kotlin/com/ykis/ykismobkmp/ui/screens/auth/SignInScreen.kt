package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.EmailField
import com.ykis.ykismobkmp.ui.components.LogoImage
import com.ykis.ykismobkmp.ui.components.PasswordField
import org.koin.compose.koinInject

private const val tag = "SignInScreen"

class SignInScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current

    // Внедряем нашу кроссплатформенную ScreenModel
    val screenModel = koinInject<SignInScreenModel>()
    val singInUiState by screenModel.AuthUiState.collectAsState()
    val googleResponse by screenModel.signInWithGoogleResponse.collectAsState()

    // Проверяем статус фонового процесса Google авторизации
    val isGoogleLoading = googleResponse is Resource.Loading

    SignInScreenStateless(
      email = singInUiState.email,
      onEmailChange = screenModel::onEmailChange,
      password = singInUiState.password,
      onPasswordChange = screenModel::onPasswordChange,
      isGoogleLoading = isGoogleLoading,
      onSignInClick = {
        keyboard?.hide()
        // Запускаем вход по Email и передаем лямбду успешной навигации
        screenModel.onSignInClick {
          // После входа RootNavGraph сам решит, куда вести,
          // но если нужно форсировать — сбрасываем стек
          println("[$tag]: Успішний вхід по Email. Перезапуск графа навигации.")
        }
      },
      onForgotPasswordClick = { screenModel.onForgotPasswordClick() },
      onSignUpClick = {
        // Согласно твоему сценарию: Если у него нет аккаунта, переходим на SignUpScreen
        println("[$tag]: [NAVIGATION] Перехід на екран реєстрації SignUpScreen")
        navigator.push(SignUpScreen())
      },
      onGoogleTokenReceived = { idToken ->
        println("[$tag]: [EVENT] Отримано Google ID Token. Запуск авторизації в Firebase...")
        screenModel.onSignUpWithGoogle(idToken) {
          println("[$tag]: [NAVIGATE] Успіх Google Auth. Очищення стека на увійти.")
          // Полностью сбрасываем навигацию, так как юзер вошел в систему
          navigator.popUntilRoot()
        }
      }
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
  onSignInClick: () -> Unit,
  onForgotPasswordClick: () -> Unit,
  onSignUpClick: () -> Unit,
  onGoogleTokenReceived: (String) -> Unit, // Принимаем токен строкой
  isGoogleLoading: Boolean
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = modifier
        .fillMaxHeight()
        .widthIn(max = 460.dp),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Заменено stringResource(R.string) на обычные строки для кроссплатформенности
      DefaultAppBar(title = "Вхід в систему", canNavigateBack = false)

      Column(
        modifier = modifier
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        LogoImage()
        Spacer(modifier = Modifier.height(16.dp))

        EmailField(email, onEmailChange, modifier)
        Spacer(modifier = Modifier.height(8.dp))

        PasswordField(password, onPasswordChange)

        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
          Text(
            modifier = modifier
              .clip(MaterialTheme.shapes.medium)
              .clickable { onForgotPasswordClick() }
              .padding(4.dp),
            text = "Забули пароль?"
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
          modifier = modifier.fillMaxWidth(),
          onClick = onSignInClick,
          enabled = !isGoogleLoading
        ) {
          Text(text = "Увійти", style = MaterialTheme.typography.titleMedium)
        }

        Row(
          modifier = modifier.padding(vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
          Text(modifier = modifier.padding(horizontal = 4.dp), text = "або", color = MaterialTheme.colorScheme.outline)
          Box(modifier = modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
        }

        // ВЫЗОВ НАШЕЙ ПЛАТФОРМЕННОЙ КНОПКИ GOOGLE:
        // ID ресурсов временно заменяем на заглушки/строки под твой менеджер ресурсов
        GoogleAuthButton(
          buttonTextRes = 0, // Или передай строковый ключ локализации
          isLoading = isGoogleLoading,
          onTokenReceived = { token -> onGoogleTokenReceived(token) }
        )

        Spacer(modifier = modifier.height(8.dp))
        Row(
          modifier = modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Немає аккаунту?")
          Spacer(Modifier.width(4.dp))
          Text(
            modifier = modifier
              .clip(MaterialTheme.shapes.small)
              .clickable { onSignUpClick() },
            color = MaterialTheme.colorScheme.primary,
            text = "Реєстрація"
          )
        }
      }
    }
  }
}

