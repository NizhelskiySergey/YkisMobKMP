package com.ykis.ykismobkmp.di
import android.content.Context
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import org.koin.core.module.Module
import org.koin.dsl.module
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
 * [androidPlatformModule] — Специфический платформенный Android-модуль инжекции контекстов ОС.
 */
val androidPlatformModule: Module = module {
  single<AppSettingsRepository> { AppSettingsRepositoryImpl(settings = get()) }
  single { LocalAiEngine() }
}

/**
 * [databaseModule] — Android-реализация СУБД.
 */
actual val databaseModule: Module = module {
  single { DatabaseDriverFactory(context = get()) }
  
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
 * [initAndroidKoin] — Точка нативного пускового старта DI-графа на уровне MainActivity.kt.
 */
fun initAndroidKoin(context: Context) {
  initKoin(
    platformModule = module {
      single<Context> { context }
      includes(androidPlatformModule)
    }
  )
}
