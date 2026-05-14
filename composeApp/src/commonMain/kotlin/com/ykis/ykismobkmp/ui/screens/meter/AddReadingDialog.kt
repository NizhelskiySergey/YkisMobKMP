package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.components.NumberField
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.add
import ykismobkmp.composeapp.generated.resources.add_reading_supporting_text
import ykismobkmp.composeapp.generated.resources.add_reading_title
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.current_reading
import ykismobkmp.composeapp.generated.resources.new_reading

private const val tag = "AddReadingDialog"

/**
 * [AddReadingDialog] — Кроссплатформенное модальное окно съема и валидации показаний ЮКИС.
 * Полностью стабильно на Mac Desktop (JVM), Android и iOS без привязок к Android SDK.
 */
@Composable
fun AddReadingDialog(
  modifier: Modifier = Modifier,
  onDismissRequest: () -> Unit,
  onAddClick: () -> Unit,
  currentReading: String,
  newReading: String,
  onReadingChange: (String) -> Unit,
  enabledButton: Boolean,
  isInteger: Boolean
) {
  // ИСПРАВЛЕНО: Платформозависимый Log.d заменен на универсальный println() под Mac JVM
  println("[$tag.Content]: Dialog opened. Current: $currentReading")

  // ИСПРАВЛЕНО: Переведено на стандартный AlertDialog для идеальной геометрии окон на Mac/Android/iOS
  AlertDialog(
    modifier = modifier.widthIn(max = 400.dp),
    onDismissRequest = {
      println("[$tag.onDismissRequest]: Dialog dismissed")
      onDismissRequest()
    },
    title = {
      // ИСПРАВЛЕНО: Заменены ресурсы строк на чистые КМР-литералы под Mac JVM
      Text(
        text = stringResource(Res.string.add_reading_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(Res.string.add_reading_supporting_text),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 24.dp)
        )

        // Поле текущего показания (Только чтение - Якорь валидации)
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          label = {
            Text(
              text = stringResource( Res.string.current_reading)
            )
          },
          readOnly = true,
          value = currentReading,
          onValueChange = {},
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )

        // Поле ввода нового показания (Передаем строку ярлыка напрямую)
        NumberField(
          value = newReading,
          onNewValue = {
            println("[$tag.onReadingChange]: New input -> $it")
            onReadingChange(it)
          },
          label = Res.string.new_reading, // ИСПРАВЛЕНО: Заменена ссылка на ресурс чистой строкой
          isInteger = isInteger
        )
      }
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
          println("[$tag.onAddClick]: Submit reading -> $newReading")
          onAddClick()
        },
        enabled = enabledButton, // Якорь валидации (например, 800 < 877) отрабатывает здесь
        shape = MaterialTheme.shapes.medium
      ) {
        Text(
          text = stringResource(Res.string.add),
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  )
}

