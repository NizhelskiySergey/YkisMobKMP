package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Глобальные ключи доступа Compose Local Provider.
 * Позволяют любому экрану ЮКИС вычитать тип развертки напрямую из контекста рантайма.
 */
val LocalContentType = staticCompositionLocalOf { ContentType.SINGLE_PANE }
val LocalNavigationType = staticCompositionLocalOf { NavigationType.BOTTOM_NAVIGATION }

/**
 * [AdaptiveWindowSizeProvider] — Единый КМР-замерщик геометрии приложения.
 * Оборачивает корень приложения, вычисляет размеры окон Mac Desktop/смартфона ровно один раз
 * и реактивно обновляет контекст при изменении размеров окна пользователем.
 */
@Composable
fun AdaptiveWindowSizeProvider(
  content: @Composable () -> Unit
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val windowWidth = maxWidth

    // Вычисляем ContentType на основе брейкпоинтов Material 3
    val computedContentType = if (windowWidth >= 600.dp) {
      ContentType.DUAL_PANE
    } else {
      ContentType.SINGLE_PANE
    }

    // Вычисляем детальный NavigationType по твоей четырехступенчатой сетке
    val computedNavigationType = when {
      windowWidth >= 840.dp -> NavigationType.PERMANENT_NAVIGATION_DRAWER
      windowWidth >= 600.dp -> NavigationType.NAVIGATION_RAIL_EXPANDED
      windowWidth >= 400.dp -> NavigationType.NAVIGATION_RAIL_COMPACT
      else -> NavigationType.BOTTOM_NAVIGATION
    }

    // Пробрасываем вычисленные значения по всему дереву Compose вниз
    CompositionLocalProvider(
      LocalContentType provides computedContentType,
      LocalNavigationType provides computedNavigationType
    ) {
      content()
    }
  }
}
