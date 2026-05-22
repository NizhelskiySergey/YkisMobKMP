package com.ykis.ykismobkmp.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.FirebaseService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val className = "TermsAndConditionScreen"

/**
 * [TermsAndConditionScreen] — Стартовый экран лицензионного соглашения ЮКИС.
 * ИСПРАВЛЕНО: Кнопка «Принять» прижата к самому низу экрана и заблокирована до полной прокрутки текста.
 */
object TermsAndConditionScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val firebaseService = koinInject<FirebaseService>()
    val coroutineScope = rememberCoroutineScope()

    // Динамическое реактивное состояние текста соглашения
    var termsText by remember { mutableStateOf(firebaseService.agreementText) }

    // Принудительно заставляем Remote Config обновиться из облака при открытии экрана
    LaunchedEffect(Unit) {
      println("[YkisLogKMP.$className.Content]: Запуск асинхронной подгрузки Remote Config...")
      val isSuccess = firebaseService.fetchConfiguration()
      println("[YkisLogKMP.$className.Content]: Результат fetchConfiguration = $isSuccess")

      termsText = firebaseService.agreementText
      println("[YkisLogKMP.$className.Content]: Получен текст из облака. Длина: ${termsText.length}")
    }

    // Если текст из облака Firebase всё ещё пуст — удерживаем безопасный лоадер
    if (termsText.isBlank()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Очікування відповіді від серверів розрахункового центру...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    } else {
      // Текст успешно получен из Firebase Remote Config — рендерим оферту
      TermsAndConditionContent(
        termsText = termsText,
        onAccept = {
          coroutineScope.launch {
            println("[YkisLogKMP.$className.Content.onAccept]: Користувач підтвердив згоду")
            firebaseService.setUserAgreed(true)
            navigator.replaceAll(SignInScreen)
          }
        }
      )
    }
  }
}

/**
 * [TermsAndConditionContent] — Декларативная верстка экрана лицензии Material 3.
 */
@Composable
fun TermsAndConditionContent(
  termsText: String,
  onAccept: () -> Unit
) {
  val scrollState = rememberScrollState()

  // ВЫЧИСЛЕНИЕ ДИНАМИЧЕСКОЙ БЛОКИРОВКИ: Кнопка активна только если пользователь доскроллил до конца.
  // Если maxValue == 0 (текст поместился целиком без скролла), кнопка активируется сразу.
  val isScrollFinished = remember(scrollState.value, scrollState.maxValue) {
    scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Умови користування ІС \"ЮКІС\"",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Скроллируемый блок занимает ВСЁ свободное место на холсте с помощью .weight(1f).
    // Это автоматически выталкивает кнопку Button на самый низ экрана!
    Box(modifier = Modifier.weight(1f)) {
      Text(
        text = termsText,
        modifier = Modifier.verticalScroll(scrollState),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Кнопка фиксации согласия жильца.
    // Параметр 'enabled' нативно управляет визуальным состоянием (серая/активная)
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      onClick = {
        println("[YkisLogKMP.$className.TermsAndConditionContent]: Клик по кнопке фиксации оферты")
        onAccept()
      },
      enabled = isScrollFinished, // Привязываем состояние блокировки к скроллу
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = if (isScrollFinished) "Я приймаю умови угоди" else "Прокрутіть текст до кінця ↓",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}
