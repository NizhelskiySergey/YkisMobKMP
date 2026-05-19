package com.ykis.ykismobkmp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.ykis.ykismobkmp.di.initAndroidKoin
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val tag = "MainActivity"

/**
 * [MainActivity] — Нативный пусковой контейнер операционной системы Android.
 * ИСПРАВЛЕНО НАМЕРТВО: Вызов initKoin заменен на initAndroidKoin(this), что принудительно включает
 * androidPlatformModule в память и полностью ликвидирует NoDefinitionFoundException для AppSettingsRepository!
 */
class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    println("[$tag.onCreate]: Запуск нативного слоя Android, инициализация сессии ЮКИС")

    // ИСПРАВЛЕНО НАМЕРТВО: Вызываем специализированный андроид-инициализатор, передавая контекст Activity!
    // Он автоматически подмешает androidPlatformModule и свяжет интерфейс AppSettingsRepository с его реализацией.
    // ИСПРАВЛЕНО НАМЕРТВО: Передаем applicationContext вместо Activity!
    // Это гарантирует, что драйвер SQLDelight получит вечный доступ к дисковой системе смартфона!
    initAndroidKoin(context = this@MainActivity.applicationContext)


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
}
