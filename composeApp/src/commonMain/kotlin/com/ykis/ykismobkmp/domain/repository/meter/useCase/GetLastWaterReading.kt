package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [GetLastWaterReading] — КМР Use Case для отримання останнього показника води.
 */
class GetLastWaterReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetLastWaterReading"

  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<WaterReadingEntity?>> =
    flow<Resource<WaterReadingEntity?>> {
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // 1. ПЕРЕВІРКА КЕШУ - Безпечно для Web
        var cachedReading: WaterReadingEntity? = null
        try {
          if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              withTimeoutOrNull(500) {
                  cachedReading = meterCache.getLastWaterReadingByMeter(vodomerId)
              }
          } else {
              cachedReading = meterCache.getLastWaterReadingByMeter(vodomerId)
          }

          if (cachedReading != null) {
            println("[$className.$methodName]: [LOCAL_HIT] Завантажено з кешу")
            emit(Resource.Success<WaterReadingEntity?>(cachedReading))
          }
        } catch (e: Exception) {
          println("[$className.$methodName]: Локальна БД недоступна або зависла")
        }

        // 2. ЗАПИТ В МЕРЕЖУ
        println("[$className.$methodName]: [NETWORK_START] ID: $vodomerId")
        val response = repository.getLastWaterReading(uid, vodomerId)

        if (response.success == 1) {
          val remoteReading = response.waterReading 
          emit(Resource.Success<WaterReadingEntity?>(remoteReading))

          // 3. ПЕРЕЗАПИС КЕШУ
          if (remoteReading != null) {
            try {
              meterCache.insertWaterReadings(listOf(remoteReading))
            } catch (dbEx: Exception) {
              println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
            }
          }
        } else {
          emit(Resource.Error<WaterReadingEntity?>(message = response.message ?: "Помилка завантаження"))
        }

      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] ${ex.message}")
        val lastHope = try { meterCache.getLastWaterReadingByMeter(vodomerId) } catch(e: Exception) { null }
        if (lastHope != null) {
          emit(Resource.Success<WaterReadingEntity?>(lastHope))
        } else {
          emit(Resource.Error<WaterReadingEntity?>(message = "Відсутній зв'язок"))
        }
      }
    }.flowOn(Dispatchers.Default)
}
