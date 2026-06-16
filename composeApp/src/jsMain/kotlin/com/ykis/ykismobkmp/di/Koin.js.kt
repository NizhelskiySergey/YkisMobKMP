package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryJsImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
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
import kotlinx.browser.window
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
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.storage.FirebaseStorage

/**
 * [jsPlatformModule] — Нативний DI-граф для Web ЮКІС.
 */
val jsPlatformModule: Module = module {
  single<Settings> { StorageSettings() }
  single<AppSettingsRepository> { AppSettingsRepositoryJsImpl() }
  single { LocalAiEngine() }

  single<FirebaseApp> {
    println("[YkisLogKMP.Koin]: Ініціалізація Firebase App (Web)...")
    val options = FirebaseOptions(
        applicationId = "1:1062920014188:web:cd8ced095f943b9d088b49",
        apiKey = "AIzaSyDZUPgZSs5RMcQFtt2DjcoZoYTi9wdI37k",
        projectId = "ykis-mob",
        storageBucket = "ykis-mob.firebasestorage.app",
        databaseUrl = "https://ykis-mob-default-rtdb.europe-west1.firebasedatabase.app",
        authDomain = "ykis-mob.firebaseapp.com"
    )
    val app = Firebase.initialize(options = options)
    println("[YkisLogKMP.Koin]: Firebase успішно ініціалізовано.")
    if (RECAPTCHA_SITE_KEY != "ТВОЙ_КЛЮЧ_ЗДЕСЬ") {
        initializeRecaptcha(RECAPTCHA_SITE_KEY)
    }
    app
  }

  // Реєстрація Firebase під-сервісів для ChatRepository (Common)
  single<FirebaseFirestore> { Firebase.firestore(get<FirebaseApp>()) }
  single<FirebaseDatabase> { Firebase.database(get<FirebaseApp>()) }
  single<FirebaseStorage> { Firebase.storage(get<FirebaseApp>()) }

  single<FirebaseService> { 
    FirebaseServiceImpl(settings = get()) 
  }
}

/**
 * [databaseModule] — Web-реализация СУБД.
 */
actual val databaseModule: Module = module {
  // 1. Создаем драйвер
  single<SqlDriver> { DatabaseDriverFactory().createDriver() }
  
  // 2. Создаем базу данных
  single<YkisDatabases> {
    val driver = get<SqlDriver>()
    // В асинхронном режиме SQLDelight Web создание схемы делается один раз
    // Мы вызываем его, но не ждем здесь (DAO сами разрулят через suspend)
    YkisDatabases.Schema.create(driver) 
    YkisDatabases(driver)
  }

  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }

  // 3. Регистрируем DAO
  single { ApartmentDao(get<YkisDatabasesQueries>()) }
  single { MeterDao(get<YkisDatabasesQueries>()) }
  single { LedgerDao(get<YkisDatabasesQueries>()) }

  // 4. Регистрируем Cache
  single<ApartmentCache> { ApartmentCacheImpl(apartmentDao = get()) }
  single<MeterRepositoryCash> { MeterRepositoryCashImpl(meterDao = get()) }
  single<LedgerRepositoryCash> { LedgerRepositoryCashImpl(ledgerDao = get()) }
}

/**
 * [initializeRecaptcha] — Вызов JS-функции из index.html.
 */
fun initializeRecaptcha(key: String) {
    try {
        window.asDynamic().initializeFirebaseAppCheck(key)
    } catch (e: Exception) { }
}

/**
 * [initJsKoin] — Точка запуска DI-графа.
 */
fun initJsKoin() {
  initKoin(platformModule = jsPlatformModule)
}
