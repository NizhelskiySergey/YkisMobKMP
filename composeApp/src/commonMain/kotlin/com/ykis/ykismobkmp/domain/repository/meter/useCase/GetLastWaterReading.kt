package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetLastWaterReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetLastWaterReading"

  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<WaterReadingEntity?>> = flow {
    if (uid.isBlank() || vodomerId <= 0L) {
      emit(Resource.Success<WaterReadingEntity?>(null))
      return@flow
    }

    emit(Resource.Loading<WaterReadingEntity?>())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      println("[$className]: Запит останнього показання води для ID: $vodomerId")
      val response = repository.getLastWaterReading(uid, vodomerId)
      val remoteReading = response.waterReading

      if (response.success == 1) {
        emit(Resource.Success<WaterReadingEntity?>(remoteReading))

        if (!isWeb && remoteReading != null) {
            try { meterCache.insertWaterReadings(listOf(remoteReading)) } catch (e: Exception) { }
        }
      } else {
          if (!isWeb) {
              val local = try { meterCache.getLastWaterReadingByMeter(vodomerId) } catch(e: Exception) { null }
              emit(Resource.Success<WaterReadingEntity?>(local))
          } else {
              emit(Resource.Success<WaterReadingEntity?>(null))
          }
      }

    } catch (ex: Exception) {
      println("[YkisLogKMP.${className}_ERROR]: ${ex.message}")
      emit(Resource.Success<WaterReadingEntity?>(null))
    }
  }.flowOn(Dispatchers.Default)
}
