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
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.ai.GeminiCloudProvider
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
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
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
 * [commonModule] — Инфраструктурный модуль: Сеть (Ktor), Сервисы, Firebase, ИИ и Системные репозитории.
 */
val commonModule = module {
  single {
    HttpClient {
      install(ContentNegotiation) {
        json(
          json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
          },
          contentType = io.ktor.http.ContentType.Application.Json
        )
      }
      install(Logging) {
        // Теперь абсолютно каждый запрос и HTML/JSON ответ бэкенда Южного получит наш сквозной маркер!
        logger = object : Logger {
          override fun log(message: String) {
            // Пропускаем через println, что гарантирует вывод в общий поток Logcat без обрезки тегов
            println("[YkisLogKMP.Network]: $message")
          }
        }
        level = LogLevel.ALL
      }

      install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
      }
    }
  }

  single { KtorApiService(get()) }
  single { SnackbarManager }
  single { Firebase.firestore }
  single { Firebase.database }
  single { Firebase.storage }
  single { Firebase.functions }
  single { LogService() }
  single<com.russhwolf.settings.Settings> { com.russhwolf.settings.Settings() }

  // Инициализация Generative AI (Gemini) для умного ассистента ЮКИС г. Южный
  single {
    dev.shreyaspatil.ai.client.generativeai.GenerativeModel(
      modelName = "gemini-pro",
      apiKey = GEMINI_API_KEY
    )
  }
  single<GeminiAiManager> { GeminiCloudProvider(model = get(), localEngine = get()) }
  single {
    ChatRepository(
      firestore = get(),
      realtime = get(),
      storage = get(),
      functions = get(),
      aiManager = get()
    )
  }

  // Каноничный прокси-сервис аутентификации Firebase
  single<FirebaseService> {
    val realService = FirebaseServiceImpl(
      settings = get(),
      apartmentService = get(),
      chatRepo = get()
    )

    //  КМР-ПРОКСИ СЕРВИСА GOOGLE FIREBASE
    object : FirebaseService by realService {

      // 1. Каскадное глушение Snapshot-соединений в ОЗУ смартфона
      override fun stopAllListeners() {
        try {
          realService.stopAllListeners()
        } catch (e: Exception) {
          println("[$KOIN_TAG.FirebaseProxy_ERR]: Сбой при остановке слушателей: ${e.message}")
        }
      }

      //  прокси-объект нативно перехватывает наш suspend вызов signOut(),
      // аннулирует Auth сессию в Keystore и стирает UID, полностью вычищая рантайм!
      override suspend fun signOut() {
        try {
          println("[$KOIN_TAG.FirebaseProxy]: Перехват вызова безопасного логаута. Перенаправление в realService...")
          realService.signOut()
        } catch (e: Exception) {
          println("[$KOIN_TAG.FirebaseProxy_ERR]: Каскадный сбой при закрытии Auth сессии Firebase: ${e.message}")
        }
      }

      // 3. Каскадное безвозвратное удаление коммунального профиля из баз данных
      // Внутри анонимного прокси object : FirebaseService by realService в Koin:
      override suspend fun revokeAccess(): Resource<Boolean> {
        return try {
          println("[$KOIN_TAG.FirebaseProxy]: Перехват вызова безвозвратного удаления профиля ЮКИС...")
          realService.revokeAccess()
        } catch (e: Exception) {
          println("[$KOIN_TAG.FirebaseProxy_ERR]: Каскадный сбой при удалении аккаунта: ${e.message}")
          Resource.Error(message = e.message)
        }
      }
    }
    }


  // ====================================================================
  // --- ИСПРАВЛЕНО НАМЕРТВО: ПОЛНАЯ СЕТЕВАЯ СВЯЗКА РЕПОЗИТОРИЕВ ЮКИС ---
  // ====================================================================

  // Пакет 1. Недвижимость (Apartment)
  single<ApartmentRemote> { ApartmentRemoteImpl(ktorApiService = get()) }
  single<ApartmentRepository> { ApartmentRepositoryImpl(remote = get()) }

  // Пакет 2. Приборы учета (Meter)
  single<MeterRemoteRepository> { MeterRemoteRepositoryImpl(ktorApiService = get()) }
  single<MeterRepository> { MeterRepositoryImpl( get()) }

  // Пакет 3. Биллинг и бухгалтерские начисления (Ledger)
  single<LedgerRemoteRepository> { LedgerRemoteRepositoryImpl(ktorApiService = get()) }
  single<LedgerRepository> { LedgerRepositoryImpl(get()) }
}

/**
 * [databaseModule] — Инициализация СУБД SQLDelight 2.x, объектов доступа к данным (DAO) и КМР-кэша.
 */
val databaseModule = module {
  single<YkisDatabases> {
    val driverFactory = get<com.ykis.ykismobkmp.db.DatabaseDriverFactory>()
    YkisDatabases(driverFactory.createDriver())
  }

  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }

  // Регистрируем все очищенные от Room-зависимостей объекты DAO
  single { ApartmentDao(dbQueries = get()) }
  single { MeterDao(dbQueries = get()) }
  single { LedgerDao(dbQueries = get()) }

  // Снабжаем рантайм КМР-реализациями локального кэширования диска устройства
  single<ApartmentCache> { ApartmentCacheImpl(apartmentDao = get()) }
  single<MeterRepositoryCash> { MeterRepositoryCashImpl(meterDao = get()) }
  single<LedgerRepositoryCash> { LedgerRepositoryCashImpl(ledgerDao = get()) }
}

/**
 * [navigationModule] — Презентационный слой: ScreenModels для Voyager и Сервисы-Комбайны.
 */
/**
 * Главная точка старта и инициализации Koin-контекста для всех платформ.
 * ИСПРАВЛЕНО: Добавлен жесткий защитный предохранитель от повторного запуска Koin
 * при переворотах экрана планшета (устраняет KoinApplicationAlreadyStartedException).
 */
fun initKoin(
  platformModule: Module = module {},
  appDeclaration: KoinAppDeclaration = {}
) {
  // КМР-ПРЕДОХРАНИТЕЛЬ: Если платформа выдает готовый Koin, значит приложение просто перевернулось
  if (KoinPlatform.getKoinOrNull() != null) {
    println("[YkisLogKMP.Koin]: Граф DI вже запущенний на платформі. Пропуск повторного старту при повороті.")
    return
  }

  startKoin {
    appDeclaration()
    modules(
      listOf(
        commonModule,
        databaseModule,
        domainModule,
        navigationModule,
        platformModule
      )
    )
  }
}



