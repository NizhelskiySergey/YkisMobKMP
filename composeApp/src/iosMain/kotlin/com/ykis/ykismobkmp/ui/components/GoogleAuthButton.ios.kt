package com.ykis.ykismobkmp.ui.components

import com.ykis.ykismobkmp.core.utils.SnackbarManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.ic_google_logo


@Composable
fun AppleAuthButton(
  isLoading: Boolean,
  onStart: () -> Unit,
  onTokenReceived: (String) -> Unit
) {
  Button(
    onClick = {
      onStart()
      println("[AppleAuthButton.ios]: Клик на Sign in with Apple.")
      // Здесь будет логика вызова нативного окна Apple Auth
      SnackbarManager.showMessage("Авторизація Apple в розробці...")
    },
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    enabled = !isLoading,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.onSurface,
      contentColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Можно использовать системную иконку или ресурс
      Text(
        text = "Увійти через Apple",
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

@Composable
actual fun GoogleAuthButton(
  buttonTextRes: Int,
  isLoading: Boolean,
  onTokenReceived: (String) -> Unit
) {
  OutlinedButton(
    onClick = {
      println("[GoogleAuthButton.ios]: Клик на iOS.")
      SnackbarManager.showMessage("Авторизація через Google для iOS знаходиться в розробці.")
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
