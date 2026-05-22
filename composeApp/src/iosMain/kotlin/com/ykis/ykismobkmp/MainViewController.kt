package com.ykis.ykismobkmp

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import platform.UIKit.UIViewController
import org.koin.dsl.module
import com.ykis.ykismobkmp.di.initKoin // Твой оригинальный метод из commonMain

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun createDefaultWindowSizeClass(): WindowSizeClass {
  return WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
}

/**
 * [MainViewController] — Главная точка входа графического холста для iOS.
 * Автономная самоинициализация Koin графа полностью возвращена!
 */
fun MainViewController(windowSize: WindowSizeClass, initialChatId: String?): UIViewController {

  val isKoinActive = try {
    org.koin.mp.KoinPlatform.getKoin() != null
  } catch (e: Exception) {
    false
  }

  if (!isKoinActive) {
    try {
      initKoin(
        platformModule = module { },
        appDeclaration = { }
      )
      println("[MainViewController.iosMain]: Граф Koin успешно самоинициализирован на стороне Kotlin-Native")
    } catch (e: Exception) {
      println("[MainViewController.iosMain_WARN]: Граф уже был запущен параллельно: ${e.message}")
    }
  }

  return ComposeUIViewController {
    YkisPamAppRoot(
      windowSize = windowSize,
      displayFeatures = emptyList(),
      initialChatId = initialChatId
    )
  }
}
