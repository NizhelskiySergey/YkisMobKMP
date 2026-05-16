package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature

private const val className = "AdaptiveWindowSizeBridge"

/**
 * [AndroidDevicePosture] — Внутренние запечатанные состояния геометрии гибких экранов Android.
 * ИСПРАВЛЕНО: NormalPosture объявлен как data object, что полностью убирает ошибку отсутствия конструктора.
 */
private sealed interface AndroidDevicePosture {
  data object NormalPosture : AndroidDevicePosture
  data class BookPosture(val bounds: Rect) : AndroidDevicePosture
  data class Separating(val bounds: Rect, val isVertical: Boolean) : AndroidDevicePosture
}

/**
 * [rememberAdaptiveLayoutType] — Android-реализация expect-контракта с поддержкой Fold-устройств.
 * ИСПРАВЛЕНО: Полное устранение ошибок WHEN_CALL и конфликтов возвращаемых типов Unit.
 */
@Composable
actual fun rememberAdaptiveLayoutType(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
): Pair<NavigationType, ContentType> {

  return remember(windowSize, displayFeatures) {
    // Безопасно фильтруем и кастим общий КМР-список в нативные классы Android WindowManager
    val androidFeatures = displayFeatures.filterIsInstance<DisplayFeature>()
    val foldingFeature = androidFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

    // Вычисляем текущую позу смартфона на основе нативного SDK
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
      // ИСПРАВЛЕНО: Обращение к data object пишется без круглых скобок ()
      else -> AndroidDevicePosture.NormalPosture
    }

    val widthClass = windowSize.widthSizeClass

    // ИСПРАВЛЕНО: when развернут в виде чистых булевых условий (==) без аргументов в скобках.
    // Блок является последним выражением лямбды и напрямую возвращает Pair<NavigationType, ContentType> наружу.
    when {
      widthClass == WindowWidthSizeClass.Compact -> {
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
      }
      widthClass == WindowWidthSizeClass.Medium -> {
        val nav = NavigationType.NAVIGATION_RAIL_COMPACT
        // ИСПРАВЛЕНО: Сравнение с синглтоном выполняется без вызова конструктора ()
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
