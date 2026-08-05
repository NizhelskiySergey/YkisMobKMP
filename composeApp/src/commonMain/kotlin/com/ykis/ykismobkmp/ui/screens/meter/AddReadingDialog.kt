package com.ykis.ykismobkmp.ui.screens.meter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.add
import com.ykis.ykismobkmp.*
import com.ykis.ykismobkmp.add_reading_title
import com.ykis.ykismobkmp.cancel
import com.ykis.ykismobkmp.current_reading
import com.ykis.ykismobkmp.new_reading

// Временная КМР-заглушка числового поля ввода, подставь свой импорт, если оно лежит в другом пакете
@Composable
fun NumberField(
  modifier: Modifier = Modifier,
  value: String,
  onNewValue: (String) -> Unit,
  label: String,
  isInteger: Boolean,
  isError: Boolean = false,
  errorMessage: String? = null
) {
  Column(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = value,
      onValueChange = { input ->
        // ГАРАНТИЯ КМР: Фильтруем ввод на лету
        val filtered = if (isInteger) {
          input.filter { it.isDigit() }
        } else {
          input.replace(',', '.').filter { it.isDigit() || it == '.' }.let { s ->
            // Разрешаем только одну точку
            if (s.count { it == '.' } <= 1) s else s.substringBeforeLast(".")
          }
        }
        onNewValue(filtered)
      },
      label = { Text(label) },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(
        keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal
      ),
      singleLine = true,
      isError = isError,
      supportingText = if (isError && !errorMessage.isNullOrBlank()) {
        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
      } else null,
      shape = MaterialTheme.shapes.medium
    )
  }
}

private const val tag = "AddReadingDialog"

/**
 * [AddReadingDialog] — Кроссплатформенное модальное окно съема и валидации показаний ЮКИС.
 * ИСПРАВЛЕНО: Ресурс ярлыка в NumberField обернут в stringResource() для устранения Type mismatch.
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
  isInteger: Boolean,
  isError: Boolean = false,
  errorMessage: String? = null
) {
  // Вывод логов по правилу [Класс.Метод] через КМР-команду println()
  println("[$tag.Content]: Діалогове вікно введення відкрито. Поточний якір: $currentReading")

  AlertDialog(
    modifier = modifier.widthIn(max = 400.dp),
    onDismissRequest = {
      println("[$tag.onDismissRequest]: Закриття діалогу користувачем")
      onDismissRequest()
    },
    title = {
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

        // Поле текущего показания (Только чтение - неизменяемый якорь валидации биллинга г. Южного)
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          label = {
            Text(text = stringResource(Res.string.current_reading))
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

        // Числовое поле ввода новых кубометров или гигакалорий
        NumberField(
          value = newReading,
          onNewValue = { input ->
            println("[$tag.onReadingChange]: Введення символу показання -> $input")
            onReadingChange(input)
          },
          label = stringResource(Res.string.new_reading),
          isInteger = isInteger,
          isError = isError,
          errorMessage = errorMessage
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
          println("[$tag.onAddClick]: Підтверджено надсилання показання: $newReading")
          onAddClick()
        },
        enabled = enabledButton, // Сквозной якорь валидации (например, 800.0 < 845.2) отрабатывает здесь
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


