package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.PreferencesSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences
import com.ykis.ykismobkmp.db.YkisDatabases
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.cash.sqlDelight.ApartmentDao
import com.ykis.ykismobkmp.cash.sqlDelight.LedgerDao
import com.ykis.ykismobkmp.cash.sqlDelight.MeterDao
import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.cash.apartment.ApartmentCacheImpl
import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCashImpl
import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCashImpl

/**
 * [desktopPlatformModule] — Граф нативных зависимостей для Mac Desktop.
 */
val desktopPlatformModule: Module = module {
  single<Settings> {
    PreferencesSettings(Preferences.userNodeForPackage(DatabaseDriverFactory::class.java))
  }
  single<AppSettingsRepository> { AppSettingsRepositoryImpl(get()) }
  single { DatabaseDriverFactory() }
}

/**
 * [databaseModule] — JVM-реализация СУБД.
 */
actual val databaseModule: Module = module {
  single { DatabaseDriverFactory() }
  
  single<YkisDatabases> {
    YkisDatabases(get<DatabaseDriverFactory>().createDriver())
  }

  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }

  single { ApartmentDao(get<YkisDatabasesQueries>()) }
  single { MeterDao(get<YkisDatabasesQueries>()) }
  single { LedgerDao(get<YkisDatabasesQueries>()) }

  single<ApartmentCache> { ApartmentCacheImpl(apartmentDao = get()) }
  single<MeterRepositoryCash> { MeterRepositoryCashImpl(meterDao = get()) }
  single<LedgerRepositoryCash> { LedgerRepositoryCashImpl(ledgerDao = get()) }
}

/**
 * [initDesktopKoin] — Точка запуска DI со стороны настольного приложения.
 */
fun initDesktopKoin() {
  initKoin(platformModule = desktopPlatformModule)
}
