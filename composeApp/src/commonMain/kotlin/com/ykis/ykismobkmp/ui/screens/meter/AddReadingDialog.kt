package com.ykis.ykismobkmp.ui.screens.meter.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ykis.ykismobkmp.ui.components.NumberField
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res
import android.util.Log

private const val className = "AddReadingDialog"

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
  // Логируем открытие диалога согласно правилу [Класс.Метод]
  Log.d("YkisLog", "[$className.Content]: Dialog opened. Current: $currentReading")

  Dialog(
    onDismissRequest = {
      Log.d("YkisLog", "[$className.onDismissRequest]: Dialog dismissed")
      onDismissRequest()
    },
  ) {
    Card(
      shape = MaterialTheme.shapes.extraLarge,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .widthIn(max = 400.dp) // Ограничение ширины для Desktop (Mac)
      ) {
        Text(
          text = stringResource(Res.string.add_reading_title),
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(bottom = 16.dp),
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          modifier = Modifier.padding(bottom = 24.dp),
          style = MaterialTheme.typography.bodyMedium,
          text = stringResource(Res.string.add_reading_supporting_text),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Поле текущего показания (Только чтение - Якорь)
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          label = { Text(text = stringResource(Res.string.current_reading)) },
          readOnly = true,
          value = currentReading,
          onValueChange = {},
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          )
        )

        // Поле ввода нового показания (Наш компонент из TextFields.kt)
        NumberField(
          value = newReading,
          onNewValue = {
            Log.d("YkisLog", "[$className.onReadingChange]: New input -> $it")
            onReadingChange(it)
          },
          label = Res.string.new_reading,
          isInteger = isInteger
        )

        // Блок кнопок
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onDismissRequest
          ) {
            Text(text = stringResource(Res.string.cancel))
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = {
              Log.i("YkisLog", "[$className.onAddClick]: Submit reading -> $newReading")
              onAddClick()
            },
            enabled = enabledButton, // Якорь 800 < 877 работает здесь!
            shape = MaterialTheme.shapes.medium
          ) {
            Text(stringResource(Res.string.add))
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun PreviewAddReadingDialog() {
  YkisPAMTheme {
    AddReadingDialog(
      onDismissRequest = { },
      onAddClick = { },
      currentReading = "877",
      newReading = "800",
      onReadingChange = {},
      enabledButton = false, // Кнопка неактивна, так как 800 < 877
      isInteger = true
    )
  }
}
