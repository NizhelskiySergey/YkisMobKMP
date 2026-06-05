package com.ykis.ykismobkmp
import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ykis.ykismobkmp.di.initAndroidKoin
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val tag = "MainActivity"

/**
 * [MainActivity] — Нативный пусковой контейнер операционной системы Android.
 * ИСПРАВЛЕНО НАМЕРТВО: Вызов initKoin заменен на initAndroidKoin(this), что принудительно включает
 * androidPlatformModule в память и полностью ликвидирует NoDefinitionFoundException для AppSettingsRepository!
 */
class MainActivity : ComponentActivity() {

  // Регистрация запроса разрешений для Android 13+ (Push уведомления)
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      println("[YkisLogKMP.MainActivity]: Разрешение на уведомления получено")
    } else {
      println("[YkisLogKMP.MainActivity]: Разрешение на уведомления отклонено")
    }
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    println("[$tag.onCreate]: Запуск MainActivity")

    checkNotificationPermission()
    
    // ИСПРАВЛЕНО: Koin уже инициализирован в YkisApp.kt
    // Здесь только обрабатываем диплинк
    handleDeepLink(intent)

    // 3. ОТКЛЮЧЕНО: App Check и прочие тяжелые нативные модули Google
    // Это гарантированно уберет конфликт в SystemProperties

    setContent {
      YkisPAMTheme {
        // Замеряем окно без try-catch (Compose сам обработает это в рантайме)
        val windowSizeClass = calculateWindowSizeClass(activity = this)

        YkisPamAppRoot(
          windowSize = windowSizeClass,
          displayFeatures = emptyList()
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    println("[YkisLogKMP.MainActivity]: Получен новый Intent (Hot Push)")
    handleDeepLink(intent)
  }

  private fun handleDeepLink(intent: Intent?) {
    val extras = intent?.extras
    println("[YkisLogKMP.TRAP.Main]: --- ДЕТАЛЬНЫЙ АНАЛИЗ INTENT ---")
    println("[YkisLogKMP.TRAP.Main]: Action: ${intent?.action}")
    
    if (extras != null) {
      for (key in extras.keySet()) {
        println("[YkisLogKMP.TRAP.Main]: Ключ: \"$key\" -> Значение: \"${extras.getString(key)}\"")
      }
    } else {
      println("[YkisLogKMP.TRAP.Main]: Extras пустые")
    }

    // Ищем ID по всем возможным вариантам написания
    val chatId = extras?.getString("chat_id") 
      ?: extras?.getString("chatId") 
      ?: extras?.getString("gcm.notification.chatId")
      ?: extras?.getString("id")

    if (!chatId.isNullOrEmpty()) {
      println("[YkisLogKMP.TRAP.Main]: [FOUND] Идентификатор чата пойман: \"$chatId\"")
      
      lifecycleScope.launch {
        delay(500) 
        val chatModel: ChatScreenModel by inject()
        chatModel.setPendingPushChatId(chatId)
      }
    } else {
      println("[YkisLogKMP.TRAP.Main]: [NOT_FOUND] chat_id не найден в этом интенте.")
    }
  }

  /**
   * [checkNotificationPermission] — Проверка и запрос разрешений на Push-уведомления для Android 13+.
   */
  private fun checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
      ) {
        println("[YkisLogKMP.MainActivity]: Запрос разрешения на POST_NOTIFICATIONS")
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }
}
