package com.ykis.ykismobkmp.ui


import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.ui.navigation.RootNavGraph
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject

/**
 * [YkisPamApp] — Основной визуальный каркас приложения.
 * Адаптирует интерфейс ЖКХ-панели под мобильный (Android/iOS) и десктопный (Mac JVM) режимы
 * на основе кроссплатформенной текстовой конфигурации windowSize.
 */
@Composable
fun YkisPamApp(
  appState: YkisPamAppState,
  initialChatId: String? = null,
  windowSize: String
) {
  val className = "YkisPamApp"

  // ИСПРАВЛЕНО: Кроссплатформенный вывод логов
  println("[$className.YkisPamApp]: Start Multiplatform UI. WindowSize=$windowSize, initialChatId=$initialChatId")

  // Логика адаптивности для KMP на основе строк ("compact", "medium", "expanded")
  val (navigationType, contentType) = when (windowSize.lowercase()) {
    "compact" -> {
      NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }
    "medium" -> {
      NavigationType.NAVIGATION_RAIL to ContentType.SINGLE_PANE
    }
    "expanded" -> {
      // Режим Permanent Drawer (Боковой сайдбард) и Dual Pane (Двухпанельный чат) для твоего Mac Desktop
      NavigationType.PERMANENT_DRAWER to ContentType.DUAL_PANE
    }
    else -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
  }

  // Основной навигационный граф приложения
  RootNavGraph(
    appState = appState,
    contentType = contentType,
    navigationType = navigationType,
    initialChatId = initialChatId
  )
}

/**
 * [rememberAppState] — Инициализация кроссплатформенного состояния приложения.
 * Использует koinInject() для SnackbarManager, обеспечивая глобальный перехват ошибок.
 */
@Composable
fun rememberAppState(
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  snackbarManager: SnackbarManager = koinInject(), // Достаем кроссплатформенный менеджер уведомлений
  coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(snackbarHostState, snackbarManager, coroutineScope) {
  YkisPamAppState(
    snackbarHostState = snackbarHostState,
    snackbarManager = snackbarManager,
    coroutineScope = coroutineScope
  )
}
/**
 * Перечисления для адаптивного интерфейса в KMP (Android + Desktop Mac + iOS)
 */
enum class NavigationType {
  BOTTOM_NAVIGATION, NAVIGATION_RAIL, PERMANENT_DRAWER
}

enum class ContentType {
  SINGLE_PANE, DUAL_PANE
}

