package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.PreferencesSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences
import app.cash.sqldelight.db.SqlDriver
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
import com.ykis.ykismobkmp.db.DatabaseSchemaInitializer

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * [desktopPlatformModule] — Граф нативных зависимостей для Mac Desktop.
 */
val desktopPlatformModule: Module = module {
  single {
    HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(Json { 
          ignoreUnknownKeys = true 
          isLenient = true 
          encodeDefaults = true 
          coerceInputValues = true
          allowSpecialFloatingPointValues = true
        })
      }
      install(Logging) {
        logger = object : Logger { override fun log(message: String) { println("[YkisLogKMP.Network]: $message") } }
        level = LogLevel.ALL
      }
      install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 30_000 }
    }
  }

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
  single<SqlDriver> { DatabaseDriverFactory().createDriver() }
  
  single<YkisDatabases> {
    YkisDatabases(get<SqlDriver>())
  }

  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }

  // Явно вказуємо параметри для уникнення помилок конструктора
  single { 
    ApartmentDao(
        dbQueries = get<YkisDatabasesQueries>(),
        driver = get<SqlDriver>(),
        schemaInitializer = get<DatabaseSchemaInitializer>()
    ) 
  }
  single { 
    MeterDao(
        dbQueries = get<YkisDatabasesQueries>(),
        driver = get<SqlDriver>(),
        schemaInitializer = get<DatabaseSchemaInitializer>()
    ) 
  }
  single { 
    LedgerDao(
        dbQueries = get<YkisDatabasesQueries>(),
        driver = get<SqlDriver>(),
        schemaInitializer = get<DatabaseSchemaInitializer>()
    ) 
  }

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
