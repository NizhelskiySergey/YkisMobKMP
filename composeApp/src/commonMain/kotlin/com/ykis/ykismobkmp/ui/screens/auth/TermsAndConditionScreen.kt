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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.FirebaseService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val tag = "TermsAndConditionScreen"

/**
 * [TermsAndConditionScreen] — Стартовый экран лицензионного соглашения ЮКИС.
 * Общий интерфейс для жителей города Южный и администраторов ЖКХ на Mac/Android.
 */
class TermsAndConditionScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val firebaseService = koinInject<FirebaseService>()

    // РЕШЕНИЕ: Создаем scope корутин для обработки асинхронного клика
    val coroutineScope = rememberCoroutineScope()

    // Получаем текст соглашения из кроссплатформенного Remote Config GitLive SDK
    val termsText = firebaseService.agreementText

    TermsAndConditionContent(
      termsText = termsText,
      onAccept = {
        // Запускаем корутину при клике по кнопке
        coroutineScope.launch {
          println("[$tag.onAccept]: Співпраця та згода підтверджені користувачем")

          // Теперь suspend функция вызывается безопасно внутри корутины
          firebaseService.setUserAgreed(true)

          // После успешной записи переходим на экран входа
          navigator.replaceAll(SignInScreen())
        }
      }
    )
  }

}

/**
 * [TermsAndConditionContent] — Чистая верстка экрана, изолированная от навигации, доступная для Preview.
 */
@Composable
fun TermsAndConditionContent(
  termsText: String,
  onAccept: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Заголовок окна (Используем чистые строки для кроссплатформенности на Mac Desktop)
    Text(
      text = "Умови користування ІС \"ЮКІС\"",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary
    )

    Spacer(Modifier.height(16.dp))

    // Текст лицензии с плавной прокруткой для тачпадов Mac и сенсорных экранов Android
    Box(modifier = Modifier.weight(1f)) {
      Text(
        text = termsText.ifBlank { "Завантаження умов конфіденційності з серверів розрахункового центру..." },
        modifier = Modifier.verticalScroll(rememberScrollState()),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
      )
    }

    Spacer(Modifier.height(24.dp))

    // Кнопка подтверждения и фиксации согласия GDPR
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      onClick = {
        println("[$tag.Button]: Клік ПРИЙНЯТИ")
        onAccept()
      },
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = "Я приймаю умови угоди",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}


