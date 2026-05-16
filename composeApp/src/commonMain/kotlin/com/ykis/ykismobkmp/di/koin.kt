package com.ykis.ykismobkmp.di

import com.ykis.ykismobkmp.core.utils.SnackbarManager
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.functions.functions
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.db.YkisDatabases
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.ai.GeminiCloudProvider
import com.ykis.ykismobkmp.domain.mapper.toDomainRaion
import com.ykis.ykismobkmp.domain.mapper.toDbRaion
import com.ykis.ykismobkmp.domain.mapper.toDomainFamily
import com.ykis.ykismobkmp.domain.mapper.toDbFamily
import com.ykis.ykismobkmp.domain.mapper.toDbHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDbHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDbHouse
import com.ykis.ykismobkmp.domain.mapper.toDbWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDbWaterReading
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDomainHouse
import com.ykis.ykismobkmp.domain.mapper.toDomainWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainWaterReading
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
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetFamilyList
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepositoryImpl
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.FirebaseServiceImpl
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.KoinApplication

// ==========================================
// 1. ОБЩИЙ МОДУЛЬ (Сеть, Firebase, Чаты)
// ==========================================
val commonModule = module {
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

  // Кроссплатформенный Firebase (GitLive)
  single { Firebase.firestore }
  single { Firebase.database }
  single { Firebase.storage }
  single { Firebase.functions }
  single<FirebaseService> { FirebaseServiceImpl(get(), get(), get()) }
  single { LogService() }
  // Репозитории (Управление Сетью)
  single<ApartmentRepository> { ApartmentRepositoryImpl(get()) }
  single<HeatMeterRepository> { HeatMeterRepositoryImpl(get()) }
  single<WaterMeterRepository> { WaterMeterRepositoryImpl(get()) }
  single<WaterMeterRepository> { WaterMeterRepositoryImpl(apiService = get()) }
  single<HeatMeterRepository> { HeatMeterRepositoryImpl(apiService = get()) }
  single {ChatRepository(firestore = get(),realtime = get(),storage = get(),functions = get(),aiManager = get())  }
  val commonModule = module {
    // 1. Регистрируем саму облачную GenerativeModel (из KMP-библиотеки Shreyas Patil)
    single {
      dev.shreyaspatil.ai.client.generativeai.GenerativeModel(
        modelName = "gemini-pro",
        apiKey = "ТВОЙ_API_КЛЮЧ_ИИ"
      )
    }
    single<GeminiAiManager> {
      GeminiCloudProvider(model = get(), localEngine = get())
    }
  }


}

// ==========================================
// 2. МОДУЛЬ БАЗЫ ДАННЫХ (Единый файл .sq)
// ==========================================
val databaseModule = module {
  single<YkisDatabases> {
    val driverFactory: DatabaseDriverFactory = get()
    YkisDatabases(driverFactory.createDriver())
  }

  // Регистрируем ОДИН общий синглтон запросов для всего приложения
  single<YkisDatabasesQueries> { get<YkisDatabases>().ykisDatabasesQueries }
}

// ==========================================
// 4. СТАРТ DI ГРАФА (Для Mac/Desktop)
// ==========================================
fun initKoin(
  platformModule: Module = module {},
  appDeclaration: KoinApplication.() -> Unit = {} // Декларация для контекстов платформ
) {
  startKoin {
    appDeclaration() // Вызывается в правильной области видимости KoinApplication
    modules(
      commonModule,
      databaseModule,
      domainModule,
      platformModule
    )
  }
}
