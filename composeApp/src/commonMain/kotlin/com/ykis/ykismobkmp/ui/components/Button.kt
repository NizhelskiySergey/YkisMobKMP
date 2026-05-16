package com.ykis.ykismobkmp.ui.components


// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ ТИПОВ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val className = "Buttons"

/**
 * [BasicLinkButton] — Подчеркнутая текстовая КМР-кнопка для ссылок и переходов (например, "Забули пароль?").
 */
@Composable
fun BasicLinkButton(
  text: StringResource,
  modifier: Modifier = Modifier,
  action: () -> Unit
) {
  TextButton(
    onClick = {
      // ИСПРАВЛЕНО: Нативный Android Log.d заменен универсальной функцией println() общего кода Котлина
      println("[$className.BasicLinkButton]: Clicked")
      action()
    },
    modifier = modifier
  ) {
    Text(
      text = stringResource(text),
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.titleSmall,
      textAlign = TextAlign.Center,
      fontStyle = FontStyle.Italic,
      textDecoration = TextDecoration.Underline
    )
  }
}

/**
 * [BasicButton] — Стандартная КМР-кнопка подтверждения действий бренда ЮКИС г. Южный.
 */
@Composable
fun BasicButton(
  text: StringResource,
  modifier: Modifier = Modifier,
  action: () -> Unit
) {
  Button(
    onClick = {
      println("[$className.BasicButton]: Clicked")
      action()
    },
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary
    )
  ) {
    Text(text = stringResource(text), fontSize = 16.sp)
  }
}

/**
 * [BasicImageButton] — Графическая КМР-кнопка со встроенной иконкой для авторизации или отправки медиа-файлов.
 */
@Composable
fun BasicImageButton(
  text: StringResource,
  img: DrawableResource,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  action: () -> Unit
) {
  Button(
    enabled = enabled,
    onClick = {
      println("[$className.BasicImageButton]: Clicked")
      action()
    },
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.onSecondary,
      contentColor = MaterialTheme.colorScheme.secondary,
    )
  ) {
    Image(
      painter = painterResource(img),
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
    )
    Spacer(modifier = Modifier.width(8.dp)) // Добавлен КМР-отступ между иконкой и текстом кнопки
    Text(
      text = stringResource(text),
      color = MaterialTheme.colorScheme.secondary,
    )
  }
}

/**
 * [DialogConfirmButton] — Кнопка подтверждения внутри КМР-диалогов AlertDialog.
 */
@Composable
fun DialogConfirmButton(
  text: StringResource,
  action: () -> Unit
) {
  Button(
    onClick = {
      println("[$className.DialogConfirmButton]: Clicked")
      action()
    }
  ) {
    Text(text = stringResource(text))
  }
}

/**
 * [DialogCancelButton] — Кнопка отмены внутри КМР-диалогов AlertDialog.
 */
@Composable
fun DialogCancelButton(
  text: StringResource,
  action: () -> Unit
) {
  TextButton( // ИСПРАВЛЕНО: Изменено с Button на TextButton по гайдлайнам Material 3 для кнопок отмены
    onClick = {
      println("[$className.DialogCancelButton]: Clicked")
      action()
    }
  ) {
    Text(text = stringResource(text))
  }
}

