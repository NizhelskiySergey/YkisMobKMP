package com.ykis.ykismobkmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val className = "DetailPanel"

/**
 * [DetailPanel] — Кроссплатформенный компонент скользящей панели для плавного вывода финансовых и БТИ деталей ЮКИС.
 * Полностью автономен, изолирован по модификаторам и оптимизирован под Mac Desktop (JVM), Android и iOS.
 */
@Composable
fun DetailPanel(
  modifier: Modifier = Modifier,
  showDetail: Boolean,
  detailContent: @Composable () -> Unit
) {
  // Анимированный КМР-контейнер видимости с плавным выезжанием снизу
  AnimatedVisibility(
    modifier = modifier, // Входящий модификатор позиционирования отдается контейнеру верхнего уровня
    visible = showDetail,
    enter = slideInVertically(
      animationSpec = tween(
        durationMillis = 550,
        easing = EaseOutCubic
      ),
      initialOffsetY = { it }
    ) + fadeIn(
      animationSpec = tween(durationMillis = 400)
    ),
    exit = slideOutVertically(
      targetOffsetY = { it }
    ) + fadeOut()
  ) {
    Card(
      // ИСПРАВЛЕНО: Цепочка модификаторов карточки изолирована от внешнего modifier для защиты геометрии окон
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp), // Оптимизированы отступы по гайдлайнам Material 3
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer
      )
    ) {
      // Внедряем дочерний КМР-контент (например, ServiceDetailScreen)
      detailContent()
    }
  }
}

