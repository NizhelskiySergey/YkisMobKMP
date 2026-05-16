package com.ykis.ykismobkmp.di


import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.db.mapper.toDbApartment
import com.ykis.ykismobkmp.data.db.mapper.toDomainApartment
import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.domain.mapper.toDbFamily
import com.ykis.ykismobkmp.domain.mapper.toDbHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDbHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDbHouse
import com.ykis.ykismobkmp.domain.mapper.toDbPayment
import com.ykis.ykismobkmp.domain.mapper.toDbRaion
import com.ykis.ykismobkmp.domain.mapper.toDbWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDbWaterReading
import com.ykis.ykismobkmp.domain.mapper.toDomainFamily
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDomainHouse
import com.ykis.ykismobkmp.domain.mapper.toDomainPayment
import com.ykis.ykismobkmp.domain.mapper.toDomainRaion
import com.ykis.ykismobkmp.domain.mapper.toDomainWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainWaterReading
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.AddApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.DeleteApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.DeleteUserAccount
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetApartment
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetApartmentList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetFamilyList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetHouseList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetOsbbApartmentsList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.GetRaionList
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.SaveUserUid
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.UpdateBti
import com.ykis.ykismobkmp.domain.repository.apartment.useCase.VerifyAdminCode
import com.ykis.ykismobkmp.domain.repository.meter.useCase.AddHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.AddWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.DeleteLastHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.DeleteLastWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetHeatMeterList
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetHeatReadings
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetLastHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetLastWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetWaterMeterList
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetWaterReadings
import com.ykis.ykismobkmp.domain.repository.payment.request.GetPaymentList
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.family.FamilyListScreenModel
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.ui.screens.service.GetFlatServices
import com.ykis.ykismobkmp.ui.screens.service.GetTotalDebtServices
import com.ykis.ykismobkmp.ui.screens.service.InsertPayment
import com.ykis.ykismobkmp.ui.screens.service.ServiceScreenModel
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import kotlinx.coroutines.flow.flow
import org.koin.dsl.module


// ==========================================
// 3. ДОМЕННЫЙ МОДУЛЬ (Use Cases)
// ==========================================


val domainModule = module {
  // Экранная модель чата для Voyager
  factory { ChatScreenModel(get(), get()) }
  factory { SignInScreenModel(get(), get()) }
  factory { SignUpScreenModel(get(), get()) }
  factory { SettingsScreenModel(get(), get(),get(),get()) }
  factory { ApartmentScreenModel(get(), get(), get()) }
  factory { FamilyListScreenModel(get(), get()) }
  factory { MeterScreenModel(get(), get(),get()) }

  factory {
    ServiceScreenModel(
      getFlatService = get<GetFlatServices>(),
      getTotalDebtServices = get<GetTotalDebtServices>(),
      getPaymentListRepo = { addressId, year, uid ->
        val useCase = get<com.ykis.ykismobkmp.domain.repository.payment.request.GetPaymentList>()
        useCase(addressId, year, uid)
      },

      insertPaymentRepo = get<InsertPayment>(),
      logService = get()
    )
  }


  factory {
    SettingsScreenModel(
      dataStore = get(),
      firebaseService = get(),
      logService = get(),
      clearDatabase = {
        flow {
          val queries = get<YkisDatabasesQueries>()
          queries.transaction {
            // Атомарно вырезаем кэш всего ЖКХ-фонда при выходе из учетной записи
            queries.deleteAllApartments()
            queries.deleteAllFamily()
            queries.deleteAllPayments()
            queries.deleteAllHeatReadings()
            queries.deleteAllHeatMeters()
            queries.deleteAllWaterMeters()
            queries.deleteAllWaterReadings()
          }
          emit(Resource.Success(Unit))
        }
      }
    )
  }

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
          queries.deleteAllApartments()
          queries.deleteAllFamily()
          queries.deleteAllWaterMeters()
          queries.deleteAllHeatMeters()
          queries.deleteAllService()
          queries.deleteAllPayments()
          queries.deleteAllWaterReadings()
          queries.deleteAllHeatReadings()
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
      getLocal = { raionId ->
        val queries = get<YkisDatabasesQueries>()
        // Гарантируем, что rId передается как Long в SQLDelight
        val targetRaionId = raionId.toString().toLongOrNull() ?: 0L
        queries.getHousesByRaion(targetRaionId).executeAsList().map { it.toDomainHouse() }
      },
      saveLocal = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          val currentRaionId = list.firstOrNull()?.raionId?.toString()?.toLongOrNull()
          if (currentRaionId != null && currentRaionId != 0L) {
            queries.deleteHousesByRaionId(currentRaionId)
          }
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
    GetPaymentList(
      repository = get(),
      getLocal = { addressId ->
        val queries = get<YkisDatabasesQueries>()
        // Извлекаем List<DbPaymentTable> и переводим в доменную UI-коллекцию
        queries.getPaymentsByApartment(addressId)
          .executeAsList()
          .map { it.toDomainPayment() } // Убедись, что маппер точных платежей написан в data/db/mapper
      },
      saveLocal = { addressId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          // 1. Атомарно очищаем старый локальный кэш оплат по этой квартире
          queries.deletePaymentsByApartment(addressId)

          // 2. Записываем свежие транзакции, используя КМР-маппер .toDbPayment()
          list.forEach { domainPayment ->
            queries.insertPayment(domainPayment.toDbPayment())
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
  factory { AddWaterReading(repository = get()) }
  factory { AddHeatReading(repository = get()) }

  factory { AddApartment(get()) }
  factory { DeleteApartment(get()) }
  factory { DeleteUserAccount(get(), get()) }
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
