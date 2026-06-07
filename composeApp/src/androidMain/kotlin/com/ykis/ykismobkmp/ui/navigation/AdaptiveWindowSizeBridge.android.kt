package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature

private const val className = "AdaptiveWindowSizeBridge"

private sealed interface AndroidDevicePosture {
  data object NormalPosture : AndroidDevicePosture
  data class BookPosture(val bounds: Rect) : AndroidDevicePosture
  data class Separating(val bounds: Rect, val isVertical: Boolean) : AndroidDevicePosture
}

@Composable
actual fun rememberAdaptiveLayoutType(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
): Pair<NavigationType, ContentType> {

  return remember(windowSize, displayFeatures) {
    val androidFeatures = displayFeatures.filterIsInstance<DisplayFeature>()
    val foldingFeature = androidFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

    val foldingDevicePosture = when {
      foldingFeature != null && foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
        foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL -> {
        AndroidDevicePosture.BookPosture(
          Rect(
            foldingFeature.bounds.left.toFloat(),
            foldingFeature.bounds.top.toFloat(),
            foldingFeature.bounds.right.toFloat(),
            foldingFeature.bounds.bottom.toFloat()
          )
        )
      }
      foldingFeature != null && foldingFeature.isSeparating -> {
        AndroidDevicePosture.Separating(
          Rect(
            foldingFeature.bounds.left.toFloat(),
            foldingFeature.bounds.top.toFloat(),
            foldingFeature.bounds.right.toFloat(),
            foldingFeature.bounds.bottom.toFloat()
          ),
          foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL
        )
      }
      else -> AndroidDevicePosture.NormalPosture
    }

    val widthClass = windowSize.widthSizeClass

    // ВОЗВРАЩЕНО: Используем Rail для средних и больших экранов, но с сохранением BottomBar в Scaffold
    when {
      widthClass == WindowWidthSizeClass.Compact -> {
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Medium -> {
        val nav = NavigationType.NAVIGATION_RAIL_COMPACT
        val content = if (foldingDevicePosture != AndroidDevicePosture.NormalPosture) {
          ContentType.DUAL_PANE
        } else {
          ContentType.SINGLE_PANE
        }
        nav to content
      }
      widthClass == WindowWidthSizeClass.Expanded -> {
        val nav = if (foldingDevicePosture is AndroidDevicePosture.BookPosture) {
          NavigationType.NAVIGATION_RAIL_EXPANDED
        } else {
          NavigationType.PERMANENT_NAVIGATION_DRAWER
        }
        nav to ContentType.DUAL_PANE
      }
      else -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }
  }
}
