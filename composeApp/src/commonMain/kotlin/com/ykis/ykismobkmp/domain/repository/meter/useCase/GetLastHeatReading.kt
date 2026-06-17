package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [GetLastHeatReading] — КМР Use Case для отримання останнього показника тепла.
 */
class GetLastHeatReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetLastHeatReading"

  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<HeatReadingEntity?>> =
    flow<Resource<HeatReadingEntity?>> {
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // 1. ПЕРЕВІРКА КЕШУ - Безпечно для Web
        var cachedReading: HeatReadingEntity? = null
        try {
          if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              withTimeoutOrNull(500) {
                  cachedReading = meterCache.getLastHeatReadingByMeter(teplomerId)
              }
          } else {
              cachedReading = meterCache.getLastHeatReadingByMeter(teplomerId)
          }

          if (cachedReading != null) {
            println("[$className.$methodName]: [LOCAL_HIT] Завантажено з кешу")
            emit(Resource.Success<HeatReadingEntity?>(cachedReading))
          }
        } catch (e: Exception) {
          println("[$className.$methodName]: Локальна БД недоступна або зависла")
        }

        // 2. ЗАПИТ В МЕРЕЖУ
        println("[$className.$methodName]: [NETWORK_START] ID: $teplomerId")
        val response = repository.getLastHeatReading(uid, teplomerId)

        if (response.success == 1) {
          val remoteReading = response.heatReading 
          emit(Resource.Success<HeatReadingEntity?>(remoteReading))

          // 3. ПЕРЕЗАПИС КЕШУ
          if (remoteReading != null) {
            try {
              meterCache.insertHeatReadings(listOf(remoteReading))
            } catch (dbEx: Exception) {
              println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
            }
          }
        } else {
          emit(Resource.Error<HeatReadingEntity?>(message = response.message ?: "Помилка завантаження"))
        }

      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] ${ex.message}")
        val lastHope = try { meterCache.getLastHeatReadingByMeter(teplomerId) } catch(e: Exception) { null }
        if (lastHope != null) {
          emit(Resource.Success<HeatReadingEntity?>(lastHope))
        } else {
          emit(Resource.Error<HeatReadingEntity?>(message = "Відсутній зв'язок"))
        }
      }
    }.flowOn(Dispatchers.Default)
}
