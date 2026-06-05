package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * [iosPlatformModule] — Граф нативных зависимостей для iPhone и Симуляторов.
 */
val iosPlatformModule: Module = module {
  single<Settings> {
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
  }
  single<AppSettingsRepository> {
    AppSettingsRepositoryImpl( get())
  }
  single { DatabaseDriverFactory() }
  
  // ИСПРАВЛЕНО: Добавляем локальный AI движок Apple Core ML
  single { LocalAiEngine() }
}

/**
 * [AppInitializer] — Точка входа для Swift.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("AppInitializer")
class AppInitializer {
    fun run() {
        // Инициализируем Napier для логов в Xcode
        Napier.base(DebugAntilog())

        initKoin(
            platformModule = iosPlatformModule
        )
    }
}
