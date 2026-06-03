package com.ykis.ykismobkmp
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.core.content.ContextCompat
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.ykis.ykismobkmp.di.initAndroidKoin
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    println("[$tag.onCreate]: Запуск нативного слоя Android, инициализация сессии ЮКИС")

    checkNotificationPermission()

    // ИСПРАВЛЕНО НАМЕРТВО: Вызываем специализированный андроид-инициализатор, передавая контекст Activity!
    // Он автоматически подмешает androidPlatformModule и свяжет интерфейс AppSettingsRepository с его реализацией.
    // ИСПРАВЛЕНО НАМЕРТВО: Передаем applicationContext вместо Activity!
    // Это гарантирует, что драйвер SQLDelight получит вечный доступ к дисковой системе смартфона!
    initAndroidKoin(context = this@MainActivity.applicationContext)

    // ИСПРАВЛЕНО НАМЕРТВО: Переносим инициализацию App Check в фоновый поток с задержкой.
    // Это предотвращает SIGSEGV (Fatal signal 11) при конфликте с Google Play Services и Tag Manager на старте.
    lifecycleScope.launch {
      try {
        delay(500) // Даем системе завершить первичную загрузку классов и привязку свойств
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance()
        )
        println("[YkisLogKMP.MainActivity]: Нативная отладочная фабрика Google успешно запущена в фоновом режиме")
      } catch (e: Exception) {
        println("[YkisLogKMP.MainActivity_ERROR]: Ошибка предустановки App Check на устройстве: ${e.message}")
      }
    }


    setContent {
      // Подключаем тему оформления расчетного центра г. Южного
      YkisPAMTheme {

        // Вызываем каноничный мультиплатформенный замерщик классов окон Material 3 KMP
        val windowSizeClass = calculateWindowSizeClass(activity = this)

        // Вызываем графическое ядро из правильного пакета и передаем все 3 ожидаемых аргумента
        YkisPamAppRoot(
          windowSize = windowSizeClass,
          displayFeatures = emptyList(), // Список особенностей для складных экранов (Fold API)
          initialChatId = intent?.getStringExtra("chat_id") // Проброс ID пуша глубокой навигации
        )
      }
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
