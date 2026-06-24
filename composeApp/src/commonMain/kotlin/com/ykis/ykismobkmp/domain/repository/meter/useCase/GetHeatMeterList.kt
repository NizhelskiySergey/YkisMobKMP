package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetHeatMeterList] — Use Case для отримання списку лічильників тепла.
 * УНІФІКОВАНО: Пряма робота з мережею для Web, кешування для мобільних платформ.
 */
class GetHeatMeterList(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetHeatMeterList"

  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<HeatMeterEntity>>> = flow {
    if (uid.isBlank() || addressId <= 0L) {
      emit(Resource.Success(emptyList()))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. СПРОБА КЕШУ (тільки для мобільних)
      if (!isWeb) {
          try {
              val local = meterCache.getHeatMetersByApartment(addressId)
              if (local.isNotEmpty()) emit(Resource.Success(local))
          } catch (e: Exception) { }
      }

      // 2. МЕРЕЖЕВИЙ ЗАПИТ
      val response = repository.getHeatMeterList(uid, addressId)
      val remoteMeters = response.heatMeters ?: emptyList()

      if (response.success == 1) {
        emit(Resource.Success(remoteMeters))

        // 3. ОНОВЛЕННЯ КЕШУ (тільки для мобільних)
        if (!isWeb && remoteMeters.isNotEmpty()) {
            try {
                meterCache.deleteHeatMetersByApartment(addressId)
                meterCache.insertHeatMeter(remoteMeters)
            } catch (e: Exception) { }
        }
      } else {
          emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Success(emptyList()))
    }
  }.flowOn(Dispatchers.Default)
}
