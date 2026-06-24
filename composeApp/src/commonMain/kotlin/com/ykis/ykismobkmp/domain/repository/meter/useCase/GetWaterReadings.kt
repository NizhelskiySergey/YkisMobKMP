package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetWaterReadings] — Use Case для отримання історії показань води.
 * УНІФІКОВАНО: Прямий запит для Web, робота з кешем для Android/iOS.
 */
class GetWaterReadings(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetWaterReadings"

  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<List<WaterReadingEntity>>> = flow {
    if (uid.isBlank() || vodomerId <= 0L) {
      emit(Resource.Success(emptyList()))
      return@flow
    }

    emit(Resource.Loading())

    val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true) || 
                com.ykis.ykismobkmp.getPlatform().name.contains("JS", true)

    try {
      // 1. КЕШ (Мобільні)
      if (!isWeb) {
          try {
              val local = meterCache.getWaterReadingsByMeter(vodomerId)
              if (local.isNotEmpty()) emit(Resource.Success(local))
          } catch (e: Exception) { }
      }

      // 2. МЕРЕЖА
      println("[$className]: Запит історії води для ID: $vodomerId")
      val response = repository.getWaterReadings(uid, vodomerId)
      val remoteReadings = response.waterReadings ?: emptyList()

      if (response.success == 1) {
        emit(Resource.Success(remoteReadings))

        // 3. ОНОВЛЕННЯ КЕШУ
        if (!isWeb && remoteReadings.isNotEmpty()) {
            try {
                meterCache.deleteWaterReadingsByMeter(vodomerId)
                meterCache.insertWaterReadings(remoteReadings)
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
