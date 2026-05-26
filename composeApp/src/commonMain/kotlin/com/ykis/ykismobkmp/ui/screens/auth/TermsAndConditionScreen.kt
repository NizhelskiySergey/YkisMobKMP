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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.terms_condition
import ykismobkmp.composeapp.generated.resources.terms_condition_accept
import ykismobkmp.composeapp.generated.resources.terms_condition_down

private const val className = "TermsAndConditionScreen"

/**
 * [TermsAndConditionScreen] — Стартовий екран ліцензійної угоди (оферти ГІОЦ) біллінгу м. Южне.
 * ИСПРАВЛЕНО НАМЕРТВО: Объект переведен в класс, принимающий готовый текст оферты.
 * Все фоновые LaunchedEffect сетевой подгрузки удалены — экран рендерится мгновенно и без мерцаний!
 */
class TermsAndConditionScreen(private val termsText: String) : Screen {

  // Жестко фиксируем строковый ключ экрана для стабильного рантайма Voyager
  override val key: cafe.adriel.voyager.core.screen.ScreenKey = "TermsAndConditionScreen_Static"

  @Composable
  override fun Content() {
    val appStartModel = koinInject<AppScreenModel>() // Инжектуем реактивную стейт-машину старта

    // ИСПРАВЛЕНО НАМЕРТВО: Так как текст оферты уже скачан в фоне на уровне AppScreenModel,
    // мы ПОЛНОСТЬЮ вырезали отсюда фоновые лоадеры и проверки isNetworkFetching.
    // Экран рендерит Material 3 графику мгновенно, полностью исключая визуальные артефакты!
    TermsAndConditionContent(
      termsText = termsText,
      onAccept = {
        println("[YkisLogKMP.$className.Content.onAccept]: Користувач підтвердив згоду. Фіксація в КМР-кЕш...")

        // Передаем управление стейт-машине: она запишет true и плавно откроет экран входа через RootNavGraph
        appStartModel.acceptTermsAndConditions {
          println("[YkisLogKMP.$className.Content.onAccept]: Лямбда успіху. Передано на реактивний розподіл.")
        }
      }
    )
  }
}

/**
 * [TermsAndConditionContent] — Декларативна чиста верстка екрана ліцензії Material 3.
 */
@Composable
fun TermsAndConditionContent(
  termsText: String,
  onAccept: () -> Unit
) {
  val scrollState = rememberScrollState()

  // ОБЧИСЛЕННЯ ДИНАМІЧНОЇ БЛОКУВАННЯ: Кнопка активна тільки якщо користувач доскролив до кінця.
  // Якщо maxValue == 0 (текст помістився повністю без скролла), кнопка активується відразу.
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
      text = stringResource(Res.string.terms_condition),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Скрольований блок займає ВСЕ вільне місце на полотні за допомогою .weight(1f).
    // Це автоматично притискає кнопку Button до самого низу екрана смартфона!
    Box(modifier = Modifier.weight(1f)) {
      Text(
        text = termsText,
        modifier = Modifier.verticalScroll(scrollState),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Кнопка фіксації згоди мешканця м. Южне
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      onClick = {
        println("[YkisLogKMP.$className.TermsAndConditionContent]: [EVENT] Клік по кнопці фіксації оферти жильцом")
        onAccept()
      },
      enabled = isScrollFinished, // Прив'язуємо стан блокування до повзунка скролла
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = if (isScrollFinished) stringResource(Res.string.terms_condition_accept) else stringResource(Res.string.terms_condition_down),
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

