package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.SnackbarMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobKMP:
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.RootNavGraph
import com.ykis.ykismobkmp.ui.navigation.rememberAdaptiveLayoutType

private const val className = "YkisPamApp"

/**
 * [YkisPamApp] — Основной визуальный адаптивный каркас приложения ЮКИС г. Южный.
 * ИСПРАВЛЕНО: Интегрирован вызов expect-моста, удалены локальные заглушки менеджера снэкбаров YkisMobKMP.
 */
@Composable
fun YkisPamApp(
  windowSize: WindowSizeClass, // Принимаем системный WindowSizeClass для работы expect/actual мостов
  displayFeatures: List<Any>,  // Особенности шлейфа экрана для Android-складных устройств
  initialChatId: String? = null
) {
  // ВНЕДРЕНИЕ НАШЕГО EXPECT-МОСТА: Вычисляем форм-фактор на основе системных замеров платформ
  val (navigationType, contentType) = rememberAdaptiveLayoutType(
    windowSize = windowSize,
    displayFeatures = displayFeatures
  )

  // Логирование рантайма согласно правилу [Класс.Метод]
  LaunchedEffect(navigationType, contentType) {
    println("[$className.YkisPamApp]: Конфігурація геометрії прийнята. Навігація=$navigationType, Контент=$contentType")
  }

  // Запускаем твой основной кроссплатформенный граф навигации Voyager
  RootNavGraph(
    appState = rememberAppState(),
    contentType = contentType,
    navigationType = navigationType,
    initialChatId = initialChatId // Пробрасываем токен пуша дальше в стейт-машину графа
  )
}

/**
 * [rememberAppState] — Инициализация кроссплатформенного состояния приложения.
 */
@Composable
fun rememberAppState(
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  snackbarManager: SnackbarManager = koinInject(), // Достаем сквозной КМР-менеджер из DI Koin YkisMobKMP
  coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(snackbarHostState, snackbarManager, coroutineScope) {
  YkisPamAppState(
    snackbarHostState = snackbarHostState,
    snackbarManager = snackbarManager,
    coroutineScope = coroutineScope
  )
}

/**
 * [YkisPamAppState] — Нативный КМР-менеджер последовательного вывода Snackbar-ошибок ГИОЦ.
 */
@Stable
class YkisPamAppState(
  val snackbarHostState: SnackbarHostState,
  private val snackbarManager: SnackbarManager,
  val coroutineScope: CoroutineScope
) {
  private val logTag = "YkisPamAppState"

  init {
    coroutineScope.launch {
      println("[$logTag.init]: Запуск кроссплатформенного слухача Snackbar повідомлень YkisMobKMP")

      snackbarManager.snackbarMessages
        .filterNotNull()
        .collect { snackbarMessage ->
          // 1. Формируем текст уведомления
          val text = try {
            when (snackbarMessage) {
              is SnackbarMessage.Resource -> {
                // Вычитываем строку через асинхронный КМР-менеджер JetBrains Res
                org.jetbrains.compose.resources.getString(snackbarMessage.resId)
              }
              is SnackbarMessage.Text -> {
                snackbarMessage.message
              }
            }
          } catch (e: Exception) {
            println("[$logTag.init] Критическая ошибка извлечения строки Res: ${e.message}")
            "Помилка відображення сповіщення"
          }

          // 2. Показываем плашку на холсте Material 3
          if (text.isNotBlank()) {
            snackbarHostState.showSnackbar(
              message = text,
              withDismissAction = true
            )

            // 3. КРИТИЧЕСКИЙ ФИКС YkisMobKMP: Очищаем менеджер для защиты от холостых вылетов при рекомпозициях
            snackbarManager.clearMessage()
            println("[$logTag.init]: Повідомлення Snackbar успішно оброблено та видалено з черги")
          }
        }
    }
  }
}

/**
 * Типы навигации в зависимости от размера экрана:
 * - BOTTOM_NAVIGATION: для телефонов (снизу)
 * - NAVIGATION_RAIL: боковая узкая панель (для планшетов)
 * - PERMANENT_DRAWER: широкая панель (для десктопов/больших экранов)
 */
enum class NavigationType {
  BOTTOM_NAVIGATION,
  NAVIGATION_RAIL_COMPACT,
  NAVIGATION_RAIL_EXPANDED,
  PERMANENT_NAVIGATION_DRAWER
}

/**
 * ContentType определяет, сколько колонок контента показывать:
 * - SINGLE_PANE: одна колонка (телефон)
 * - DUAL_PANE: две колонки (планшет/складной экран)
 */
enum class ContentType {
  SINGLE_PANE, DUAL_PANE
}

/**
 * Перечисление разделов приложения для детального отображения контента.
 */

enum class ContentDetail {
  STANDARD_USER,
  BTI,            // БТИ
  FAMILY,         // Состав семьи
  UNKNOWN,           // ОСМД / ОСББ
  OSBB,           // ОСМД / ОСББ
  WATER_SERVICE,  // Водоканал
  WARM_SERVICE,   // Теплосеть
  GARBAGE_SERVICE,// Вывоз мусора
  WATER_METER,    // Водомеры (инфо)
  HEAT_METER,     // Теплосчетчики (инфо)
  WATER_READINGS, // Показания воды
  HEAT_READINGS,  // Показания тепла
  PAYMENT_LIST,   // Список платежей
  PAYMENT_CHOICE  // Выбор оплаты (исправил CHOICE вместо PAYMENT_CHOICE)
}

