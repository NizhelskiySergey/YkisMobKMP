package com.ykis.ykismobkmp.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import org.koin.core.module.Module
import org.koin.dsl.module
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
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.storage.FirebaseStorage
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.FirebaseServiceImpl
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
 * [androidPlatformModule] — Специфічний платформенний Android-модуль.
 */
val androidPlatformModule: Module = module {
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

  single<Settings> { Settings() }
  single<AppSettingsRepository> { AppSettingsRepositoryImpl(settings = get()) }
  single { LocalAiEngine() }

  // Реєстрація Firebase під-сервісів для ChatRepository (Common)
  single<FirebaseFirestore> { Firebase.firestore }
  single<FirebaseDatabase> { Firebase.database }
  single<FirebaseStorage> { Firebase.storage }

  // Платформенна реєстрація сервісу авторизації
  single<FirebaseService> { FirebaseServiceImpl(settings = get()) }
}

/**
 * [databaseModule] — Android-реалізація СУБД.
 */
actual val databaseModule: Module = module {
  single<SqlDriver> { DatabaseDriverFactory(context = get()).createDriver() }
  
  single<YkisDatabases> {
    YkisDatabases(get<SqlDriver>())
  }

  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }

  // Явно вказуємо параметри для уникнення помилок компіляції
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
 * [initAndroidKoin] — Точка пуску DI со стороны Android.
 */
fun initAndroidKoin(context: Context) {
  initKoin(
    platformModule = module {
      single<Context> { context }
      includes(androidPlatformModule)
    }
  )
}
