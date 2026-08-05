package com.ykis.ykismobkmp.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import org.jetbrains.compose.resources.painterResource
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.ic_google_logo

@Composable
actual fun GoogleAuthButton(
  buttonTextRes: Int,
  isLoading: Boolean,
  onTokenReceived: (String) -> Unit
) {
  OutlinedButton(
    onClick = {
      // На десктопе администраторы входят по паре Email/Пароль
      println("[GoogleAuthButton.jvm]: Клик на Mac Desktop. Выводим подсказку.")
      SnackbarManager.showMessage("Вхід через Google доступний тільки на Android. Будь ласка, використовуйте Email та пароль.")
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = !isLoading,
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // ИСПРАВЛЕНО: Логотип Google теперь рендерится на Mac Desktop из общих ресурсов!
      Image(
        painter = painterResource(Res.drawable.ic_google_logo),
        contentDescription = "Google logo",
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = "Вхід через Google (Тільки для смартфона)",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}
