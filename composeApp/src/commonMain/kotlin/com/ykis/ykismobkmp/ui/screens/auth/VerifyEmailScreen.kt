package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.navigation.MainApartmentScreen
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ СТРОК JETBRAINS
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "VerifyEmailScreen"

/**
 * [VerifyEmailScreen] — Кроссплатформенный экран подтверждения учетной записи через Email.
 * ИСПРАВЛЕНО: Полностью удален хардкод геометрии при редиректе, типы вычитываются динамически из контекста.
 */
class VerifyEmailScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Реактивно считываем глобально вычисленные на старте параметры адаптивного окна
    val adaptiveContentType = LocalContentType.current
    val adaptiveNavigationType = LocalNavigationType.current

    // Внедряем нашу кроссплатформенную ScreenModel YkisMobKMP
    val viewModel = koinInject<SignUpScreenModel>()
    val reloadUserResponse by viewModel.reloadUserResponse.collectAsState()

    // Логирование согласно правилу [Класс.Метод]
    LaunchedEffect(Unit) {
      println("[$tag.Content]: [ENTER_SCREEN] Очікування підтвердження пошти для ${viewModel.email}")
    }

    VerifyEmailScreenStateless(
      modifier = Modifier,
      onRepeatEmailClick = { viewModel.repeatEmailVerified() },
      onReloadClick = {
        viewModel.reloadUser {
          println("[$tag.Content]: [SUCCESS] Пошта підтверджена. Запуск головного хабу.")

          // ИСПРАВЛЕНО: Никакого хардкода! Передаем динамические параметры окна для бесшовного старта Mac/Android
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
          println("[$tag.VerifyEmailScreenStateless]: [BACK_CLICK] Поверенння на реєстрацію")
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
          println("[$tag.VerifyEmailScreenStateless]: [CHECK_CLICK] Перевірка статусу для $email")
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
            Text(text = "Я підтвердив пошту", style = MaterialTheme.typography.titleMedium)
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
        text = "Лист надіслано на:",
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
          println("[$tag.VerifyEmailScreenStateless]: [RESEND_CLICK] Повторне надсилання листа")
          onRepeatEmailClick()
        }
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Надіслати лист ще раз",
          style = MaterialTheme.typography.labelLarge,
          textDecoration = TextDecoration.Underline,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }
  }
}
