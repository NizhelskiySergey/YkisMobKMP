package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.di.WEB_GOOGLE_CLIENT_ID
import kotlinx.browser.window
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_google_logo

@Composable
actual fun GoogleAuthButton(
  buttonTextRes: Int,
  isLoading: Boolean,
  onTokenReceived: (String) -> Unit
) {
  // Регистрируем коллбек в объекте window, чтобы JS мог его вызвать
  LaunchedEffect(Unit) {
    (window.asDynamic()).onGoogleTokenReceived = { credential: String ->
      println("[GoogleAuthButton.js]: ОТРИМАНО ТОКЕН ВІД GOOGLE (Credential length: ${credential.length})")
      onTokenReceived(credential)
    }
  }

  OutlinedButton(
    onClick = {
      println("[GoogleAuthButton.js]: Запуск Google Auth (GIS)...")
      try {
        (window.asDynamic()).triggerGoogleAuth(WEB_GOOGLE_CLIENT_ID)
      } catch (e: Exception) {
        println("[GoogleAuthButton.js_ERROR]: Не вдалося викликати JS міст: ${e.message}")
      }
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = !isLoading,
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Image(
        painter = painterResource(Res.drawable.ic_google_logo),
        contentDescription = "Google logo",
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = "Увійти через Google",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}
