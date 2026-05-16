package com.ykis.ykismobkmp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

// Импорты инфраструктуры, настроек и адаптивной навигации ЮКИС
import com.ykis.ykismobkmp.ui.navigation.SignInScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val tag = "App"

/**
 * [YkisPamApp] — Главное кроссплатформенное ядро приложения ЮКИС.
 * Стабильно запускается на Mac Desktop (JVM), Android и iOS.
 */
@Composable
fun YkisPamApp(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>, // КМР-совместимый супертип для поддержки складных экранов
  initialChatId: String?       // Динамический токен уведомления для сквозного перехода
) {
  // ИСПРАВЛЕНО: Вместо Android koinViewModel() используем кроссплатформенный koinInject()
  val settingsScreenModel = koinInject<SettingsScreenModel>()
  val currentTheme by settingsScreenModel.theme.collectAsState()

  // Внедряем твою тему оформления ЮКИС
  YkisPAMTheme(appTheme = currentTheme ?: "system") {

    // ШАГ 1: Аппаратно раскатываем вычисленный windowSize во все вложенные КМР-экраны
    AdaptiveWindowSizeBridge(windowSize = windowSize) {

      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
      ) {
        // ШАГ 2: Запускаем навигатор Voyager с базовым экраном авторизации
        // Внутри SignInScreen adaptiveNavigationType считает стейт пушей автоматически
        cafe.adriel.voyager.navigator.Navigator(
          screen = com.ykis.ykismobkmp.ui.screens.auth.SignInScreen()
        )
      }
    }
  }
}
