package com.ykis.ykismobkmp

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import com.ykis.ykismobkmp.di.initJsKoin
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import kotlinx.browser.window
import kotlinx.browser.localStorage
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun main() {
  initJsKoin()

  // 1. Читаємо мову та встановлюємо атрибут lang для HTML (критично для ресурсів)
  val savedLang = localStorage.getItem("app_language") ?: "uk"
  println("[Main.js]: Считана локаль: $savedLang. Установка атрибута lang...")
  document.documentElement?.setAttribute("lang", savedLang)

  fun handleUrlParams() {
    val urlParams = window.location.search
    if (urlParams.contains("chatId=")) {
      val chatId = urlParams.substringAfter("chatId=").substringBefore("&")
      try {
          val chatModel: ChatScreenModel = KoinPlatform.getKoin().get()
          chatModel.setPendingPushChatId(chatId)
      } catch (e: Exception) { }
    }
  }

  handleUrlParams()
  window.addEventListener("popstate", { handleUrlParams() })

  (window.asDynamic()).onForegroundMessage = { payload: dynamic ->
      val data = payload.data
      val title = data?.title?.toString() ?: payload.notification?.title?.toString() ?: "ЮКІС"
      val body = data?.body?.toString() ?: payload.notification?.body?.toString() ?: "Нове повідомлення"
      SnackbarManager.showMessage("$title: $body")
  }

  val htmlBodyElement = document.getElementById("ComposeTarget") ?: document.body

  if (htmlBodyElement != null) {
    ComposeViewport(viewportContainer = htmlBodyElement) {
      val webWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1024.dp, 768.dp))
      YkisPamAppRoot(
        windowSize = webWindowSizeClass,
        displayFeatures = emptyList()
      )
    }
  }
}
