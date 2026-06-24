package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetLastHeatReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetLastHeatReading"

  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<HeatReadingEntity?>> = flow {
    if (uid.isBlank() || teplomerId <= 0L) {
      emit(Resource.Success<HeatReadingEntity?>(null))
      return@flow
    }

    emit(Resource.Loading<HeatReadingEntity?>())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      println("[$className]: Запит останнього показання тепла для ID: $teplomerId")
      val response = repository.getLastHeatReading(uid, teplomerId)
      val remoteReading = response.heatReading

      if (response.success == 1) {
        emit(Resource.Success<HeatReadingEntity?>(remoteReading))

        if (!isWeb && remoteReading != null) {
            try { meterCache.insertHeatReadings(listOf(remoteReading)) } catch (e: Exception) { }
        }
      } else {
          if (!isWeb) {
              val local = try { meterCache.getLastHeatReadingByMeter(teplomerId) } catch(e: Exception) { null }
              emit(Resource.Success<HeatReadingEntity?>(local))
          } else {
              emit(Resource.Success<HeatReadingEntity?>(null))
          }
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Success<HeatReadingEntity?>(null))
    }
  }.flowOn(Dispatchers.Default)
}
