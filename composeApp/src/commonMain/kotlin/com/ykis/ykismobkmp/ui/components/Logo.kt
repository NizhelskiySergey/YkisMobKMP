package com.ykis.ykismobkmp.ui.components


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.app_name
import ykismobkmp.composeapp.generated.resources.full_name
import ykismobkmp.composeapp.generated.resources.ykis

private const val className = "LogoImageKt"

@Composable
fun LogoImage(modifier: Modifier = Modifier) {
  // Логирование согласно правилу [Класс.Метод]
  Log.d("YkisLog", "[$className.LogoImage]: Rendering logo")

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val imageModifier = Modifier
      .size(60.dp)
      .clip(CircleShape)
      .border(BorderStroke(0.dp, Color.Transparent))
      .background(Color.Transparent)
      .align(Alignment.CenterVertically)

    Image(
      painter = painterResource(Res.drawable.ykis),
      contentDescription = stringResource(Res.string.app_name),
      contentScale = ContentScale.Fit,
      modifier = imageModifier,
      alignment = Alignment.Center
    )

    Text(
      style = MaterialTheme.typography.titleSmall,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.primary,
      text = stringResource(Res.string.full_name),
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

