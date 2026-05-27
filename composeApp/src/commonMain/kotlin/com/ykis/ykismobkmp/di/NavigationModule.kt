package com.ykis.ykismobkmp.di

import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentService
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerService
import com.ykis.ykismobkmp.domain.repository.meter.MeterService
import com.ykis.ykismobkmp.domain.services.ClearDatabase
import com.ykis.ykismobkmp.ui.navigation.AppScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.AuthScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.family.FamilyListScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import org.koin.core.logger.KOIN_TAG
import org.koin.dsl.module


val navigationModule = module {
  println("[$KOIN_TAG]: Реєстрація життєвих циклів ScreenModels для Voyager Framework")
  single { AppScreenModel(firebaseService = get(), get(), get()) }

  // Остальные локальные модели экранов остаются в режиме factory для очистки памяти при закрытии окон
  factory { AuthScreenModel(get(), get(), get()) }
  factory { ChatScreenModel(get(), get()) }
  factory { ApartmentScreenModel(get(), get(), get()) }
  factory { FamilyListScreenModel(get(), get()) }
  factory { MeterScreenModel(get(), get()) }
  factory { ClearDatabase() }
  single {
    ApartmentService(
      getApartmentList = get(), getOsbbApartmentsList = get(), getRaionList = get(),
      getHouseList = get(), getApartment = get(), addApartment = get(),
      verifyAdminCode = get(), deleteApartment = get(), updateBti = get(),
      saveUserUid = get(), deleteUserAccount = get()
    )
  }

  // ИСПРАВЛЕНО НАМЕРТВО: Монолитный сервис-комбайн счетчиков тепла и воды ЮКИС
  single {
    MeterService(
      getWaterMeterList = get(), getWaterReadings = get(), getLastWaterReading = get(),
      addWaterReading = get(), deleteLastWaterReading = get(), getHeatMeterList = get(),
      getHeatReadings = get(), getLastHeatReading = get(), addHeatReading = get(),
      deleteLastHeatReading = get()
    )
  }
  single {
    LedgerService(
      getFlatServices = get(),
      getPaymentList = get(),
      getTotalDebtServices = get()
    )
  }

  // Фабрика ScreenModel финансово-расчетного учета коммунальных услуг стала кристально чистой!
  factory {
    LedgerScreenModel(
      ledgerService = get(), // Koin автоматически прокинет комбайн со всеми 3 Use Cases
      logService = get()
    )
  }
  // Фабрики ScreenModels финансово-расчетного учета коммунальных услуг


  single {
    try {
      SettingsScreenModel(
        settings = get(),
        firebaseService = get(),
        clearDatabase = get<ClearDatabase>()::invoke,
        logService = get()
      )
    } catch (t: Throwable) {
      println("[$KOIN_TAG.SettingsScreenModel_CRITICAL]: Настоящая причина падения конструктора: ${t.message}")
      t.printStackTrace()
      throw t
    }
  }
}
