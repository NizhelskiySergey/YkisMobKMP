package com.ykis.ykismobkmp.ui.components

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ ТИПОВ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

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
      // ИСПРАВЛЕНО: Нативный Android Log.d заменен универсальной функцией println() общего кода Котлина
      println("[$className.BasicField]: Value changed to $it")
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

/**
 * [PasswordField] — Поле ввода пароля авторизации.
 * ИСПРАВЛЕНО: Добавлен пропущенный модификатор во внутреннюю перегрузку функции.
 */
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

  val icon = if (isVisible) painterResource(Res.drawable.ic_visibility_on)
  else painterResource(Res.drawable.ic_visibility_off)

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
        println("[$className.PasswordField]: Visibility toggled")
        isVisible = !isVisible
      }) {
        Icon(painter = icon, contentDescription = "Visibility", modifier = Modifier.size(24.dp))
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
      println("[$className.NumberField]: Value changed to $it")
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
