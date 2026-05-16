package com.ykis.ykismobkmp.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val className = "AnimatedCircle"
private const val DividerLengthInDegrees = 0f

/**
 * [AnimatedCircleProgress] — Нативные КМР-состояния прокрутки финансовой анимации.
 */
private enum class AnimatedCircleProgress { START, END }

/**
 * [AnimatedCircle] — Кроссплатформенный интерактивный Canvas-компонент круговой диаграммы долей задолженности ГИОЦ.
 * Оптимизирован под рендеринг Skiko на Mac Desktop (JVM), а также нативные графические конвейеры Android и iOS.
 */
@Composable
fun AnimatedCircle(
  proportions: List<Double>,
  colors: List<Color>,
  modifier: Modifier = Modifier
) {
  // Автоматическая нормализация входящих ЖКХ-сумм (Приведение к КМР-стандарту долей от 0.0 до 1.0)
  val normalizedProportions = remember(proportions) {
    val totalSum = proportions.sum()
    if (totalSum <= 0.0) {
      List(proportions.size) { 1.0 / proportions.size }
    } else {
      proportions.map { it / totalSum }
    }
  }

  val currentState = remember {
    MutableTransitionState(AnimatedCircleProgress.START).apply {
      targetState = AnimatedCircleProgress.END
    }
  }

  // Толщина линии круга Material 3 адаптирована под пиксельную плотность целевой ОС
  val stroke = with(LocalDensity.current) { Stroke(32.dp.toPx()) }
  val transition = updateTransition(currentState, label = "CircleTransition")

  val angleOffset by transition.animateFloat(
    transitionSpec = {
      tween(
        delayMillis = 200,
        durationMillis = 1000,
        easing = LinearOutSlowInEasing
      )
    }, label = "AngleOffsetAnim"
  ) { progress ->
    if (progress == AnimatedCircleProgress.START) 0f else 360f
  }

  val shift by transition.animateFloat(
    transitionSpec = {
      tween(
        delayMillis = 250,
        durationMillis = 900,
        easing = CubicBezierEasing(0f, 0.75f, 0.35f, 0.85f)
      )
    }, label = "ShiftAnim"
  ) { progress ->
    if (progress == AnimatedCircleProgress.START) 0f else 30f
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val innerRadius = (size.minDimension - stroke.width) / 2
    val halfSize = size / 2.0f
    val topLeft = Offset(
      halfSize.width - innerRadius,
      halfSize.height - innerRadius
    )
    val arcSize = Size(innerRadius * 2, innerRadius * 2)
    var startAngle = shift - 90f

    normalizedProportions.forEachIndexed { index, proportion ->
      // Теперь sweep займет строго положенную долю от 360 градусов (например, 0.25 * 360 = 90)
      val sweep = proportion * angleOffset

      // Защита от OutOfBoundsException, если количество цветов не совпадает с количеством ЖКХ-служб
      val sectorColor = colors.getOrNull(index) ?: Color.Gray

      drawArc(
        color = sectorColor,
        startAngle = startAngle + DividerLengthInDegrees / 2,
        sweepAngle = (sweep - DividerLengthInDegrees).toFloat(),
        topLeft = topLeft,
        size = arcSize,
        useCenter = false,
        style = stroke
      )
      startAngle += sweep.toFloat()
    }
  }
}

