@file:JvmName("MainJvmKt") // Уникальное имя байт-кода для Mac Desktop JVM рантайма
package com.ykis.ykismobkmp

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass // КМР замерщик окон Material 3
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ykis.ykismobkmp.di.initKoin

/**
 * [main] — Главная пусковая точка входа Java-машины для десктопной платформы Mac Desktop (JVM) / Windows.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun main() = application {
  // 1. Аппаратно инициализируем Koin до создания нативного фрейма окна ОС
  initKoin()

  val windowState = rememberWindowState(
    size = DpSize(width = 1100.dp, height = 800.dp) // Комфортные стартовые габариты для Mac-админки ОСМД
  )

  Window(
    onCloseRequest = ::exitApplication,
    state = windowState,
    title = "ЮКІС Южне — Адміністрування та фінансовий хаб"
  ) {
    // ИСПРАВЛЕНО НАМЕРТВО: Функция calculateWindowSizeClass() на Десктопе вызывается БЕЗ АРГУМЕНТОВ!
    // Она сама нативно определит размеры окна Java-машины, ликвидируя ошибку "Too many arguments"
    val windowSizeClass = calculateWindowSizeClass()

    // Вызываем наше зафиксированное корневое ядро интерфейса ЮКИС
    YkisPamAppRoot(
      windowSize = windowSizeClass,
      displayFeatures = emptyList(),
      initialChatId = null // На десктопе глубокая пуш-навигация отсутствует
    )
  }
}
