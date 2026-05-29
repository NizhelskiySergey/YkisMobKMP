package com.ykis.ykismobkmp.ui.screens.appartment

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
import com.ykis.ykismobkmp.ui.components.EmailField
import com.ykis.ykismobkmp.ui.components.LabelTextWithTextAndIcon
import com.ykis.ykismobkmp.ui.components.PhoneField
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

@Composable
fun ContactsCard(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState = BaseUIState(),
  // ИСПРАВЛЕНО НАМЕРТВО: Изменен тип с () -> Unit на (String, String) -> Unit!
  onUpdateBti: (String, String) -> Unit,
  phone: String,
  email: String,
  onEmailChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  val openDialog = remember { mutableStateOf(false) }

  BaseCard(
    modifier = modifier,
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
      initialPhone = phone,
      initialEmail = email,
      onEmailChange = onEmailChange,
      onPhoneChange = onPhoneChange,
      onUpdateClick = onUpdateBti // Передаем двухпараметрический канал дальше
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
  // ИСПРАВЛЕНО НАМЕРТВО: Изменен тип с () -> Unit на (String, String) -> Unit для синхронизации!
  onUpdateClick: (String, String) -> Unit,
  initialPhone: String,
  initialEmail: String,
  onEmailChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  var localPhone by remember { mutableStateOf(initialPhone) }
  var localEmail by remember { mutableStateOf(initialEmail) }

  val previousPhone = baseUIState.apartment.phone.orEmpty().trim()
  val previousEmail = baseUIState.apartment.email.orEmpty().trim()

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
      Column(modifier = Modifier.fillMaxWidth()) {
        PhoneField(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          value = localPhone,
          onNewValue = { localPhone = it }
        )
        EmailField(
          modifier = Modifier.fillMaxWidth(),
          value = localEmail,
          onNewValue = { localEmail = it }
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismissRequest()
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
          // АТОМАРНО ПРОШИВАЕМ ДАННЫЕ В МОМЕНТ КЛИКА СОХРАНЕНИЯ ПРЯМО В КТOR СЕТЬ!
          onUpdateClick(localPhone, localEmail)
          onDismissRequest()
        },
        enabled = (previousEmail != localEmail.trim() || previousPhone != localPhone.trim()),
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



