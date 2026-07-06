package com.ykis.ykismobkmp.di
import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.cash.apartment.ApartmentCacheImpl
import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCashImpl
import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCashImpl
import com.ykis.ykismobkmp.cash.sqlDelight.ApartmentDao
import com.ykis.ykismobkmp.cash.sqlDelight.LedgerDao
import com.ykis.ykismobkmp.cash.sqlDelight.MeterDao
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.remote.apartment.ApartmentRemote
import com.ykis.ykismobkmp.data.remote.apartment.ApartmentRemoteImpl
import com.ykis.ykismobkmp.data.remote.ledger.LedgerRemoteRepository
import com.ykis.ykismobkmp.data.remote.ledger.LedgerRemoteRepositoryImpl
import com.ykis.ykismobkmp.data.remote.meter.MeterRemoteRepository
import com.ykis.ykismobkmp.data.remote.meter.MeterRemoteRepositoryImpl
import com.ykis.ykismobkmp.db.YkisDatabases
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.db.DatabaseSchemaInitializer
import com.ykis.ykismobkmp.domain.ai.CloudAiEngine
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.ai.GeminiCloudProvider
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepository
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepositoryImpl
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.FirebaseServiceImpl
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.MailService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform


private const val KOIN_TAG = "YkisLogKMP.Koin"

/**
 * [commonModule] — Инфраструктурный модуль.
 */
val commonModule = module {
  single {
    HttpClient {
      install(ContentNegotiation) {
        json(Json { 
          ignoreUnknownKeys = true 
          isLenient = true 
          encodeDefaults = true 
          prettyPrint = true
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

  // Firebase Services (Базові інстанси для Koin)
  single { Firebase.firestore }
  single { Firebase.database }
  single { Firebase.storage }
  single { Firebase.auth }

  single { KtorApiService(get()) }
  single { SnackbarManager }
  single { LogService() }
  single { MailService(getOrNull()) }

  // Ініціалізація новітнього Firebase AI Logic (Гібридна архітектура 2026)
  single { CloudAiEngine(apiKey = GEMINI_API_KEY) }
  single<GeminiAiManager> { GeminiCloudProvider(localEngine = get(), cloudEngine = get()) }

  // Кроссплатформенное создание ChatRepository (GitLive)
  single {
    ChatRepository(
      _firestore = getOrNull<FirebaseFirestore>(),
      _realtime = getOrNull<FirebaseDatabase>(),
      _storage = getOrNull<FirebaseStorage>(),
      aiManager = get()
    )
  }

  // РЕШЕНИЕ: FirebaseService реєструється ТІЛЬКИ в платформенних модулях (actual)
  // для коректної ініціалізації специфічних Auth-провайдерів на кожній ОС.

  // Репозитории
  single<ApartmentRemote> { ApartmentRemoteImpl(ktorApiService = get()) }
  single<ApartmentRepository> { ApartmentRepositoryImpl(remote = get()) }
  single<MeterRemoteRepository> { MeterRemoteRepositoryImpl(ktorApiService = get()) }
  single<MeterRepository> { MeterRepositoryImpl( get()) }
  single<LedgerRemoteRepository> { LedgerRemoteRepositoryImpl(ktorApiService = get()) }
  single<LedgerRepository> { LedgerRepositoryImpl(get()) }
}

/**
 * [databaseModule] — Ожидаемый модуль СУБД для платформенной реализации.
 */
expect val databaseModule: Module

/**
 * [databaseCommonModule] — Общие компоненты БД для всех платформ.
 */
val databaseCommonModule = module {
    single<DatabaseSchemaInitializer> { DatabaseSchemaInitializer() }
}

fun initKoin(platformModule: Module = module {}, appDeclaration: KoinAppDeclaration = {}) {
  if (KoinPlatform.getKoinOrNull() != null) return
  startKoin {
    appDeclaration()
    modules(listOf(commonModule, databaseCommonModule, databaseModule, domainModule, navigationModule, platformModule))
  }
}
