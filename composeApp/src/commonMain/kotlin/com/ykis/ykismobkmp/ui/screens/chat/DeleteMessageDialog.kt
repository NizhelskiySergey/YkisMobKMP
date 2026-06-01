package com.ykis.ykismobkmp.ui.screens.chat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
/**
 * [DeleteMessageDialog] — Мультиплатформенный диалог подтверждения деструктивного удаления сообщений из хмари.
 * ПОЯСНЕНИЕ: Использует красный колір темы для кнопки "Видалити" и мягкий тональный подъем контейнера.
 */
@Composable
fun DeleteMessageDialog(
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    // Поверхность с небольшим подъемом, чтобы выделяться на фоне чата Единого Хаба ЮКІС
    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error // Красный акцент для опасного действия удаления из Firebase
      )
    },
    title = {
      Text(
        text = "Видалити повідомлення?",
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Text(
        text = "Повідомлення буде видалено безповоротно!",
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        // Текст кнопки удаления тоже делаем красным под каноны деструктивных транзакций
        colors = ButtonDefaults.textButtonColors(
          contentColor = MaterialTheme.colorScheme.error
        )
      ) {
        Text("Видалити", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Відмінити")
      }
    }
  )
}

