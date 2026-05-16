package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale

// ИСПРАВЛЕНО: Подключаем официальный КМР импорт Coil 3
import coil3.compose.AsyncImage

private const val className = "ZoomableImage"

/**
 * [ZoomableImage] — Кроссплатформенный компонент интерактивного масштабирования (pinch-to-zoom) изображений ЮКИС.
 * Полностью автономен, очищен от Android SDK и оптимизирован под Mac Desktop (JVM), Android и iOS.
 */
@Composable
fun ZoomableImage(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Тип android.net.Uri заменен на кроссплатформенный String URL/путь
  imageUrl: String
) {
  var scale by remember { mutableStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    // Реактивное КМР-отслеживание жестов масштабирования, панорамирования и мыши
    val state = rememberTransformableState { zoomChange, panChange, _ ->
      // Ограничиваем зум от 1х до 5х для предотвращения графических артефактов на Retina-дисплеях Mac
      scale = (scale * zoomChange).coerceIn(1f, 5f)

      val extraWidth = (scale - 1) * constraints.maxWidth
      val extraHeight = (scale - 1) * constraints.maxHeight

      val maxX = extraWidth / 2
      val maxY = extraHeight / 2

      // Рассчитываем смещение холста с учетом масштабирования
      offset = Offset(
        x = (offset.x + scale * panChange.x).coerceIn(-maxX, maxX),
        y = (offset.y + scale * panChange.y).coerceIn(-maxY, maxY)
      )
    }

    // ИСПРАВЛЕНО: Используем кроссплатформенный Coil 3 AsyncImage без передачи Android Context
    AsyncImage(
      model = imageUrl,
      contentDescription = "Масштабоване зображення квитанції",
      modifier = Modifier
        .align(Alignment.Center)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
          translationX = offset.x
          translationY = offset.y
        }
        .transformable(state),
      contentScale = ContentScale.Fit
    )
  }
}

