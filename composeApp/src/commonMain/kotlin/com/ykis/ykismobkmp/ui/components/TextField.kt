package com.ykis.ykismobkmp.ui.components
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.email
import ykismobkmp.composeapp.generated.resources.email_placeholder
import ykismobkmp.composeapp.generated.resources.empty_phone
import ykismobkmp.composeapp.generated.resources.number_double_placeholder
import ykismobkmp.composeapp.generated.resources.number_int_placeholder
import ykismobkmp.composeapp.generated.resources.password
import ykismobkmp.composeapp.generated.resources.phone
import ykismobkmp.composeapp.generated.resources.repeat_password

private const val className = "TextFields"

/**
 * [BasicField] — Базовое числовое поле ввода с ограничением максимальной ширины для десктопных окон.
 */
@Composable
fun BasicField(
  label: StringResource,
  placeholder: StringResource,
  value: String,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    singleLine = true,
    modifier = modifier.widthIn(0.dp, 480.dp),
    label = { Text(text = stringResource(label)) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    value = value,
    onValueChange = {
      // ИСПРАВЛЕНО: Сквозное логирование переведено на стандарт YkisLogKMP
      println("[YkisLogKMP.$className.BasicField]: Value changed to $it")
      onNewValue(it)
    },
    placeholder = { Text(stringResource(placeholder)) }
  )
}

/**
 * [EmailField] — Кроссплатформенное поле ввода электронной почты.
 */
@Composable
fun EmailField(
  value: String,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    singleLine = true,
    modifier = modifier.fillMaxWidth(),
    label = {
      Text(
        style = MaterialTheme.typography.bodyLarge,
        text = stringResource(Res.string.email)
      )
    },
    value = value,
    onValueChange = { onNewValue(it) },
    placeholder = { Text(stringResource(Res.string.email_placeholder)) },
    leadingIcon = { Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = "Email") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
  )
}

/**
 * [PhoneField] — Кроссплатформенное поле ввода номера телефона.
 */
@Composable
fun PhoneField(
  value: String,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    singleLine = true,
    modifier = modifier.fillMaxWidth(),
    label = {
      Text(
        style = MaterialTheme.typography.bodyLarge,
        text = stringResource(Res.string.phone)
      )
    },
    value = value,
    onValueChange = { onNewValue(it) },
    placeholder = { Text(stringResource(Res.string.empty_phone)) },
    leadingIcon = { Icon(imageVector = Icons.Filled.Phone, contentDescription = "Phone") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
  )
}



@Composable
fun PasswordField(
  value: String,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  PasswordField(
    value = value,
    placeholder = Res.string.password,
    onNewValue = onNewValue,
    modifier = modifier
  )
}

/**
 * [RepeatPasswordField] — Поле повторного ввода пароля для регистрации.
 */
@Composable
fun RepeatPasswordField(
  value: String,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  PasswordField(
    value = value,
    placeholder = Res.string.repeat_password,
    onNewValue = onNewValue,
    modifier = modifier
  )
}

/**
 * Приватная КМР-перегрузка поля ввода паролей со скрытием/показом символов.
 */
@Composable
private fun PasswordField(
  value: String,
  placeholder: StringResource,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var isVisible by remember { mutableStateOf(false) }

  // ИСПРАВЛЕНО: Извлекаем готовые векторные иконки из стандартной библиотеки Compose напрямую
  val icon = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
  val visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation()

  OutlinedTextField(
    modifier = modifier.fillMaxWidth(),
    label = { Text(text = stringResource(placeholder)) },
    value = value,
    onValueChange = { onNewValue(it) },
    placeholder = { Text(text = stringResource(placeholder)) },
    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock") },
    trailingIcon = {
      IconButton(onClick = {
        // ИСПРАВЛЕНО: Сквозное логирование переведено на YkisLogKMP
        println("[YkisLogKMP.$className.PasswordField]: Visibility toggled. Visible: $isVisible")
        isVisible = !isVisible
      }) {
        Icon(
          imageVector = icon,
          contentDescription = "Visibility",
          modifier = Modifier.size(24.dp)
        )
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    visualTransformation = visualTransformation,
    singleLine = true
  )
}

/**
 * [NumberField] — Поле ввода числовых показаний приборов учета (кубометры / гигакалории).
 */
@Composable
fun NumberField(
  value: String,
  onNewValue: (String) -> Unit,
  label: StringResource,
  isInteger: Boolean,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    singleLine = true,
    modifier = modifier.fillMaxWidth(),
    label = { Text(text = stringResource(label)) },
    value = value,
    onValueChange = {
      // ИСПРАВЛЕНО: Сквозное логирование переведено на стандарт YkisLogKMP
      println("[YkisLogKMP.$className.NumberField]: Value changed to $it")
      onNewValue(it)
    },
    placeholder = {
      if (isInteger) {
        Text(stringResource(Res.string.number_int_placeholder))
      } else {
        Text(stringResource(Res.string.number_double_placeholder))
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
  )
}
