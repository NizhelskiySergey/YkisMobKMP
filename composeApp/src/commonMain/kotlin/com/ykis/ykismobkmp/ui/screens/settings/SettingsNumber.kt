package com.ykis.ykismobkmp.ui.screens.settings


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Подключаем сгенерированный КМР-пакет строк для поиска Res.string.next
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.next

private const val className = "SettingsNumber"

/**
 * [SettingsNumber] — Кроссплатформенный элемент изменения числовых параметров профиля ЮКИС.
 * Полностью очищен от Android SDK и готов к нативной компиляции под Mac Desktop и iOS.
 */
@Composable
fun SettingsNumber(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Платформенные аннотации удалены, типы переведены на KMP ресурсы JetBrains
  icon: DrawableResource,
  iconDesc: StringResource,
  name: StringResource,
  state: State<String>,
  onSave: (String) -> Unit,
  inputFilter: (String) -> String,
  onCheck: (String) -> Boolean
) {
  var isDialogShown by remember { mutableStateOf(false) }

  if (isDialogShown) {
    TextEditNumberDialog(
      name = name,
      storedValue = state,
      inputFilter = inputFilter,
      onSave = onSave,
      onCheck = onCheck,
      // ИСПРАВЛЕНО: Вызов утилиты .not() заменен лаконичным Котлин-оператором логического отрицания
      onDismiss = { isDialogShown = false }
    )
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    onClick = { isDialogShown = true },
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          painter = painterResource(icon),
          contentDescription = stringResource(iconDesc),
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
          modifier = Modifier.weight(1f).padding(vertical = 4.dp),
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = stringResource(name),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = state.value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
  }
}

/**
 * [TextEditNumberDialog] — Кроссплатформенное модальное числовое окно с принудительной фильтрацией ввода (ввод телефона/счета).
 * ИСПРАВЛЕНО: Низкоуровневый Dialog заменен на официальный AlertDialog Material 3 Compose Multiplatform.
 */
@Composable
private fun TextEditNumberDialog(
  name: StringResource,
  storedValue: State<String>,
  inputFilter: (String) -> String,
  onSave: (String) -> Unit,
  onCheck: (String) -> Boolean,
  onDismiss: () -> Unit
) {
  var currentInput by remember { mutableStateOf(TextFieldValue(storedValue.value)) }
  var isValid by remember { mutableStateOf(onCheck(storedValue.value)) }

  // ИСПРАВЛЕНО: Ручная верстка Dialog { Surface { Column } } переведена на системный AlertDialog
  AlertDialog(
    modifier = Modifier.widthIn(max = 400.dp),
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(name),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth(),
          value = currentInput,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          onValueChange = { newValue ->
            // Применяем КМР функциональный фильтр ввода и удаляем лишние буквенные символы
            val filteredText = inputFilter(newValue.text)
            isValid = onCheck(filteredText)
            currentInput = TextFieldValue(filteredText)
          },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge,
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
          )
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(
          text = "Скасувати",
          style = MaterialTheme.typography.labelLarge
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(currentInput.text)
          onDismiss()
        },
        enabled = isValid,
        shape = MaterialTheme.shapes.small
      ) {
        Text(
          text = stringResource(Res.string.next), // ИСПРАВЛЕНО: КМР-ссылка на общую строку "Далі" через Res
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  )
}
