package com.ykis.ykismobkmp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass // КМР импорт класса замера окон
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

// Импорты инфраструктуры, настроек и адаптивной навигации ЮКИС г. Южный
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import com.ykis.ykismobkmp.ui.navigation.YkisPamApp
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val className = "App"

/**
 * [YkisPamAppRoot] — Глобальная кроссплатформенная точка сборки интерфейса KMP.
 * ИСПРАВЛЕНО: Исправлен пропуск запятой в аргументах, тип windowSize переведен на WindowSizeClass.
 */
@Composable
fun YkisPamAppRoot(
  windowSize: WindowSizeClass, // Сквозной КМР-класс для корректной работы expect/actual мостов
  displayFeatures: List<Any>,  // Особенности экрана (Fold API) для Android-устройств
  initialChatId: String?       // Динамический токен пуш-уведомления для глубокой навигации
) {
  // Инжектируем нашу кроссплатформенную модель настроек экрана через Koin мост
  val settingsScreenModel = koinInject<SettingsScreenModel>()

  // Реактивно подписываемся на выбранную пользователем схему оформления ("dark", "light", "system")
  val currentTheme by settingsScreenModel.theme.collectAsState()

  // Логирование согласно правилу [Класс.Метод]
  println("[$className.YkisPamAppRoot]: Инициализация графического дерева. Тема: ${currentTheme ?: "system"}")

  YkisPAMTheme(appTheme = currentTheme ?: "system") {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      // Вызываем наше центральное адаптивное ядро
      // ИСПРАВЛЕНО: Расставлены все запятые, типы параметров полностью согласованы с YkisPamApp.kt
      YkisPamApp(
        windowSize = windowSize,
        displayFeatures = displayFeatures,
        initialChatId = initialChatId
      )
    }
  }
}
