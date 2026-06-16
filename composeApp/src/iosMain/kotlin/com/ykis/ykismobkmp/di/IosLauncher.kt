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

/**
 * [iosPlatformModule] — Граф нативних залежностей для iOS.
 */
val iosPlatformModule: Module = module {
  single<Settings> {
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
  }
  single<AppSettingsRepository> {
    AppSettingsRepositoryImpl(get())
  }
  single { LocalAiEngine() }

  // Реєстрація Firebase під-сервісів для ChatRepository (Common)
  single<FirebaseFirestore> { Firebase.firestore }
  single<FirebaseDatabase> { Firebase.database }
  single<FirebaseStorage> { Firebase.storage }
}

/**
 * [databaseModule] — iOS-реалізація СУБД.
 */
actual val databaseModule: Module = module {
  single<SqlDriver> { DatabaseDriverFactory().createDriver() }
  
  single<YkisDatabases> {
    YkisDatabases(get<SqlDriver>())
  }

  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }

  // ИСПРАВЛЕНО: Передаем 3 параметра для соответствия новому конструктору
  single { ApartmentDao(get(), get(), get()) }
  single { MeterDao(get(), get(), get()) }
  single { LedgerDao(get(), get(), get()) }

  single<ApartmentCache> { ApartmentCacheImpl(apartmentDao = get()) }
  single<MeterRepositoryCash> { MeterRepositoryCashImpl(meterDao = get()) }
  single<LedgerRepositoryCash> { LedgerRepositoryCashImpl(ledgerDao = get()) }
}

/**
 * [AppInitializer] — Точка входу для Swift.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("AppInitializer")
class AppInitializer {
    fun run() {
        Napier.base(DebugAntilog())
        initKoin(platformModule = iosPlatformModule)
    }
}
