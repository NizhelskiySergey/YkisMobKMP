package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private const val className = "AdaptiveWindowSizeBridge"

/**
 * [rememberAdaptiveLayoutType] — Нативная Web-реализация вычисления геометрии (Kotlin/JS).
 * ИСПРАВЛЕНО: Устранен конфликт WHEN_CALL путем перевода на явные булевы условия.
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
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Medium -> {
        NavigationType.NAVIGATION_RAIL_EXPANDED to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Expanded -> {
        NavigationType.NAVIGATION_RAIL_EXPANDED to ContentType.DUAL_PANE
      }
      else -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }
  }
}
