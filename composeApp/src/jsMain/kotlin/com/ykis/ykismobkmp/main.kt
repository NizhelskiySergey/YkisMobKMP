package com.ykis.ykismobkmp

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport // ИСПРАВЛЕНО: Новый легитимный КМР-контейнер JetBrains
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.browser.document // Прямой доступ к DOM-дереву браузера
import org.khronos.webgl.WebGLRenderingContext
import com.ykis.ykismobkmp.di.initJsKoin

/**
 * [main] — Пусковая точка входа JavaScript-движка для браузерной Web-версии ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Устаревший депрекейт-вызов CanvasBasedWindow заменен на каноничный ComposeViewport API!
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun main() {
  // 1. Аппаратно инициализируем Koin через специализированный Web-инициализатор
  initJsKoin()

  // Находим HTML-элемент холста на веб-странице твоегоindex.html (например, <body id="ykis-app-body">)
  // РЕШЕНИЕ: Нативно передаем DOM-элемент напрямую внутрь ComposeViewport API
  val htmlBodyElement = document.getElementById("ComposeTarget") ?: document.body

  if (htmlBodyElement != null) {
    ComposeViewport(viewportContainer = htmlBodyElement) {

      // Вычисляем адаптивный класс геометрии окна для браузерного Web-интерфейса
      val webWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1024.dp, 768.dp))

      // Вызываем наше зафиксированное корневое ядро интерфейса ЮКИС г. Южный
      YkisPamAppRoot(
        windowSize = webWindowSizeClass,
        displayFeatures = emptyList()
      )
    }
  } else {
    println("[Main.js.kt]: [FATAL] Не вдалося знайти базовий HTML-елемент для монтування ComposeViewport холста!")
  }
}
