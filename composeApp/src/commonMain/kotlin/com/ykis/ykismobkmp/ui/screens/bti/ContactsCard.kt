package com.ykis.ykismobkmp.ui.screens.bti

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.LabelTextWithTextAndIcon

// ИМПОРТЫ КМР РЕСУРСОВ JETBRAINS:
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ContactsCard"

/**
 * [ContactsCard] — Кроссплатформенная карточка контактных данных абонента БТИ г. Южный.
 */
@Composable
fun ContactsCard(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState = BaseUIState(),
  onUpdateBti: () -> Unit,
  phone: String,
  email: String,
  onEmailChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  val openDialog = remember { mutableStateOf(false) }

  BaseCard(
    modifier = modifier,
    // ИСПРАВЛЕНО: Заменен Android R.string на КМР Res.string
    label = stringResource(Res.string.contacts),
    actionButton = {
      IconButton(onClick = { openDialog.value = true }) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Редагувати контакти"
        )
      }
    }
  ) {
    // ИСПРАВЛЕНО: Каждой строке передан явный модификатор отступа для симметрии верстки
    LabelTextWithTextAndIcon(
      modifier = Modifier.padding(vertical = 2.dp),
      imageVector = Icons.Default.Phone,
      labelText = stringResource(Res.string.phone_colon),
      valueText = phone
    )
    LabelTextWithTextAndIcon(
      modifier = Modifier.padding(vertical = 2.dp),
      imageVector = Icons.Default.AlternateEmail,
      labelText = stringResource(Res.string.email_colon),
      valueText = email
    )
  }

  if (openDialog.value) {
    ChangeContactsDialog(
      baseUIState = baseUIState,
      onDismissRequest = { openDialog.value = false },
      phone = phone,
      email = email,
      previousPhone = baseUIState.apartment.phone,
      previousEmail = baseUIState.apartment.email,
      onEmailChange = onEmailChange,
      onPhoneChange = onPhoneChange,
      onUpdateClick = onUpdateBti
    )
  }
}

/**
 * [ChangeContactsDialog] — Кроссплатформенное модальное окно изменения почты и телефона.
 */
@Composable
fun ChangeContactsDialog(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  onDismissRequest: () -> Unit,
  onUpdateClick: () -> Unit,
  phone: String,
  email: String,
  previousPhone: String,
  previousEmail: String,
  onEmailChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  // ИСПРАВЛЕНО: Низкоуровневый Dialog заменен на стандартный AlertDialog для стабильности на Mac/iOS
  AlertDialog(
    modifier = modifier.widthIn(max = 400.dp),
    onDismissRequest = onDismissRequest,
    title = {
      Text(
        text = stringResource(Res.string.update_bti),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      // ИСПРАВЛЕНО: Внутренние элементы больше не дублируют параметры входящего modifier
      Column(modifier = Modifier.fillMaxWidth()) {
        // Твои кастомные КМР-компоненты полей ввода текста (PhoneField / EmailField)
        PhoneField(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          value = phone,
          onNewValue = onPhoneChange
        )
        EmailField(
          modifier = Modifier.fillMaxWidth(),
          value = email,
          onNewValue = onEmailChange
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismissRequest()
          onEmailChange(previousEmail)
          onPhoneChange(previousPhone)
        }
      ) {
        Text(
          text = stringResource(Res.string.cancel),
          style = MaterialTheme.typography.labelLarge
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onDismissRequest()
          onUpdateClick()
        },
        // Кнопка активна только в случае реального изменения кубов/данных в полях
        enabled = (previousEmail != email || previousPhone != phone),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(
          text = stringResource(Res.string.change),
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  )
}

// Временные заглушки полей ввода для успешной компиляции файла (замени на свои из TextFields.kt)
@Composable fun PhoneField(modifier: Modifier = Modifier, value: String, onNewValue: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onNewValue, modifier = modifier, label = { Text("Телефон") }) }
@Composable fun EmailField(modifier: Modifier = Modifier, value: String, onNewValue: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onNewValue, modifier = modifier, label = { Text("Email") }) }
