package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import org.koin.compose.koinInject

// Импорты твоих внутренних полей ввода и AppBar
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.LogoImage
import com.ykis.ykismobkmp.ui.components.EmailField
import com.ykis.ykismobkmp.ui.components.PasswordField
import com.ykis.ykismobkmp.ui.navigation.MainApartmentScreen
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.login_details

private const val className = "SignInScreen"

@Composable
fun GoogleAuthButton(buttonTextRes: Int, isLoading: Boolean, onTokenReceived: (String) -> Unit) {
  Button(
    onClick = { onTokenReceived("mock_token") },
    enabled = !isLoading,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text("Увійти через Google")
  }
}

/**
 * [SignInScreen] — Кроссплатформенный экран авторизации абонента расчетного центра ЮКИС.
 */
object SignInScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current

    val screenModel = koinInject<SignInScreenModel>()
    val singInUiState by screenModel.AuthUiState.collectAsState()
    val googleResponse by screenModel.signInWithGoogleResponse.collectAsState()

    val isGoogleLoading = googleResponse is Resource.Loading

    // ====================================================================
    // ДИНАМИЧЕСКИЙ ЗАМЕР ОКНА: Автоматическое вычисление адаптивности KMP
    // ====================================================================
    var currentContentType by remember { mutableStateOf(ContentType.SINGLE_PANE) }
    var currentNavigationType by remember { mutableStateOf(NavigationType.BOTTOM_NAVIGATION) }

    // BoxWithConstraints нативно перехватывает ширину холста на любой ОС в реальном времени
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val windowWidth = maxWidth

      // Динамический пересчет брейкпоинтов Material 3
      LaunchedEffect(windowWidth) {
        if (windowWidth >= 840.dp) {
          currentContentType = ContentType.DUAL_PANE
          currentNavigationType = NavigationType.PERMANENT_NAVIGATION_DRAWER
          println("[YkisLogKMP.$className.Content]: Широкий екран Mac Desktop (PERMANENT_NAVIGATION_DRAWER)")
        } else if (windowWidth >= 600.dp) {
          currentContentType = ContentType.DUAL_PANE
          currentNavigationType = NavigationType.NAVIGATION_RAIL_EXPANDED
          println("[YkisLogKMP.$className.Content]: Адаптивний режим: Mac Desktop / Планшет (DUAL_PANE)")
        } else {
          currentContentType = ContentType.SINGLE_PANE
          currentNavigationType = NavigationType.BOTTOM_NAVIGATION
          println("[YkisLogKMP.$className.Content]: Адаптивний режим: Смартфон (SINGLE_PANE)")
        }
      }

      SignInScreenStateless(
        email = singInUiState.email,
        onEmailChange = screenModel::onEmailChange,
        password = singInUiState.password,
        onPasswordChange = screenModel::onPasswordChange,
        isGoogleLoading = isGoogleLoading,
        onSignInClick = {
          keyboard?.hide()
          screenModel.onSignInClick {
            println("[YkisLogKMP.$className.Content]: Успішний вхід по Email. Запуск адаптивного хабу.")

            navigator.replaceAll(
              MainApartmentScreen(
                contentType = currentContentType,
                navigationType = currentNavigationType
              )
            )
          }
        },
        onForgotPasswordClick = { screenModel.onForgotPasswordClick() },
        onSignUpClick = {
          println("[YkisLogKMP.$className.Content]: [NAVIGATION] Перехід на екран реєстрації SignUpScreen")
          // ИСПРАВЛЕНО: Нативный вызов синглтона SignUpScreen без лишних круглых скобок
          navigator.push(SignUpScreen())
        },
        onGoogleTokenReceived = { idToken ->
          println("[YkisLogKMP.$className.Content]: [EVENT] Отримано Google ID Token. Запуск авторизації...")
          screenModel.onSignUpWithGoogle(idToken) {
            println("[YkisLogKMP.$className.Content]: [NAVIGATE] Успіх Google Auth. Запуск адаптивного хабу.")

            navigator.replaceAll(
              MainApartmentScreen(
                contentType = currentContentType,
                navigationType = currentNavigationType
              )
            )
          }
        }
      )
    }
  }
}

/**
 * [SignInScreenStateless] — Декларативная верстка разметки полей ввода Material 3.
 */
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
  onGoogleTokenReceived: (String) -> Unit,
  isGoogleLoading: Boolean
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
        Spacer(modifier = Modifier.height(24.dp))

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
            text = "Забули пароль?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          modifier = Modifier.fillMaxWidth(),
          onClick = onSignInClick,
          enabled = !isGoogleLoading
        ) {
          Text(text = "Увійти", style = MaterialTheme.typography.titleMedium)
        }

        Row(
          modifier = Modifier.padding(vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
          Text(modifier = Modifier.padding(horizontal = 12.dp), text = "або", color = MaterialTheme.colorScheme.outline)
          Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }

        GoogleAuthButton(
          buttonTextRes = 0,
          isLoading = isGoogleLoading,
          onTokenReceived = onGoogleTokenReceived
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Немає аккаунту?", style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            modifier = Modifier
              .clip(MaterialTheme.shapes.small)
              .clickable { onSignUpClick() }
              .padding(4.dp),
            color = MaterialTheme.colorScheme.primary,
            text = "Реєстрація",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}
