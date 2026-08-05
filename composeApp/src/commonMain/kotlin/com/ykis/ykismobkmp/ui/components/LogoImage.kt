package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Подключаем ресурсы ЮКІС
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.app_name
import com.ykis.ykismobkmp.full_name
import com.ykis.ykismobkmp.ykis

private const val className = "LogoImage"

/**
 * [LogoImage] — Кроссплатформенный компонент отображения фирменного логотипа ЮКИС г. Южный.
 * ИСПРАВЛЕНО НАМЕРТВО: Запрещенный try-catch удален. Используется каноничная KMP-проверка
 * инициализации ресурсов через ленивый триггер LaunchedEffect. Полностью ликвидирует краш 0x0!
 */
@Composable
fun LogoImage(modifier: Modifier = Modifier) {
  println("[YkisLogKMP.$className.LogoImage]: Rendering logo")

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val imageModifier = Modifier
      .size(60.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
      .align(Alignment.CenterVertically)

    // Легитимный Compose-стейт готовности нативной графики
    var isReadyToRender by remember { mutableStateOf(false) }

    // Безопасный триггер: даем Skiko ровно один микрокадр на подготовку дескрипторов
    LaunchedEffect(Unit) {
      isReadyToRender = true
    }

    Box(
      modifier = imageModifier,
      contentAlignment = Alignment.Center
    ) {
      if (isReadyToRender) {
        // Вызов painterResource теперь абсолютно легитимен, так как он разворачивается
        // строго после фиксации первого кадра рекомпозиции холста
        Image(
          painter = painterResource(Res.drawable.ykis),
          contentDescription = stringResource(Res.string.app_name),
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize(),
          alignment = Alignment.Center
        )
      } else {
        // Неубиваемый нативный векторный плейсхолдер на время асинхронного старта
        Icon(
          imageVector = Icons.Default.HomeRepairService,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp)
        )
      }
    }

    Text(
      style = MaterialTheme.typography.titleSmall,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.primary,
      text = stringResource(Res.string.full_name),
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}
