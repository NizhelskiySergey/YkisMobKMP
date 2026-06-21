package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.ykis.ykismobkmp.core.utils.rememberSmsRetriever
import com.ykis.ykismobkmp.ui.navigation.MainApartmentScreen
import com.ykis.ykismobkmp.ui.navigation.SignUpScreenDest
import com.ykis.ykismobkmp.ui.navigation.VerifyEmailScreenDest
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import dev.gitlive.firebase.auth.auth
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.email_tab
import ykismobkmp.composeapp.generated.resources.forgot_password_link
import ykismobkmp.composeapp.generated.resources.login_details
import ykismobkmp.composeapp.generated.resources.no_account_text
import ykismobkmp.composeapp.generated.resources.or_divider
import ykismobkmp.composeapp.generated.resources.phone_number_label
import ykismobkmp.composeapp.generated.resources.phone_number_placeholder
import ykismobkmp.composeapp.generated.resources.phone_tab
import ykismobkmp.composeapp.generated.resources.repeat_email_not_verified_message
import ykismobkmp.composeapp.generated.resources.send_code_button
import ykismobkmp.composeapp.generated.resources.sign_in_button
import ykismobkmp.composeapp.generated.resources.sign_in_with_google
import ykismobkmp.composeapp.generated.resources.sign_up_link
import ykismobkmp.composeapp.generated.resources.sms_code_label
import ykismobkmp.composeapp.generated.resources.sms_code_placeholder
import ykismobkmp.composeapp.generated.resources.verify_code_button

private const val className = "SignInScreen"

@Composable
fun AppleAuthButton(isLoading: Boolean, onStart: () -> Unit, onTokenReceived: (String) -> Unit) {
    val platform = com.ykis.ykismobkmp.getPlatform().name
    println("[YkisLogKMP.AppleAuthButton]: Поточна платформа: $platform")

    // Відображаємо кнопку на всіх Apple пристроях (iPhone, iPad, Mac)
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
                onTokenReceived = { token ->
                    println("[YkisLogKMP.AppleAuthButton]: [SUCCESS] Apple Token отримано")
                    onTokenReceived(token)
                },
                onError = { error ->
                    println("[YkisLogKMP.AppleAuthButton]: [ERROR] $error")
                    if (error != "Canceled") {
                        SnackbarManager.showMessage(error)
                    }
                    onTokenReceived("") // Скидаємо лоадер
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
  val platform = com.ykis.ykismobkmp.getPlatform().name
  
  // Кнопка Google видна на Android и iOS (через WebView или натив)
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
    modifier = Modifier.fillMaxWidth()
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
    val googleResponse by screenModel.signInWithGoogleResponse.collectAsState()
    val signInResponse by screenModel.signInResponse.collectAsState()
    val smsSendResponse by screenModel.smsSendResponse.collectAsState()
    val contextActivity = platformActivityContext()
    val isSmsLoading = signInResponse is Resource.Loading || smsSendResponse is Resource.Loading
    val isGoogleLoading by screenModel.isGoogleLoading.collectAsState()
    var currentContentType by remember { mutableStateOf(ContentType.SINGLE_PANE) }
    var currentNavigationType by remember { mutableStateOf(NavigationType.BOTTOM_NAVIGATION) }

    // АВТОМАТИЗАЦИЯ SMS: Инициализируем платформенный ретривер
    val smsRetriever = rememberSmsRetriever()

    // Останавливаем прослушивание при выходе с экрана
    DisposableEffect(Unit) {
      onDispose { smsRetriever.stopRetriever() }
    }

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
            println("[YkisLogKMP.$className.Content]: [SMS_SENT] Запрос кода успешно передан оператору. Запуск ретривера...")
            
            // ЗАПУСК АВТОМАТИЗАЦИИ: Начинаем слушать входящие SMS
            smsRetriever.startRetriever { autoCode ->
              println("[YkisLogKMP.$className.Content]: [AUTO_FILL] SMS код перехвачен: $autoCode")
              screenModel.onSmsCodeChange(autoCode)
              // Опционально: можно сразу вызвать подтверждение, если код пришел полностью
              // screenModel.verifySmsAndSignIn { ... }
            }
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
        onAppleTokenReceived = { idToken ->
          println("[YkisLogKMP.$className.Content]: [EVENT] Получен Apple ID Token.")
          screenModel.onSignUpWithApple(idToken) {
            println("[YkisLogKMP.$className.Content]: [SUCCESS] Успех Apple Auth.")
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
  onAppleTokenReceived: (String) -> Unit,
  onGoogleStart: () -> Unit,
  onGoogleError: () -> Unit,
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
            text = { Text(stringResource(Res.string.email_tab), fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text(stringResource(Res.string.phone_tab), fontWeight = FontWeight.SemiBold) }
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
              text = stringResource(Res.string.forgot_password_link),
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
            label = { Text(stringResource(Res.string.phone_number_label)) },
            placeholder = { Text(stringResource(Res.string.phone_number_placeholder)) },
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
              label = { Text(stringResource(Res.string.sms_code_label)) },
              placeholder = { Text(stringResource(Res.string.sms_code_placeholder)) },
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
            selectedTab == 0 -> stringResource(Res.string.sign_in_button)
            !isSmsSent -> stringResource(Res.string.send_code_button)
            else -> stringResource(Res.string.verify_code_button)
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
              text = stringResource(Res.string.or_divider),
              color = MaterialTheme.colorScheme.outline
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
            onTokenReceived = onAppleTokenReceived
          )
        }
        Spacer(modifier = Modifier.height(24.dp))
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


