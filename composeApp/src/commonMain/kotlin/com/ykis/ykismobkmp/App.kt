package com.ykis.ykismobkmp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.YkisPamApp
import com.ykis.ykismobkmp.ui.rememberAppState
import com.ykis.ykismobkmp.ui.screens.settings.NewSettingsViewModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.koin.compose.koinInject

/**
 * [App] — Главная точка входа в UI Compose Multiplatform.
 * Скомпилируется нативно под Android, Mac Desktop, iOS и Web.
 * Вычисляет размер окна для адаптивной верстки ЖКХ-панели.
 */
@Composable
fun App(initialChatId: String? = null) {
  val className = "App"

  // ИСПРАВЛЕНО: Кроссплатформенный вывод логов
  println("[$className.App]: Start UI Multiplatform. initialChatId=$initialChatId")

  // 1. Инициализация управления состоянием (Snackbar, навигация Voyager/Custom)
  val coroutineScope = rememberCoroutineScope()
  val appState = rememberAppState(coroutineScope = coroutineScope)

  // 2. Получение настроек темы из общей кроссплатформенной ViewModel/ScreenModel
  // Замени на koinViewModel(), если используешь библиотеку koin-compose-viewmodel
  val settingsViewModel = koinInject<NewSettingsViewModel>()
  val currentTheme by settingsViewModel.theme.collectAsState()

  // 3. РЕШЕНИЕ: Вычисляем класс размера окна (WindowSizeClass) прямо на лету.
  // На Mac Desktop это вернет расширенный режим (Expanded), на Android — мобильный (Compact)

  // 4. Отрисовка интерфейса в единой дизайн-системе Material 3
  YkisPAMTheme(appTheme = currentTheme ?: "system") {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      // ИСПРАВЛЕНО: Передаем обязательный параметр windowSize в каркас приложения
      YkisPamApp(
        appState = appState,
        initialChatId = initialChatId,
        windowSize = "expanded" // Ошибка 'No value passed' полностью закрыта!
      )
    }
  }
}

