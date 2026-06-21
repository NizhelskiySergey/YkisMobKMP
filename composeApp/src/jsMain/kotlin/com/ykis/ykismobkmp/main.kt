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
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import kotlinx.browser.window
import kotlinx.browser.localStorage
import org.koin.mp.KoinPlatform

/**
 * [main] — Пусковая точка входа JavaScript-движка для браузерной Web-версии ЮКИС.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun main() {
  initJsKoin()

  // 1. Встановлюємо мову документа на основі налаштувань (для коректної роботи ресурсів)
  val savedLang = localStorage.getItem("app_language") ?: "uk"
  document.documentElement?.setAttribute("lang", savedLang)

  // 2. Обробка переходу до чату при старті (з URL)
  fun handleUrlParams() {
    val urlParams = window.location.search
    if (urlParams.contains("chatId=")) {
      val chatId = urlParams.substringAfter("chatId=").substringBefore("&")
      println("[Main.js]: Знайдено chatId в URL: $chatId. Налаштування редиректу...")
      try {
          val chatModel: ChatScreenModel = KoinPlatform.getKoin().get()
          chatModel.setPendingPushChatId(chatId)
      } catch (e: Exception) { }
    }
  }

  handleUrlParams()
  // Слухаємо зміни історії (якщо SW оновить URL у відкритій вкладці)
  window.addEventListener("popstate", { handleUrlParams() })

  // 3. Обробка повідомлень, коли додаток ВІДКРИТИЙ (Foreground)
  (window.asDynamic()).onForegroundMessage = { payload: dynamic ->
      println("[Main.js]: Foreground push received")
      val data = payload.data
      val title = data?.title?.toString() ?: payload.notification?.title?.toString() ?: "ЮКІС"
      val body = data?.body?.toString() ?: payload.notification?.body?.toString() ?: "Нове повідомлення"
      val chatId = data?.chatId?.toString()
      
      SnackbarManager.showMessage("$title: $body")
      
      if (!chatId.isNullOrBlank()) {
          println("[Main.js]: Foreground chatId detected: $chatId")
      }
  }

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
