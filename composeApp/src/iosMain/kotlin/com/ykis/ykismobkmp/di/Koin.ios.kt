package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/**
 * [iosPlatformModule] — Граф нативных зависимостей для iPhone и Симуляторов.
 */
val iosPlatformModule: Module = module {
  // 1. Кроссплатформенный кэш настроек на базе нативного Apple NSUserDefaults
  single<Settings> {
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
  }
  single<AppSettingsRepository> {
    AppSettingsRepositoryImpl( get())
  }
  // 2. Драйвер SQLite баз данных под iOS
  single { DatabaseDriverFactory() }
}

/**
 * [initIosKoin] — Точка старта DI для Xcode.
 * Снабжена аннотацией, делающей метод глобально видимым в Swift слое.
 */
fun initIosKoin() {
  initKoin(
    platformModule = iosPlatformModule
  )
}
