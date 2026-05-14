package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.delete_reading_title

private const val className = "DeleteReadingDialog"

/**
 * [DeleteReadingDialog] — Кроссплатформенное модальное окно подтверждения удаления показания ЮКИС.
 * Полностью стабильно на Mac Desktop (JVM), Android и iOS без привязок к Android SDK.
 */
@Composable
fun DeleteReadingDialog(
  modifier: Modifier = Modifier,
  onDismissRequest: () -> Unit,
  onDeleteClick: () -> Unit
) {
  // ИСПРАВЛЕНО: Переведено на стандартный AlertDialog для идеального отображения на Mac/Android/iOS
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    icon = {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error, // Красный цвет ошибки для деструктивного действия
        modifier = Modifier.size(32.dp)
      )
    },
    title = {

      Text(
        text = stringResource(Res.string.delete_reading_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Text(
        text = "Ви дійсно бажаєте видалити останній запис приладу обліку? Цю дію не можна буде скасувати.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(
          text = stringResource(Res.string.cancel),
          style = MaterialTheme.typography.labelLarge
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onDismissRequest() // Сначала закрываем окно
          onDeleteClick()    // Запускаем сетевое удаление из биллинга Южного
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError
        )
      ) {
        Text(
          text = stringResource( Res.string.cancel),
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  )
}

