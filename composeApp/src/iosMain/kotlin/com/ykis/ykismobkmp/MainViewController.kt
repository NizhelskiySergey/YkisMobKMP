package com.ykis.ykismobkmp

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import com.ykis.ykismobkmp.di.initKoin

/**
 * [MainViewController] — Функция-мост для генерации нативного Apple UIViewController.
 * Вызывается в Xcode внутри Swift-файла App.swift / ContentView.swift.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun MainViewController(
  windowSize: WindowSizeClass,
  initialChatId: String?
): UIViewController = ComposeUIViewController {
  // Вызываем корневую точку сборки, передавая адаптивную геометрию окна и пуш-токен чата
  YkisPamAppRoot(
    windowSize = windowSize,
    displayFeatures = emptyList(), // На iOS особенности Fold API опускаются
    initialChatId = initialChatId
  )
}

/**
 * [doInitKoin] — Точка нативного старта графа DI для iOS.
 * ИСПРАВЛЕНО НАМЕРТВО: Вызывается в Swift на старте приложения, ликвидируя IllegalStateException!
 */
fun doInitKoin() {
  println("[Main.ios.kt]: Нативный запуск контейнера Koin DI для операционной системы iOS Auth")
  initKoin() // Запуск без специфических Android-контекстов
}
