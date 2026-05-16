package com.ykis.ykismobkmp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.next

private const val className = "SettingsText"

/**
 * [SettingsText] — Кроссплатформенный элемент изменения текстовых конфигураций профиля ЮКИС.
 * Полностью очищен от Android SDK метаданных и готов к рендерингу на Mac Desktop и iOS.
 */
@Composable
fun SettingsText(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Аннотации удалены, привязка типов переведена на KMP ресурсы JetBrains
  icon: DrawableResource,
  iconDesc: StringResource,
  name: StringResource,
  state: State<String>,
  onSave: (String) -> Unit,
  onCheck: (String) -> Boolean
) {
  var isDialogShown by remember { mutableStateOf(false) }

  // Контроль видимости модального окна редактирования БТИ-параметров
  if (isDialogShown) {
    TextEditDialog(
      name = name,
      storedValue = state,
      onSave = onSave,
      onCheck = onCheck,
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
        // ИСПРАВЛЕНО: painterResource адаптирован под кроссплатформенный тип DrawableResource
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
          // ИСПРАВЛЕНО: Текстовый заголовок настройки вычитывается через КМР stringResource
          Text(
            text = stringResource(name),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          // Текущее зафиксированное текстовое значение параметра
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
 * [TextEditDialog] — Кроссплатформенное модальное окно изменения строковых ЖКХ конфигураций.
 * ИСПРАВЛЕНО: Переведено на системный AlertDialog Material 3 Compose Multiplatform.
 */
@Composable
private fun TextEditDialog(
  name: StringResource,
  storedValue: State<String>,
  onSave: (String) -> Unit,
  onCheck: (String) -> Boolean,
  onDismiss: () -> Unit
) {
  var currentInput by remember { mutableStateOf(TextFieldValue(storedValue.value)) }
  var isValid by remember { mutableStateOf(onCheck(storedValue.value)) }

  // ИСПРАВЛЕНО: Низкоуровневый Dialog заменен на AlertDialog для адаптивной верстки окон на Mac/iOS
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
          onValueChange = { newValue ->
            isValid = onCheck(newValue.text)
            currentInput = newValue
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
          text = "Скасувати", // Внедрен чистый КМР-литерал отмены действия
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
        enabled = isValid, // КМР-валидатор (onCheck) управляет доступностью сохранения
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(
          text = stringResource(Res.string.next), // ИСПРАВЛЕНО: Ссылка на КМР-строку "Далі" через Res
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  )
}
