package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryJsImpl
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.FirebaseServiceImpl
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import org.koin.core.module.Module
import org.koin.dsl.module
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
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
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.storage.FirebaseStorage
import com.ykis.ykismobkmp.db.DatabaseSchemaInitializer
import kotlinx.browser.window

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val jsPlatformModule: Module = module {
  single {
    HttpClient(Js) {
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

  single<Settings> { StorageSettings() }
  single<AppSettingsRepository> { AppSettingsRepositoryJsImpl() }
  single { LocalAiEngine() }

  single<FirebaseApp> {
    val options = FirebaseOptions(
        applicationId = "1:1062920014188:web:cd8ced095f943b9d088b49",
        apiKey = "AIzaSyD5ukrhK6g6xKlrn4Iv9zPQxB7ji_gACY4",
        projectId = "ykis-mob",
        storageBucket = "ykis-mob.firebasestorage.app",
        databaseUrl = "https://ykis-mob-default-rtdb.europe-west1.firebasedatabase.app",
        authDomain = "ykis-mob.firebaseapp.com"
    )
    val app = Firebase.initialize(options = options)
    
    // ІНІЦІАЛІЗАЦІЯ APP CHECK ДЛЯ WEB (ЯК У ХОРОШІЙ ВЕТЦІ)
    try {
        window.asDynamic().initializeFirebaseAppCheck(RECAPTCHA_SITE_KEY_WEB)
        println("[YkisLogKMP.Koin]: App Check для Web активовано успішно.")
    } catch (e: Exception) {
        println("[YkisLogKMP.Koin_ERROR]: Не вдалося ініціалізувати App Check: \${e.message}")
    }
    
    app
  }

  single<FirebaseFirestore> { Firebase.firestore(get<FirebaseApp>()) }
  single<FirebaseDatabase> { Firebase.database(get<FirebaseApp>()) }
  single<FirebaseStorage> { Firebase.storage(get<FirebaseApp>()) }
  single<FirebaseService> { FirebaseServiceImpl(get()) }
}

/**
 * [databaseModule] — Web-реализация СУБД.
 * УНІФІКОВАНО: Повертаємо безпечний драйвер-заглушку, який повертає порожні списки замість null.
 */
actual val databaseModule: Module = module {
  single<SqlDriver> { 
    object : SqlDriver {
        @Suppress("UNCHECKED_CAST")
        override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<R> {
            return QueryResult.Value(emptyList<Any>()) as QueryResult<R>
        }
        override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> = QueryResult.Value(0L)
        override fun newTransaction(): QueryResult<Transacter.Transaction> = throw IllegalStateException("DB Disabled")
        override fun currentTransaction(): Transacter.Transaction? = null
        override fun close() {}
        override fun addListener(vararg queryKeys: String, listener: Query.Listener) {}
        override fun notifyListeners(vararg queryKeys: String) {}
        override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {}
    }
  }
  
  single<DatabaseSchemaInitializer> { DatabaseSchemaInitializer() }
  single<YkisDatabases> { YkisDatabases(get<SqlDriver>()) }
  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }
  single { ApartmentDao(get(), get(), get()) }
  single { MeterDao(get(), get(), get()) }
  single { LedgerDao(get(), get(), get()) }
  single<ApartmentCache> { ApartmentCacheImpl(get()) }
  single<MeterRepositoryCash> { MeterRepositoryCashImpl(get()) }
  single<LedgerRepositoryCash> { LedgerRepositoryCashImpl(get()) }
}

fun initJsKoin() {
  initKoin(platformModule = jsPlatformModule)
}
