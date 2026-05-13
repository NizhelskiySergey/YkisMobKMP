package com.ykis.ykismobkmp.ui.screens.auth.sign_up


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.services.FirebaseService
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.agreement_check
import ykismobkmp.composeapp.generated.resources.agreement_title

/**
 * [TermsScreen] — обертка Voyager для экрана лицензионного соглашения.
 */
class TermsScreen : Screen {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val firebaseService = koinInject<FirebaseService>()

    // Получаем текст соглашения (из Remote Config или константы)
    val termsText = firebaseService.agreementText

    TermsAndConditionContent(
      termsText = termsText,
      onAccept = {
        Log.d("YkisLog", "[TermsScreen.onAccept]: Согласие получено")
        firebaseService.setAgreement(true)
        // После принятия согласия переходим на экран авторизации
        navigator.replaceAll(RootScreens.Auth)
      }
    )
  }
}

/**
 * Чистая верстка экрана, доступная для Preview и тестов.
 */
@Composable
fun TermsAndConditionContent(
  termsText: String,
  onAccept: () -> Unit
) {
  val className = "TermsAndConditionScreen"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = stringResource(Res.string.agreement_title),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary
    )

    Spacer(Modifier.height(16.dp))

    // Текст с прокруткой занимает всё свободное место
    Box(modifier = Modifier.weight(1f)) {
      Text(
        text = termsText,
        modifier = Modifier.verticalScroll(rememberScrollState()),
        style = MaterialTheme.typography.bodyMedium
      )
    }

    Spacer(Modifier.height(16.dp))

    // Кнопка подтверждения
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      onClick = {
        Log.d("YkisLog", "[$className.Button]: Клик ПРИНЯТЬ")
        onAccept()
      },
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(stringResource(Res.string.agreement_check))
    }
  }
}


