package com.ykis.ykismobkmp.di

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.domain.mapper.toDbApartment
import com.ykis.ykismobkmp.domain.mapper.toDbFamily
import com.ykis.ykismobkmp.domain.mapper.toDbHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDbHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDbHouse
import com.ykis.ykismobkmp.domain.mapper.toDbPayment
import com.ykis.ykismobkmp.domain.mapper.toDbRaion
import com.ykis.ykismobkmp.domain.mapper.toDbService
import com.ykis.ykismobkmp.domain.mapper.toDbWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDbWaterReading
import com.ykis.ykismobkmp.domain.mapper.toDomainApartment
import com.ykis.ykismobkmp.domain.mapper.toDomainFamily
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDomainHouse
import com.ykis.ykismobkmp.domain.mapper.toDomainPayment
import com.ykis.ykismobkmp.domain.mapper.toDomainRaion
import com.ykis.ykismobkmp.domain.mapper.toDomainService
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
import com.ykis.ykismobkmp.domain.repository.services.request.GetFlatServices
import com.ykis.ykismobkmp.domain.repository.services.request.GetTotalDebtServices
import com.ykis.ykismobkmp.domain.services.ClearDatabase
import org.koin.dsl.module

private const val className = "DomainModule"

/**
 * [domainModule] — Чистый монолитный Koin-модуль бизнес-логики и сценариев (Use Cases) ЮКИС.
 * исключительно реальные фабрики транзакционного кэширования для Mac Desktop, Android и iOS.
 */
val domainModule = module {
  println("[$className]: Об'єднання та тотальна фіксація доменного модуля Use Cases YkisMobKMP")

  // ====================================================================
  // --- 1. БАЗОВЫЕ КОММУНАЛЬНЫЕ USE CASES (ДЛЯ СЕТЕВЫХ ЭКРАНОВ ГИОЦ) ---
  // ====================================================================
  factory { AddWaterReading(repository = get()) }
  factory { AddHeatReading(repository = get()) }
  factory { AddApartment(repository = get()) }
  factory { DeleteApartment(repository = get()) }
  factory { DeleteUserAccount(get(), get()) }
  factory { SaveUserUid(repository = get()) }
  factory { UpdateBti(repository = get()) }
  factory { VerifyAdminCode(repository = get()) }
  factory { ClearDatabase() }

  // ====================================================================
  // --- 2. АТОМАРНЫЕ USE CASES С ТРАНЗАКЦИОННЫМ КЭШИРОВАНИЕМ SQLDELIGHT ---
  // ====================================================================
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
    GetApartmentList(
      repository = get(),
      getLocal = {
        val queries = get<YkisDatabasesQueries>()
        queries.getApartmentList().executeAsList().map { it.toDomainApartment() }
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
          queries.deleteAllRaions()
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
        queries.getPaymentsByApartment(addressId).executeAsList().map { it.toDomainPayment() }
      },
      saveLocal = { addressId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          queries.deletePaymentsByApartment(addressId)
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
        queries.getHeatReadingByMeter(teplomerId).executeAsList().map { it.toDomainHeatReading() }
      },
      saveLocal = { teplomerId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          queries.deleteHeatReadingsMeter(teplomerId)
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
        queries.getHeatMeterByApartment(addressId).executeAsList().map { it.toDomainHeatMeter() }
      },
      saveLocal = { addressId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          queries.deleteHeatMetersByApartment(addressId)
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
        queries.getWaterMetersByApartment(addressId).executeAsList().map { it.toDomainWaterMeter() }
      },
      saveLocal = { addressId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          queries.deleteWaterMetersApartment(addressId)
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
        queries.getWaterReadingsByMeter(vodomerId).executeAsList().map { it.toDomainWaterReading() }
      },
      saveLocal = { vodomerId, list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          queries.deleteWaterReadingsMeter(vodomerId)
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
        val dbEntity = queries.getLastWaterReadingByMeter(vodomerId).executeAsOneOrNull()
        dbEntity?.toDomainWaterReading()
      },
      saveLocal = { domainReading ->
        val queries = get<YkisDatabasesQueries>()
        queries.insertWaterReading(domainReading.toDbWaterReading())
      }
    )
  }

  factory {
    GetLastHeatReading(
      repository = get(),
      getLocal = { teplomerId ->
        val queries = get<YkisDatabasesQueries>()
        val dbEntity = queries.getLastHeatReadingByMeter(teplomerId).executeAsOneOrNull()
        dbEntity?.toDomainHeatReading()
      },
      saveLocal = { domainReading ->
        val queries = get<YkisDatabasesQueries>()
        queries.insertHeatReading(domainReading.toDbHeatReading())
      }
    )
  }

  factory {
    DeleteLastWaterReading(
      repository = get(),
      deleteLocal = { readingId ->
        val queries = get<YkisDatabasesQueries>()
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
        queries.transaction {
          queries.deleteHeatReadingByPokId(readingId)
        }
      }
    )
  }

  factory {
    GetFlatServices(
      repository = get(),
      getLocal = { addressId, serviceType, year ->
        val queries = get<YkisDatabasesQueries>()
        queries.getServiceDetail(addressId, serviceType, year)
          .executeAsList()
          .map { it.toDomainService() }
      },
      saveLocal = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          val firstItem = list.firstOrNull()
          if (firstItem != null) {
            queries.deleteServiceByApartment(firstItem.addressId)
          }
          list.forEach { domainService ->
            queries.insertService(domainService.toDbService())
          }
        }
      }
    )
  }

  factory {
    GetTotalDebtServices(
      repository = get(),
      getLocal = { addressId ->
        val queries = get<YkisDatabasesQueries>()
        queries.getTotalDebt(addressId)
          .executeAsOneOrNull()
          ?.toDomainService()
      },
      saveLocal = { list ->
        val queries = get<YkisDatabasesQueries>()
        queries.transaction {
          val firstItem = list.firstOrNull()
          if (firstItem != null) {
            queries.deleteServiceByApartment(firstItem.addressId)
          }
          list.forEach { domainService ->
            queries.insertService(domainService.toDbService())
          }
        }
      }
    )
  }
}
