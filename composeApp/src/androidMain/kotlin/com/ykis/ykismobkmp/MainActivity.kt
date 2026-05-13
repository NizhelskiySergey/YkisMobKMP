package com.ykis.ykismobkmp


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
  private val className = "MainActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Считываем chatId для DeepLink (Firebase KMP подхватит остальное)
    val startChatId = intent.getStringExtra("chatId")
    if (!startChatId.isNullOrEmpty()) {
      Log.i("YkisLog", "[$className.onCreate]: COLD_START ChatId: $startChatId")
    }

    setContent {
      // Передаем управление в общую часть
      App(initialChatId = startChatId)
    }
  }
}


@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
