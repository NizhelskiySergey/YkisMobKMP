package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val className = "BaseDualPanelContent"

/**
 * [BaseDualPanelContent] — Кроссплатформенный адаптивный контейнер двухпанельного отображения (Dual Pane).
 * Полностью очищен от Android DisplayFeature и оптимизирован под Mac Desktop (JVM), планшеты и iOS.
 */
@Composable
fun BaseDualPanelContent(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Платформозависимый List<DisplayFeature> полностью удален для совместимости с Desktop/iOS
  firstScreen: @Composable () -> Unit,
  secondScreen: @Composable () -> Unit,
  splitFraction: Float = 0.45f // Пропорция разделения экрана (по умолчанию 45% левая панель, 55% правая)
) {
  // ИСПРАВЛЕНО: Вместо нативного Android TwoPane используется универсальный КМР Row с весами
  Row(
    modifier = modifier.fillMaxSize()
  ) {
    // Левая панель (например, Сводный баланс ГИОЦ или Список квартир БТИ)
    Box(
      modifier = Modifier
        .weight(splitFraction)
        .fillMaxHeight()
    ) {
      firstScreen()
    }

    // Вертикальный адаптивный разделитель между экранами
    VerticalDividerKmp()

    // Правая панель (Детализация начислений, тарифные сетки или инвойсы Xpay)
    Box(
      modifier = Modifier
        .weight(1f - splitFraction)
        .fillMaxHeight()
    ) {
      secondScreen()
    }
  }
}

/**
 * [VerticalDividerKmp] — Локальный КМР-компонент аккуратной вертикальной линии Material 3.
 */
@Composable
private fun VerticalDividerKmp(modifier: Modifier = Modifier) {
  Spacer(
    modifier = modifier
      .fillMaxHeight()
      .width(1.dp)
      .background(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  )
}

