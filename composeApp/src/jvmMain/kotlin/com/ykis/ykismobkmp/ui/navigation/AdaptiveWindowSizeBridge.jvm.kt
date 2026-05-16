package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private const val className = "AdaptiveWindowSizeBridge"

/**
 * [rememberAdaptiveLayoutType] — Десктопная JVM-реализация вычисления геометрии (Mac / PC).
 * ИСПРАВЛЕНО: Применено явное булево сравнение для предотвращения ошибки вывода типов WHEN_CALL.
 */
@Composable
actual fun rememberAdaptiveLayoutType(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
): Pair<NavigationType, ContentType> {

  return remember(windowSize) {
    val widthClass = windowSize.widthSizeClass

    // РЕШЕНИЕ: Чистые логические ветки для Skiko-компилятора JVM
    when {
      widthClass == WindowWidthSizeClass.Compact -> {
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Medium -> {
        // Маленькие или сжатые окна Mac Desktop -> компактный рельс
        NavigationType.NAVIGATION_RAIL_COMPACT to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Expanded -> {
        // Полноэкранный режим на мониторах Mac -> Permanent Drawer и двухпанельный чат
        NavigationType.PERMANENT_NAVIGATION_DRAWER to ContentType.DUAL_PANE
      }
      else -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }
  }
}
