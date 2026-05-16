package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ РЕСУРСОВ JETBRAINS И COIL 3 KMP:
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_account_circle

private const val className = "UserImage"

/**
 * [UserImage] — Кроссплатформенный компонент отображения круглой аватарки абонента ЮКИС.
 * Полностью очищен от Android Context, Coil 2 билдеров и готов к сборке на Mac Desktop (JVM) и iOS.
 */
@Composable
fun UserImage(
  modifier: Modifier = Modifier,
  photoUrl: String
) {
  // ИСПРАВЛЕНО: Кроссплатформенный Coil 3 AsyncImage принимает строку URL напрямую в model без Context
  AsyncImage(
    model = photoUrl,
    contentDescription = "Аватар користувача",
    // ИСПРАВЛЕНО: Заменен Android R.drawable на КМР-ресурс генератора JetBrains Res
    error = painterResource(Res.drawable.ic_account_circle),
    placeholder = painterResource(Res.drawable.ic_account_circle),
    contentScale = ContentScale.Crop, // Изменено на Crop для правильного заполнения круга без полей
    modifier = modifier
      .size(48.dp) // ИСПРАВЛЕНО: Раздельные width/height объединены в компактный size
      .clip(CircleShape)
  )
}

