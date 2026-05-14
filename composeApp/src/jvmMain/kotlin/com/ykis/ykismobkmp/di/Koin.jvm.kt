package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.PreferencesSettings
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences
import kotlin.jvm.java

/**
 * [desktopPlatformModule] — Граф нативных зависимостей для Mac Desktop.
 */
val desktopPlatformModule: Module = module {
  // 1. Кроссплатформенный кэш настроек на базе Java Preferences API
  single<Settings> {
    PreferencesSettings(Preferences.userNodeForPackage(DatabaseDriverFactory::class.java))
  }

  // 2. Драйвер SQLite для SQLDelight 2.x на ПК
  single { DatabaseDriverFactory() }
}

/**
 * [initDesktopKoin] — Точка запуска DI со стороны настольного приложения.
 * Вызывается первой строкой внутри функции main() перед инициализацией Window.
 */
fun initDesktopKoin() {
  initKoin(
    platformModule = desktopPlatformModule
  )
}
