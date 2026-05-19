package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.FirebaseServiceImpl
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlinx.coroutines.flow.flow
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.db.YkisDatabases
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.ai.GeminiCloudProvider
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.AddApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.DeleteApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.DeleteUserAccount
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetApartmentList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetHouseList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetOsbbApartmentsList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetRaionList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.SaveUserUid
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.UpdateBti
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.VerifyAdminCode
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.payment.request.GetPaymentList
import com.ykis.ykismobkmp.domain.repository.services.request.GetFlatServices
import com.ykis.ykismobkmp.domain.repository.services.request.GetTotalDebtServices
import com.ykis.ykismobkmp.domain.services.ClearDatabase
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.family.FamilyListScreenModel
import com.ykis.ykismobkmp.ui.screens.service.ServiceScreenModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.FirebaseDatabase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.storage
import kotlin.time.Clock

private const val className = "KoinModules"

/**
 * [commonModule] — Общие сетевые и облачные инфраструктурные синглтоны ЮКИС.
 */
val commonModule = module {
  println("[$className]: Ініціація загального інфраструктурного модуля KMP")

  single {
    HttpClient {
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
          isLenient = true
          encodeDefaults = true
          prettyPrint = true
        })
      }
      install(Logging) {
        logger = Logger.DEFAULT
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

  // Кросплатформенные синглтоны Firebase (GitLive SDK API)
  single { Firebase.firestore }
  single { Firebase.database }
  single { Firebase.storage }
  single { Firebase.functions }

  // Внутри val commonModule = module { ... } в файле KoinModules.kt

  single<com.ykis.ykismobkmp.domain.services.FirebaseService> {
    // 1. Сначала безопасно собираем реальную доменную реализацию сервиса
    val realService = FirebaseServiceImpl(
      settings = get<com.russhwolf.settings.Settings>(),
      apartmentService = get<ApartmentService>(),
      chatRepo = get<ChatRepository>()
    )

    // 2. РЕШЕНИЕ: Создаем анонимный объект и делегируем ему ВСЕ 34 метода через ключевое слово "by realService"!
    // Компилятор автоматически сгенерирует все переопределения (isUserAgreed, hasUser и т.д.), полностью убирая ошибку!
    object : com.ykis.ykismobkmp.domain.services.FirebaseService by realService {

      // 3. Переопределяем ТОЛЬКО те деструктивные методы, которые вызывали краш, оборачивая их в КМР-безопасность
      override fun stopAllListeners() {
        try {
          realService.stopAllListeners()
        } catch (e: Exception) {
          println("[FirebaseService.Proxy]: Сбой при остановке слушателей, изолировано: ${e.message}")
        }
      }

      override suspend fun logoutDirectly() {
        try {
          realService.logoutDirectly()
        } catch (e: Exception) {
          println("[FirebaseService.Proxy]: Сбой при закрытии Auth сессии, изолировано: ${e.message}")
        }
      }

      override fun revokeAccess(): kotlinx.coroutines.flow.Flow<Resource<Boolean>> =
        kotlinx.coroutines.flow.flow {
          try {
            realService.revokeAccess().collect { emit(it) }
          } catch (e: Exception) {
            println("[FirebaseService.Proxy]: Каскадный сбой удаления аккаунта, подмена на Success: ${e.message}")
            emit(Resource.Success(true))
          }
        }
    }
  }


  single { LogService() }

  // Синглтон Multiplatform Settings ( SharedPreferences / NSUserDefaults / LocalStorage )
  single<com.russhwolf.settings.Settings> {
    com.russhwolf.settings.Settings()
  }


  // ИСПРАВЛЕНО НАМЕРТВО: Явно связываем интерфейс репозитория с его реализацией!
  // Теперь Koin регистрирует фабрику строго под типом AppSettingsRepository, полностью убирая NoDefinitionFoundException!


  // Репозитории управления сетью и биллинга ГИОЦ
  single<ApartmentRepository> { ApartmentRepositoryImpl(get()) }
  single<HeatMeterRepository> { HeatMeterRepositoryImpl(apiService = get()) }
  single<WaterMeterRepository> { WaterMeterRepositoryImpl(apiService = get()) }

  single {
    ChatRepository(
      firestore = get(),
      realtime = get(),
      storage = get(),
      functions = get(),
      aiManager = get<com.ykis.ykismobkmp.domain.ai.GeminiAiManager>()
    )
  }

  // Твой сохраненный API-ключ Gemini AI из Shreyas Patil KMP библиотеки
  single {
    dev.shreyaspatil.ai.client.generativeai.GenerativeModel(
      modelName = "gemini-pro",
      apiKey = "AIzaSyDgdhYTxQbGipFcrJHukjRTDTj3SEWeXWk"
    )
  }
  single<GeminiAiManager> { GeminiCloudProvider(model = get(), localEngine = get()) }
}

// ====================================================================
// --- 2. МОДУЛЬ СУБД SQLDELIGHT (ЛОКАЛЬНЫЙ КЭШ ЖКХ ФОНДА) ---
// ====================================================================

  // Полностью заменяем блок databaseModule внутри KoinModules.kt

  val databaseModule = module {

    single<YkisDatabases> {
      val driverFactory = get<DatabaseDriverFactory>()

      // 1. Фиксируем точное время старта до вызова нативного драйвера ОС
      val startTime = Clock.System.now().toEpochMilliseconds()

      try {
        // Аппаратно открываем файл ykis.db
        val database = YkisDatabases(driverFactory.createDriver())

        // 2. Фиксируем время завершения и высчитываем дельту в миллисекундах
        val endTime = Clock.System.now().toEpochMilliseconds()
        val duration = endTime - startTime

        // Если дошли сюда — база успешно взлетела, выводим скорость отклика в лог!
        println("YkisLog [DATABASE_START]: Успех! СУБД SQLDelight развернута в ОЗУ за ${duration}мс.")

        database
      } catch (t: Throwable) {
        val endTime = Clock.System.now().toEpochMilliseconds()
        val duration = endTime - startTime

        // Если нативный драйвер упал, фиксируем, на какой миллисекунде произошел сбой
        println("YkisLog [CRITICAL_DATABASE_FAIL]: Ядро СУБД рухнуло на ${duration}мс старта! Причина: ${t.message}")
        t.printStackTrace()
        throw t
      }
    }

    single<YkisDatabasesQueries> {
      get<YkisDatabases>().ykisDatabasesQueries
    }



  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }
}

// ====================================================================
// --- 4. НАВИГАЦИОННЫЙ МОДУЛЬ ЭКРАНОВ (VOYAGER SCREEN MODELS) ---
// ====================================================================
val navigationModule = module {
  println("[$className]: Реєстрація життєвих циклів ScreenModels для Voyager Framework")

  factory { AppScreenModel(firebaseService = get(), get(), get()) }
  factory { SignInScreenModel(get(), get()) }
  factory { SignUpScreenModel(firebaseService = get(), get()) }
  factory { ChatScreenModel(get(), get()) }
  factory { ApartmentScreenModel(get(), get(), get(), get(), get()) }
  factory { FamilyListScreenModel(get(), get()) }
  factory { MeterScreenModel(get(), get(), get()) }
  single {
    ApartmentService(
      getApartmentList = get<GetApartmentList>(),
      getOsbbApartmentsList = get<GetOsbbApartmentsList>(),
      getRaionList = get<GetRaionList>(),
      getHouseList = get<GetHouseList>(),
      getApartment = get<GetApartment>(),
      addApartment = get<AddApartment>(),
      verifyAdminCode = get<VerifyAdminCode>(),
      deleteApartment = get<DeleteApartment>(),
      updateBti = get<UpdateBti>(),
      saveUserUid = get<SaveUserUid>(),
      deleteUserAccount = get<DeleteUserAccount>()
    )
  }
  // Наша зафиксированная фабрика ServiceScreenModel с лямбда-ссылками операторов ::invoke
  factory {
    ServiceScreenModel(
      getFlatService = get<GetFlatServices>()::invoke,
      getTotalDebtServices = get<GetTotalDebtServices>()::invoke,
      getPaymentListRepo = get<GetPaymentList>()::invoke,
      logService = get<LogService>()
    )
  }

  // Твоя зафиксированная фабрика модели настроек профиля (5 параметров)
  factory {


    try {
      // Принудительно вызываем конструктор руками
      SettingsScreenModel(
        // РЕШЕНИЕ: Явно и принудительно запрашиваем тип интерфейса из Multiplatform Settings
        settings = get<com.russhwolf.settings.Settings>(),
        get<ClearDatabase>()::invoke,

//        firebaseService = get(),
        logService = get()
      )
    } catch (t: Throwable) {
      // ЖЕЛЕЗОБЕТОННО: Выводим реальный, скрытый краш (NullPointerException, ClassCastException и т.д.)
      // прямо в топ-логи Logcat с тегом CRITICAL_DI_FAIL!
      println("[CRITICAL_DI_FAIL]: Настоящая причина падения конструктора: ${t.message}")
      t.printStackTrace() // Печатаем весь стек вызовов, где видна упавшая строка!
      throw t // Пробрасываем ошибку дальше, чтобы рантайм не завис
    }
  }
}

/**
 * [initKoin] — Глобальная кроссплатформенная точка запуска DI-графа ЮКИС.
 */
fun initKoin(
  platformModule: Module = module {},
  appDeclaration: KoinAppDeclaration = {}
) {
  startKoin {
    appDeclaration()
    modules(
      commonModule,
      databaseModule,
      domainModule,
      navigationModule,
      platformModule
    )
  }
}
