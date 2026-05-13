package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res

private const val className = "TextFieldsKt"

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
      Log.d("YkisLog", "[$className.BasicField]: Value changed to $it")
      onNewValue(it)
    },
    placeholder = { Text(stringResource(placeholder)) }
  )
}

@Composable
fun EmailField(value: String, onNewValue: (String) -> Unit, modifier: Modifier = Modifier) {
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
    leadingIcon = { Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = "Email") }
  )
}

@Composable
fun PhoneField(value: String, onNewValue: (String) -> Unit, modifier: Modifier = Modifier) {
  OutlinedTextField(
    modifier = modifier.fillMaxWidth(),
    label = {
      Text(
        style = MaterialTheme.typography.bodyLarge,
        text = stringResource(Res.string.phone)
      )
    },
    value = value,
    onValueChange = { onNewValue(it) },
    placeholder = { Text(stringResource(Res.string.phone_placeholder)) },
    leadingIcon = { Icon(imageVector = Icons.Filled.Phone, contentDescription = "Phone") }
  )
}

@Composable
fun PasswordField(value: String, onNewValue: (String) -> Unit) {
  PasswordField(value, Res.string.password, onNewValue)
}

@Composable
fun RepeatPasswordField(
  value: String,
  onNewValue: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  PasswordField(value, Res.string.repeat_password, onNewValue, modifier)
}

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
        Log.d("YkisLog", "[$className.PasswordField]: Visibility toggled")
        isVisible = !isVisible
      }) {
        Icon(painter = icon, contentDescription = "Visibility")
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    visualTransformation = visualTransformation
  )
}

@Composable
fun NumberField(
  value: String,
  onNewValue: (String) -> Unit,
  label: StringResource,
  isInteger: Boolean
) {
  OutlinedTextField(
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(text = stringResource(label)) },
    value = value,
    onValueChange = {
      Log.d("YkisLog", "[$className.NumberField]: Value changed to $it")
      onNewValue(it)
    },
    placeholder = {
      if (isInteger) {
        Text(stringResource(Res.string.number_int_placeholder))
      } else {
        Text(stringResource(Res.string.number_double_placeholder))
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
  )
}
