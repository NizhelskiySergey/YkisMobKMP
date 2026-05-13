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

// Импорты баз данных и фабрик
import com.ykis.ykismobkmp.db.YkisDatabases
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.domain.mapper.toDomainApartment
import com.ykis.ykismobkmp.domain.mapper.toDbApartment
import com.ykis.ykismobkmp.domain.mapper.toDomainRaion
import com.ykis.ykismobkmp.domain.mapper.toDbRaion
import com.ykis.ykismobkmp.domain.mapper.toDomainFamily
import com.ykis.ykismobkmp.domain.mapper.toDbFamily
import com.ykis.ykismobkmp.domain.mapper.toDbHouse
import com.ykis.ykismobkmp.domain.mapper.toDomainHouse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.AddApartment
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.DeleteApartment
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.DeleteUserAccount
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.GetApartment
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.GetApartmentList
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.GetHouseList
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.GetOsbbApartmentsList
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.GetRaionList
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.SaveUserUid
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.UpdateBti
import com.ykis.ykismobkmp.domain.repository.apartment.usecase.VerifyAdminCode
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.repository.family.FamilyRepository
import com.ykis.ykismobkmp.domain.repository.family.FamilyRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.family.usecase.GetFamilyList
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatViewModelModel
import com.ykis.ykismobkmp.ui.screens.settings.NewSettingsViewModel
import io.ktor.client.plugins.HttpTimeout

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

  // Репозитории (Управление Сетью)
  single<ApartmentRepository> { ApartmentRepositoryImpl(get()) }
  single<FamilyRepository> { FamilyRepositoryImpl(get()) }

  single {
    ChatRepository(
      firestore = get(),
      realtime = get(),
      storage = get(),
      functions = get(),
      aiManager = get()
    )
  }

  // Экранная модель чата для Voyager
  factory { ChatScreenModel(get(), get()) }
//  factory { NewSettingsViewModel(get()) }
  factory { ApartmentScreenModel(get(),get(),get(),get()) }
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
// 3. ДОМЕННЫЙ МОДУЛЬ (Use Cases)
// ==========================================
val domainModule = module {

  factory {
    GetApartment(
      repository = get(),
      getLocal = { id ->
        val queries = get<YkisDatabasesQueries>()
        val dbEntity = queries.getFlatById(id).executeAsOneOrNull()
        dbEntity?.toDomainApartment()
      },
      saveLocal = { entity ->
        val queries = get<YkisDatabasesQueries>()
        queries.insertApartment(entity.toDbApartment())
      }
    )
  }

  factory {
    GetOsbbApartmentsList(
      repository = get(),
      getLocalAll = {
        val queries = get<YkisDatabasesQueries>()
        queries.getApartmentList().executeAsList().map { it.toDomainApartment() }
      },
      syncFullDatabase = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          val ids = list.map { it.addressId }

          queries.deleteAllApartments()
          queries.deleteFamilyByApartmentIds(ids)
          queries.deleteWaterMetersNotInIds(ids)
          queries.deleteHeatMetersNotInIds(ids)
          queries.deleteServiceByApartment(ids)
          queries.deletePaymentsByApartment(ids)
          queries.deleteWaterReadingsNotInIds(ids)
          queries.deleteHeatReadingsNotInIds(ids)

          list.forEach { domainEntity ->
            queries.insertApartment(domainEntity.toDbApartment())
          }
        }
      }
    )
  }

  factory {
    GetRaionList(
      repository = get(),
      getLocal = {
        val queries = get<YkisDatabasesQueries>()
        queries.getRaionList().executeAsList().map { it.toDomainRaion() }
      },
      saveLocal = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          queries.deleteAllRaions() // Чистим старые районы перед вставкой
          list.forEach { domainRaion ->
            queries.insertRaion(domainRaion.toDbRaion())
          }
        }
      }
    )
  }

  factory {
    GetHouseList(
      repository = get(),
      getLocal = { rId ->
        val queries = get<YkisDatabasesQueries>()
        // Гарантируем, что rId передается как Long в SQLDelight
        val targetRaionId = rId.toString().toLongOrNull() ?: 0L
        queries.getHousesByRaion(targetRaionId).executeAsList().map { it.toDomainHouse() }
      },
      saveLocal = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          // Извлекаем raionId из первого элемента и безопасно приводим к Long
          val currentRaionId = list.firstOrNull()?.raionId?.toString()?.toLongOrNull()

          if (currentRaionId != null && currentRaionId != 0L) {
            // ИСПРАВЛЕНО: Каскадно зачищаем кэш старых домов по этому району
            queries.deleteHousesByRaionId(currentRaionId)
          }

          // Массовая атомарная вставка обновленных домов
          list.forEach { domainHouse ->
            queries.insertHouse(domainHouse.toDbHouse())
          }
        }
      }
    )
  }


  factory {
    GetFamilyList(
      repository = get(),
      getLocal = { id ->
        val queries = get<YkisDatabasesQueries>()
        queries.getFamilyByApartment(id).executeAsList().map { it.toDomainFamily() }
      },
      saveLocal = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          val targetAddressId = list.firstOrNull()?.addressId?.toLong() ?: 0L
          if (targetAddressId != 0L) {
            queries.deleteFamilyByAddressId(targetAddressId)
            list.forEach { domainFamily ->
              queries.insertFamily(domainFamily.toDbFamily())
            }
          }
        }
      }
    )
  }

  // Остальные сервисные Use Cases (Инжектируются автоматически)
  factory { AddApartment(get()) }
  factory { DeleteApartment(get()) }
  factory { DeleteUserAccount(get(),get()) }
  factory { DeleteApartment(get()) }
  factory { GetApartmentList(get(), get(), get()) }
  factory { GetOsbbApartmentsList(get(), get(), get()) }
  factory { GetHouseList(get()) }
  factory { GetRaionList(get()) }
  factory { SaveUserUid(get()) }
  factory { UpdateBti(get()) }
  factory { VerifyAdminCode(get()) }
  factory { GetFamilyList(get()) }

}

// ==========================================
// 4. СТАРТ DI ГРАФА (Для Mac/Desktop)
// ==========================================
fun initKoin(platformModule: Module = module {}) {
  startKoin {
    modules(
      commonModule,
      databaseModule,
      domainModule,
      platformModule
    )
  }
}
