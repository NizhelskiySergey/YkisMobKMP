package com.ykis.ykismobkmp.di

import com.ykis.mob.domain.meter.water.reading.request.GetWaterReadings
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
import com.ykis.ykismobkmp.domain.repository.family.FamilyRepository
import com.ykis.ykismobkmp.domain.repository.family.FamilyRepositoryImpl
import com.ykis.ykismobkmp.domain.repository.family.usecase.GetFamilyList
import com.ykis.ykismobkmp.domain.repository.meter.useCase.AddHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.AddWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.DeleteLastHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.DeleteLastWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetHeatMeterList
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetHeatReadings
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetLastHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetLastWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetWaterMeterList
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
  single<FamilyRepository> { FamilyRepositoryImpl(get()) }
  single {ChatRepository(firestore = get(),realtime = get(),storage = get(),functions = get(),aiManager = get())
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
// 3. ДОМЕННЫЙ МОДУЛЬ (Use Cases)
// ==========================================
val domainModule = module {
  // Экранная модель чата для Voyager
  factory { ChatScreenModel(get(), get()) }
  factory { SignInScreenModel(get(), get()) }
  factory { SignUpScreenModel(get(), get()) }
  factory { ApartmentScreenModel(get(),get(),get(),get()) }
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
          queries.deleteHeatReadingsMeter(ids)

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
  factory {
    GetHeatReadings(
      repository = get(),
      getLocal = { teplomerId ->
        val queries = get<YkisDatabasesQueries>()

        // 1. Извлекаем список показаний счетчика тепла г. Южный
        val dbList = queries.getHeatReadingByMeter(teplomerId).executeAsList()

        // 2. ИСПРАВЛЕНО: Маппинг всей коллекции через твою точную КМР-функцию
        dbList.map { it.toDomainHeatReading() }
      },
      saveLocal = { teplomerId, list ->
        val queries = get<YkisDatabasesQueries>()

        // Выполняем атомарную очистку и перезапись кэша внутри транзакции
        queries.transaction {
          // 3. Сначала удаляем старый кэш по конкретному счетчику тепла
          queries.deleteHeatReadingsMeter(teplomerId)

          // 4. ИСПРАВЛЕНО: Записываем свежие показания строго по твоему новому эталону!
          // Переводим доменную модель в модель БД и передаем один готовый объект в метод вставки.
          list.forEach { domainReading ->
            queries.insertHeatReading(domainReading.toDbHeatReading())
          }
        }
      }
    )
  }
  factory {
    GetHeatMeterList(
      repository = get(),
      getLocal = { addressId ->
        val queries = get<YkisDatabasesQueries>()
        // Извлекаем List<DbHeatMeterTable> и переводим в доменную UI-коллекцию
        queries.getHeatMeterByApartment(addressId)
          .executeAsList()
          .map { it.toDomainHeatMeter() } // Убедись, что маппер toDomainHeatMeter() написан в data/db/mapper
      },
      saveLocal = { addressId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          // 1. Атомарно очищаем старый кэш счетчиков по этой квартире
          queries.deleteHeatMetersByApartment(addressId)

          // 2. Записываем свежие приборы учета, используя КМР-маппер .toDbHeatMeter()
          list.forEach { domainMeter ->
            queries.insertHeatMeter(domainMeter.toDbHeatMeter())
          }
        }
      }
    )
  }
  factory {
    GetWaterMeterList(
      repository = get(),
      getLocal = { addressId ->
        val queries = get<YkisDatabasesQueries>()
        // Извлекаем List<DbWaterMeterTable> из SQLDelight и переводим в доменную UI-коллекцию
        queries.getWaterMetersByApartment(addressId)
          .executeAsList()
          .map { it.toDomainWaterMeter() } // Убедись, что маппер toDomainWaterMeter() написан в data/db/mapper
      },
      saveLocal = { addressId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          // 1. Атомарно очищаем старый локальный кэш водомеров по этой квартире
          queries.deleteWaterMetersApartment(addressId)

          // 2. Записываем свежие приборы учета, используя КМР-маппер .toDbWaterMeter()
          list.forEach { domainMeter ->
            queries.insertWaterMeter(domainMeter.toDbWaterMeter())
          }
        }
      }
    )
  }
  factory {
    GetWaterReadings(
      repository = get(),
      getLocal = { vodomerId ->
        val queries = get<YkisDatabasesQueries>()
        // Извлекаем List<DbWaterReadingTable> из SQLDelight и переводим в доменную коллекцию
        queries.getWaterReadingsByMeter(vodomerId)
          .executeAsList()
          .map { it.toDomainWaterReading() } // Убедись, что маппер toDomainWaterReading() написан
      },
      saveLocal = { vodomerId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          // 1. Атомарно очищаем старый локальный кэш показаний по этому водомеру
          // Оборачиваем в listOf() из-за оператора IN в SQL, если он используется в .sq файле
          queries.deleteWaterReadingsMeter(vodomerId)

          // 2. Записываем свежие показания, используя КМР-маппер .toDbWaterReading()
          list.forEach { domainReading ->
            queries.insertWaterReading(domainReading.toDbWaterReading())
          }
        }
      }
    )
  }
  factory {
    GetLastWaterReading(
      repository = get(),
      getLocal = { vodomerId ->
        val queries = get<YkisDatabasesQueries>()
        // Извлекаем последнюю запись для водомера (замени на свой точный SQL-метод из .sq файла)
        val dbEntity = queries.getLastWaterReadingByMeter(vodomerId).executeAsOneOrNull()
        dbEntity?.toDomainWaterReading() // Маппер строки БД в доменную сущность
      },
      saveLocal = { domainReading ->
        val queries = get<YkisDatabasesQueries>()
        // Переводим доменную модель в модель БД и записываем в SQLite кэш одним объектом
        queries.insertWaterReading(domainReading.toDbWaterReading())
      }
    )
  }
  factory {
    GetLastWaterReading(
      repository = get(),
      getLocal = { vodomerId ->
        val queries = get<YkisDatabasesQueries>()
        // Высокопроизводительный КМР-запрос одной строки вместо localReadings.lastOrNull()
        val dbEntity = queries.getLastWaterReadingByMeter(vodomerId).executeAsOneOrNull()
        dbEntity?.toDomainWaterReading()
      },
      saveLocal = { domainReading ->
        val queries = get<YkisDatabasesQueries>()
        // Сохраняем один чистый SQLDelight объект
        queries.insertWaterReading(domainReading.toDbWaterReading())
      }
    )
  }
  factory {
    GetLastHeatReading(
      repository = get(),
      getLocal = { teplomerId ->
        val queries = get<YkisDatabasesQueries>()
        // Высокопроизводительный КМР-запрос одной строки вместо тяжелого localReadings.lastOrNull()
        val dbEntity = queries.getLastHeatReadingByMeter(teplomerId).executeAsOneOrNull()
        dbEntity?.toDomainHeatReading() // Наш CamelCase маппер
      },
      saveLocal = { domainReading ->
        val queries = get<YkisDatabasesQueries>()
        // Сохраняем один чистый SQLDelight объект (включая экранированное поле operator_)
        queries.insertHeatReading(domainReading.toDbHeatReading())
      }
    )
  }
  factory {
    DeleteLastWaterReading(
      repository = get(),
      deleteLocal = { readingId ->
        val queries = get<YkisDatabasesQueries>()
        // Вызываем атомарное удаление строки истории по ее первичному Long-ключу pokId из .sq файла
        queries.transaction {
          queries.deleteWaterReadingByPokId(readingId)
        }
      }
    )
  }
  factory {
    DeleteLastHeatReading(
      repository = get(),
      deleteLocal = { readingId ->
        val queries = get<YkisDatabasesQueries>()
        // Вызываем атомарное удаление строки истории тепла по ее первичному Long-ключу pokId из .sq файла
        queries.transaction {
          queries.deleteHeatReadingByPokId(readingId)
        }
      }
    )
  }
  // Остальные сервисные Use Cases (Инжектируются автоматически)
  factory {AddWaterReading(repository = get())}
  factory {AddHeatReading(repository = get())}

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
