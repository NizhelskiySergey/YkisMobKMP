package com.ykis.ykismobkmp


import com.ykis.ykismobkmp.di.initJsKoin
import androidx.compose.ui.window.CanvasBasedWindow

fun main() {
  // Инициализируем Koin со стороны JS перед отрисовкой UI
  initJsKoin()

  CanvasBasedWindow(title = "Ykis Web Admin") {
    App(initialChatId = null)
  }
}
