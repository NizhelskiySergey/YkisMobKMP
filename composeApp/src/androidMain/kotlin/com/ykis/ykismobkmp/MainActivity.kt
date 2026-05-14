package com.ykis.ykismobkmp


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
  private val className = "MainActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    // Инициализация нативного Android Splash Screen перед super.onCreate
    installSplashScreen()
    super.onCreate(savedInstanceState)

    // Включение отображения контента под системными барами (Edge-to-Edge)
    enableEdgeToEdge()

    // Считываем chatId из интента для обработки DeepLink (холодный старт)
    val startChatId = intent.getStringExtra("chatId")
    if (!startChatId.isNullOrEmpty()) {
      // ИСПРАВЛЕНО: Платформенный Log.i заменен на универсальный println() для чистоты кодстайла
      println("[$className.onCreate]: COLD_START ChatId получен: $startChatId")
    }

    setContent {
      // Передаем управление и интент в общий кроссплатформенный UI холст
      App(initialChatId = startChatId)
    }
  }
}



@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
