package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetHeatReadings] — Use Case для отримання історії показань тепла.
 */
class GetHeatReadings(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetHeatReadings"

  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<List<HeatReadingEntity>>> = flow {
    if (uid.isBlank() || teplomerId <= 0L) {
      emit(Resource.Success(emptyList()))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. КЕШ
      if (!isWeb) {
          try {
              val local = meterCache.getHeatReadingsByMeter(teplomerId)
              if (local.isNotEmpty()) emit(Resource.Success(local))
          } catch (e: Exception) { }
      }

      // 2. МЕРЕЖА
      println("[$className]: Запит історії тепла для ID: $teplomerId")
      val response = repository.getHeatReadings(uid, teplomerId)
      val remoteReadings = response.heatReadings ?: emptyList()

      if (response.success == 1) {
        emit(Resource.Success(remoteReadings))

        // 3. ОНОВЛЕННЯ КЕШУ
        if (!isWeb && remoteReadings.isNotEmpty()) {
            try {
                meterCache.deleteHeatReadingsByMeter(teplomerId)
                meterCache.insertHeatReadings(remoteReadings)
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
