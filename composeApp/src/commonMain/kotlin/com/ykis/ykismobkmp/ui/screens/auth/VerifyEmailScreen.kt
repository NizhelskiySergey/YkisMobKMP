package com.ykis.ykismobkmp.ui.screens.auth

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ СТРОК JETBRAINS

// Импорты строковых ресурсов из сгенерированного плагина JetBrains
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.navigation.MainApartmentScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.alredy_user
import ykismobkmp.composeapp.generated.resources.email_sent_to
import ykismobkmp.composeapp.generated.resources.send_again
import ykismobkmp.composeapp.generated.resources.repeat_email_not_verified_message
import ykismobkmp.composeapp.generated.resources.terms_condition_down
import ykismobkmp.composeapp.generated.resources.verify_email
import ykismobkmp.composeapp.generated.resources.verify_email_title

private const val className = "VerifyEmailScreen"

/**
 * [VerifyEmailScreen] — Кроссплатформенный экран подтверждения учетной записи через Email.
 */
object VerifyEmailScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Реактивно считываем глобально вычисленные на старте параметры адаптивного окна
    val adaptiveContentType = LocalContentType.current
    val adaptiveNavigationType = LocalNavigationType.current

    // Внедряем нашу кроссплатформенную ScreenModel YkisMobKMP
    val viewModel = koinInject<AuthScreenModel>()
    val reloadUserResponse by viewModel.reloadUserResponse.collectAsState()

    // АВТОМАТИЗАЦИЯ: Проверяем статус каждые 5 секунд
    LaunchedEffect(Unit) {
      println("[YkisLogKMP.$className.Content]: [AUTO_CHECK] Запуск фонового мониторинга подтверждения...")
      while (true) {
        delay(5000)
        
        // ИСПРАВЛЕНО: Безопасный вызов reloadUser. 
        // Если почта НЕ подтверждена, viewModel сама покажет Snackbar, но МЫ НЕ перейдем дальше.
        viewModel.reloadUser {
          // Этот блок выполнится ТОЛЬКО если verified == true
          println("[YkisLogKMP.$className.Content]: [SUCCESS] Пошта підтверджена автоматически.")
          navigator.replaceAll(
            MainApartmentScreen(
              contentType = adaptiveContentType,
              navigationType = adaptiveNavigationType
            )
          )
        }
      }
    }

    VerifyEmailScreenStateless(
      modifier = Modifier,
      onRepeatEmailClick = { viewModel.repeatEmailVerified() },
      onReloadClick = {
        viewModel.reloadUser {
          println("[YkisLogKMP.$className.Content]: [SUCCESS] Пошта підтверджена. Запуск головного хабу.")

          // Передаем динамические параметры окна для бесшовного старта Mac/Android
          navigator.replaceAll(
            MainApartmentScreen(
              contentType = adaptiveContentType,
              navigationType = adaptiveNavigationType
            )
          )
        }
      },
      email = viewModel.displayEmail,
      navigateBack = {
        navigator.pop() // Нативный КМР возврат назад во вход
      },
      isLoading = reloadUserResponse is Resource.Loading
    )
  }
}

/**
 * [VerifyEmailScreenStateless] — Декларативная верстка разметки полей верификации Material 3.
 */
@Composable
fun VerifyEmailScreenStateless(
  modifier: Modifier = Modifier,
  onReloadClick: () -> Unit,
  onRepeatEmailClick: () -> Unit,
  email: String,
  navigateBack: () -> Unit,
  isLoading: Boolean
) {
  Scaffold(
    topBar = {
      DefaultAppBar(
        title = stringResource(Res.string.verify_email_title),
        canNavigateBack = true,
        onBackClick = {
          println("[YkisLogKMP.$className.VerifyEmailScreenStateless]: [BACK_CLICK] Поверенння на реєстрацію")
          navigateBack()
        }
      )
    },
    bottomBar = {
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(16.dp),
        onClick = {
          println("[YkisLogKMP.$className.VerifyEmailScreenStateless]: [CHECK_CLICK] Перевірка статусу для $email")
          onReloadClick()
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp)
      ) {
        AnimatedContent(targetState = isLoading, label = "check_loading") { loading ->
          if (loading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
          } else {
            Text(text = stringResource(Res.string.alredy_user), style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    }
  ) { padding ->
    Column(
      modifier = modifier
        .padding(padding)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(32.dp))
      Icon(
        imageVector = Icons.Default.MarkEmailRead,
        contentDescription = null,
        modifier = Modifier.size(100.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = stringResource(Res.string.email_sent_to),
        style = MaterialTheme.typography.bodyLarge
      )
      Text(
        text = email,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(Res.string.verify_email),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(32.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = !isLoading) {
          println("[YkisLogKMP.$className.VerifyEmailScreenStateless]: [RESEND_CLICK] Повторне надсилання листа")
          onRepeatEmailClick()
        }
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(Res.string.send_again),
          style = MaterialTheme.typography.labelLarge,
          textDecoration = TextDecoration.Underline,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }
  }
}
