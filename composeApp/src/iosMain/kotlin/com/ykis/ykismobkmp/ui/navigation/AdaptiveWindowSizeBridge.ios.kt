package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private const val className = "AdaptiveWindowSizeBridge"

/**
 * [rememberAdaptiveLayoutType] — Нативная iOS-реализация вычисления геометрии (iPhone / iPad).
 * ИСПРАВЛЕНО: Выражение when развернуто в логические равенства для бесшовной трансляции в Swift/Obj-C.
 */
@Composable
actual fun rememberAdaptiveLayoutType(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
): Pair<NavigationType, ContentType> {

  return remember(windowSize) {
    val widthClass = windowSize.widthSizeClass

    when {
      widthClass == WindowWidthSizeClass.Compact -> {
        // Стандартные экраны iPhone -> мобильный нижний бар
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Medium -> {
        // Экраны планшетов iPad в портретной ориентации -> компактный рельс
        NavigationType.NAVIGATION_RAIL_COMPACT to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Expanded -> {
        // Планшеты iPad в ландшафтном режиме -> широкое боковое меню
        NavigationType.PERMANENT_NAVIGATION_DRAWER to ContentType.DUAL_PANE
      }
      else -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }
  }
}
