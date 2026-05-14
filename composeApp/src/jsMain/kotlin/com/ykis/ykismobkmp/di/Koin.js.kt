package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [jsPlatformModule] — Нативный DI-граф для запуска приложения ЮКИС в браузере.
 * Инжектирует LocalStorage-кэш настроек.
 */
val jsPlatformModule: Module = module {
  // РЕШЕНИЕ: Пустой конструктор Settings() в jsMain автоматически развернет
  // работу с браузерным хранилищем LocalStorage для флага согласия лицензии.
  single<Settings> { StorageSettings() }

  // Заглушка драйвера базы данных для веб-версии.
  // Замени на актуальный Web-драйвер SQLDelight, если планируешь сохранять кэш в браузере.
  // single { DatabaseDriverFactory() }
}

/**
 * [initJsKoin] — Точка запуска DI-графа со стороны веб-платформы (из JS main()).
 * Передает наш браузерный модуль в универсальный параметр общего кода.
 */
fun initJsKoin() {
  initKoin(platformModule = jsPlatformModule)
}
