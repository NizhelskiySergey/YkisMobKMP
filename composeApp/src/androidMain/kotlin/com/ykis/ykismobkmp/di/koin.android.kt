package com.ykis.ykismobkmp.di

import android.content.Context
import androidx.preference.PreferenceManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [androidPlatformModule] — Основной нативный граф для Android.
 */
val androidPlatformModule: Module = module {
  // Кроссплатформенный кэш настроек на базе SharedPreferences.
  // Зависимость Context подтянется автоматически через get()
  single<Settings> {
    SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(get()))
  }

  // Создание Android-драйвера для базы данных SQLDelight 2.x
  single { DatabaseDriverFactory(get()) }

  // Твоя нативная логика AndroidAiManager / `LogService.android`, если они требуют get()
}

/**
 * [initAndroidKoin] — Точка запуска DI со стороны Android Application.
 * РЕШЕНИЕ: Регистрируем Context напрямую через single { context }, убирая ошибку 'None of the following candidates'.
 */
fun initAndroidKoin(context: Context) {
  initKoin(
    platformModule = module {
      // 1. Внедряем чистый Context как синглтон в граф Koin.
      // Это полностью заменяет проблемный метод androidContext(context)
      single<Context> { context }

      // 2. Включаем наш основной платформенный модуль Android
      includes(androidPlatformModule)
    }
  )
}
