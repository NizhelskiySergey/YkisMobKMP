package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
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
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.platformActivityContext
import com.ykis.ykismobkmp.core.utils.triggerNativeGoogleSignIn
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import org.koin.compose.koinInject

// Импорты твоих внутренних полей ввода и AppBar
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.LogoImage
import com.ykis.ykismobkmp.ui.components.EmailField
import com.ykis.ykismobkmp.ui.components.PasswordField
import com.ykis.ykismobkmp.ui.components.PhoneVisualTransformation
import com.ykis.ykismobkmp.ui.navigation.MainApartmentScreen
import com.ykis.ykismobkmp.ui.navigation.SignUpScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import dev.gitlive.firebase.auth.auth
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.login_details
import ykismobkmp.composeapp.generated.resources.repeat_email_not_verified_message
import ykismobkmp.composeapp.generated.resources.sign_in_with_google

private const val className = "SignInScreen"

@Composable
fun GoogleAuthButton(buttonTextRes: Int, isLoading: Boolean, onTokenReceived: (String) -> Unit) {
  // Извлекаем контекст через твою отлаженную Activity-функцию
  val contextActivity = platformActivityContext()

  Button(
    onClick = {
      if (contextActivity == null) {
        println("[YkisLogKMP.GoogleAuthButton]: [ERROR] Контекст Activity відсутній у рантаймі КМР")
        return@Button
      }

      println("[YkisLogKMP.GoogleAuthButton]: [START] Запуск системного вікна вибору Google-аккаунтів через Play Services")

      triggerNativeGoogleSignIn(
        activityContext = contextActivity,
        onTokenReceived = { realIdToken ->
          println("[YkisLogKMP.GoogleAuthButton]: [SUCCESS] Отримано оригінальний зашифрований токен від Google Play Services")
          onTokenReceived(realIdToken)
        },
        onError = { errorMsg ->
          println("[YkisLogKMP.GoogleAuthButton]: [ERROR] Нативний збій: $errorMsg")
          SnackbarManager.showMessage(errorMsg)
        }
      )
    },
    enabled = !isLoading,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text("Увійти через Google")
  }
}


/**
 * [SignInScreen] — Кросплатформенний екран авторизації абонента розрахункового центру ЮКІС.
 */
object SignInScreen : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current
    val screenModel = koinInject<AuthScreenModel>()
    val singInUiState by screenModel.authUiState.collectAsState()
    val googleResponse by screenModel.signInWithGoogleResponse.collectAsState()
    val signInResponse by screenModel.signInResponse.collectAsState()
    val smsSendResponse by screenModel.smsSendResponse.collectAsState()
    val contextActivity = platformActivityContext()
    val isSmsLoading = signInResponse is Resource.Loading || smsSendResponse is Resource.Loading
    val isGoogleLoading by screenModel.isGoogleLoading.collectAsState()

    var currentContentType by remember { mutableStateOf(ContentType.SINGLE_PANE) }
    var currentNavigationType by remember { mutableStateOf(NavigationType.BOTTOM_NAVIGATION) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val windowWidth = maxWidth
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
        phoneNumber = singInUiState.phoneNumber,
        onPhoneChange = screenModel::onPhoneChange,
        smsCode = singInUiState.smsCode,
        onSmsCodeChange = screenModel::onSmsCodeChange,
        isSmsSent = singInUiState.isSmsSent,
        isSmsLoading = isSmsLoading,
        onSendSmsClick = {
          keyboard?.hide()
          screenModel.triggerSmsCode(contextActivity) {
            println("[YkisLogKMP.$className.Content]: [SMS_SENT] Запит коду успішно передано оператору")
          }
        },
        onVerifySmsClick = {
          keyboard?.hide()
          screenModel.verifySmsAndSignIn {
            println("[YkisLogKMP.$className.Content]: [SUCCESS] Вхід за SMS успішний. Передано під контроль реактивного ядра.")
          }
        },
        onResetSmsState = { screenModel.setSmsSentState(false) },
        isGoogleLoading = isGoogleLoading,
        onSignInClick = {
          keyboard?.hide()
          screenModel.onSignInClick {
            val activeUser = dev.gitlive.firebase.Firebase.auth.currentUser
            val isVerified = activeUser?.isEmailVerified ?: false
            if (!isVerified) {
              println("[YkisLogKMP.$className.Content]: Вхід успішний, але пошта НЕ підтверджена. Навігація на VerifyEmailScreen.")
              navigator.push(VerifyEmailScreen)
            } else {
              println("[YkisLogKMP.$className.Content]: [SUCCESS] Вхід за Email успішний. Передано під контроль реактивного ядра.")
            }
          }
        },
        onForgotPasswordClick = { screenModel.onForgotPasswordClick() },
        onSignUpClick = {
          println("[YkisLogKMP.$className.Content]: [NAVIGATION] Перехід на екран реєстрації SignUpScreen")
          navigator.push(SignUpScreen)
        },
        onGoogleTokenReceived = { idToken ->
          println("[YkisLogKMP.$className.Content]: [EVENT] Отримано Google ID Token. Запуск авторизації...")
          screenModel.onSignUpWithGoogle(idToken) {
            // ИСПРАВЛЕНО НАМЕРТВО: Лишние дублирующие вызовы кэша БТИ полностью вырезаны
            println("[YkisLogKMP.$className.Content]: [SUCCESS] Успіх Google Auth. Передано під контроль реактивного ядра.")
          }
        }
      )
    }
  }
}

@Composable
fun SignInScreenStateless(
  modifier: Modifier = Modifier,
  email: String,
  onEmailChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  phoneNumber: String,
  onPhoneChange: (String) -> Unit,
  smsCode: String,
  onSmsCodeChange: (String) -> Unit,
  isSmsSent: Boolean,
  isSmsLoading: Boolean,
  onSendSmsClick: () -> Unit,
  onVerifySmsClick: () -> Unit,
  onResetSmsState: () -> Unit,
  onSignInClick: () -> Unit,
  onForgotPasswordClick: () -> Unit,
  onSignUpClick: () -> Unit,
  onGoogleTokenReceived: (String) -> Unit,
  isGoogleLoading: Boolean
) {
  var selectedTab by remember { mutableStateOf(0) }
  val globalLoading = isGoogleLoading || isSmsLoading
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

        PrimaryTabRow(
          selectedTabIndex = selectedTab,
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          indicator = {
            TabRowDefaults.PrimaryIndicator(
              modifier = Modifier.tabIndicatorOffset(selectedTab),
              width = 64.dp,
              shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
          }
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = {
              selectedTab = 0
              onResetSmsState() // Скидаємо кроки SMS при переході на Email вкладку
            },
            text = { Text("Ел. пошта", fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Телефон", fontWeight = FontWeight.SemiBold) }
          )
        }
        if (selectedTab == 0) {
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
        } else {
          OutlinedTextField(
            value = phoneNumber,
            onValueChange = { input ->
              val cleanDigits = input.filter { it.isDigit() }
              if (cleanDigits.length <= 9) {
                onPhoneChange(cleanDigits)
              }
            },
            label = { Text("Номер телефону") },
            placeholder = { Text("93 846 81 41") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSmsSent && !globalLoading,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PhoneVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
              keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
          )
          if (isSmsSent) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = smsCode,
              onValueChange = { if (it.length <= 6) onSmsCodeChange(it) },
              label = { Text("Код із SMS") },
              placeholder = { Text("6 знаків") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              enabled = !globalLoading,
              shape = RoundedCornerShape(12.dp),
              keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
              )
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          modifier = Modifier.fillMaxWidth(),
          enabled = !globalLoading,
          onClick = {
            if (selectedTab == 0) {
              onSignInClick()
            } else {
              if (!isSmsSent) onSendSmsClick() else onVerifySmsClick()
            }
          }
        ) {
          val mainButtonText = when {
            selectedTab == 0 -> "Увійти"
            !isSmsSent -> "Надіслати код"
            else -> "Підтвердити код"
          }
          Text(text = mainButtonText, style = MaterialTheme.typography.titleMedium)
        }
        if (selectedTab == 0) {
          Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier.weight(1f).height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Text(
              modifier = Modifier.padding(horizontal = 12.dp),
              text = "або",
              color = MaterialTheme.colorScheme.outline
            )
            Box(
              modifier = Modifier.weight(1f).height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
            )
          }
          GoogleAuthButton(
            buttonTextRes = 0,
            isLoading = globalLoading,
            onTokenReceived = onGoogleTokenReceived
          )
        }
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


