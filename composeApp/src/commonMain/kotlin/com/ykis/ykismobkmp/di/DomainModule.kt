package com.ykis.ykismobkmp.di

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
import com.ykis.ykismobkmp.domain.repository.ledger.request.GetFlatServices
import com.ykis.ykismobkmp.domain.repository.ledger.request.GetPaymentList
import com.ykis.ykismobkmp.domain.repository.ledger.request.GetTotalDebtServices
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
import com.ykis.ykismobkmp.domain.services.ClearDatabase
import org.koin.dsl.module

private const val className = "DomainModule"

/**
 * [domainModule] — Чистый монолитный Koin-модуль бизнес-логики и сценариев (Use Cases) ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Все огромные легаси-лямбды баз данных вырезаны! Автоматическая инжекция get() под ключ.
 * Намертво зафиксирован для полной замены.
 */
val domainModule = module {
  println("[$className]: Об'єднання та тотальна фіксація доменного модуля Use Cases YkisMobKMP")

  // ====================================================================
  // --- 1. КАНOНИЧНЫЕ USE CASES НЕДВИЖИМОСТИ (КВАРТИРНЫЙ ПАКЕТ) -------
  // ====================================================================
  factory { AddApartment(repository = get(), cache = get()) }
  factory { DeleteApartment(repository = get(), cache = get()) }
  factory { DeleteUserAccount(repository = get(), cache = get()) }
  factory { SaveUserUid(repository = get()) }
  factory { UpdateBti(repository = get(), cache = get()) }
  factory { VerifyAdminCode(repository = get()) }
  factory { GetApartmentList(repository = get(), cache = get()) }
  factory { GetApartment(repository = get(), cache = get()) }
  factory { GetOsbbApartmentsList(repository = get(), cache = get()) }
  factory { GetRaionList(repository = get(), cache = get()) }
  factory { GetHouseList(repository = get(), cache = get()) }
  factory { GetFamilyList(repository = get(), cache = get()) }
  factory { ClearDatabase() }

  // ====================================================================
  // --- 2. КАНOНИЧНЫЕ USE CASES ПРИБОРОВ УЧЕТА (ПАКЕТ СЧЕТЧИКОВ) ------
  // ====================================================================
  factory { AddWaterReading(repository = get()) }
  factory { AddHeatReading(repository = get()) }
  factory { DeleteLastWaterReading(repository = get(), meterCache = get()) }
  factory { DeleteLastHeatReading(repository = get(), meterCache = get()) }
  factory { GetHeatMeterList(repository = get(), meterCache = get()) }
  factory { GetHeatReadings(repository = get(), meterCache = get()) }
  factory { GetLastHeatReading(repository = get(), meterCache = get()) }
  factory { GetLastWaterReading(repository = get(), meterCache = get()) }
  factory { GetWaterMeterList(repository = get(), meterCache = get()) }
  factory { GetWaterReadings(repository = get(), meterCache = get()) }

  // ====================================================================
  // --- 3. КАНOНИЧНЫЕ USE CASES БУХГАЛТЕРСКОГО БИЛЛИНГА (ПАКЕТ LEDGER) -
  // ====================================================================
  factory { GetPaymentList(repository = get(), ledgerCache = get()) }
  factory { GetFlatServices(repository = get(), ledgerCache = get()) }
  factory { GetTotalDebtServices(repository = get(), ledgerCache = get()) }
}
