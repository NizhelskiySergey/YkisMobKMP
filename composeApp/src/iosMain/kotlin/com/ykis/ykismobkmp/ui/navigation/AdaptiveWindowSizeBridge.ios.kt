package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private const val className = "AdaptiveWindowSizeBridge"

@Composable
actual fun rememberAdaptiveLayoutType(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
): Pair<NavigationType, ContentType> {

  return remember(windowSize) {
    val widthClass = windowSize.widthSizeClass
    val heightClass = windowSize.heightSizeClass

    println("[YkisLogKMP.IOS_LAYOUT]: WidthClass=$widthClass, HeightClass=$heightClass")

    when {
      widthClass == WindowWidthSizeClass.Compact -> {
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Medium -> {
        // На iPad 7 это ПОРТРЕТ (810 dp). Показываем одну панель.
        NavigationType.NAVIGATION_RAIL_EXPANDED to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Expanded -> {
        // На iPad 7 это ЛАНДШАФТ (1080 dp). Показываем две панели.
        NavigationType.NAVIGATION_RAIL_EXPANDED to ContentType.DUAL_PANE
      }
      else -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }
  }
}
