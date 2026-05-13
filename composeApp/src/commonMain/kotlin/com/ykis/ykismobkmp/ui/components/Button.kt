package com.ykis.ykismobkmp.ui.components


import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.core.utils.Log
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val className = "ButtonsKt"

@Composable
fun BasicLinkButton(
  text: StringResource,
  modifier: Modifier = Modifier,
  action: () -> Unit
) {
  TextButton(
    onClick = {
      Log.d("YkisLog", "[$className.BasicLinkButton]: Clicked")
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

@Composable
fun BasicButton(
  text: StringResource,
  modifier: Modifier = Modifier,
  action: () -> Unit
) {
  Button(
    onClick = {
      Log.d("YkisLog", "[$className.BasicButton]: Clicked")
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
      Log.d("YkisLog", "[$className.BasicImageButton]: Clicked")
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
    Text(
      text = stringResource(text),
      color = MaterialTheme.colorScheme.secondary,
    )
  }
}

@Composable
fun DialogConfirmButton(
  text: StringResource,
  action: () -> Unit
) {
  Button(
    onClick = {
      Log.d("YkisLog", "[$className.DialogConfirmButton]: Clicked")
      action()
    }
  ) {
    Text(text = stringResource(text))
  }
}

@Composable
fun DialogCancelButton(
  text: StringResource,
  action: () -> Unit
) {
  Button(
    onClick = {
      Log.d("YkisLog", "[$className.DialogCancelButton]: Clicked")
      action()
    }
  ) {
    Text(text = stringResource(text))
  }
}

