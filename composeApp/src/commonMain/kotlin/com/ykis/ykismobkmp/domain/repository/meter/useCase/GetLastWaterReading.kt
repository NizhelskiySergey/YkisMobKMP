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

/**
 * [GetLastWaterReading] — Единый КМР-стандарт интерактора получения последнего показания водомера г. Южный.
 */
class GetLastWaterReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetLastWaterReading"

  /**
   * [invoke] — Выполнение Use Case.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<WaterReadingEntity?>.
   */
  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<WaterReadingEntity?>> =
    flow<Resource<WaterReadingEntity?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
        var cachedReading: WaterReadingEntity? = null
        try {
          if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              kotlinx.coroutines.withTimeoutOrNull(500) {
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
          println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
        }

        // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
        println("[$className.$methodName]: [NETWORK_START] ID: $vodomerId")
        val response = repository.getLastWaterReading(uid, vodomerId)

        if (response.success == 1) {
          val remoteReading = response.waterReading 

          emit(Resource.Success<WaterReadingEntity?>(remoteReading))

          // 3. ПЕРЕЗАПИСЬ КЭША
          if (remoteReading != null) {
            try {
              if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
                  meterCache.insertWaterReadings(listOf(remoteReading))
              }
            } catch (dbEx: Exception) {
              println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
            }
          }
        } else {
          println("[$className.$methodName]: [NETWORK_REJECT] Сервер повернул ошибку: ${response.message}")
          // Явно типизируем ошибку сервера под контракт потока
          emit(Resource.Error<WaterReadingEntity?>(message = response.message ?: "Помилка завантаження"))
        }

      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")
        ex.printStackTrace()

        // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
        val lastHope = meterCache.getLastWaterReadingByMeter(vodomerId)
        if (lastHope != null) {
          println("[$className.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
          emit(Resource.Success<WaterReadingEntity?>(lastHope))
        } else {
          SnackbarManager.showMessage("Відсутній зв'язок з сервером водопостачання")
          // Принудительно типизируем КМР-фабрику ошибки, закрывая Return type mismatch
          emit(Resource.Error<WaterReadingEntity?>(message = "Відсутній зв'язок та немає збережених даних"))
        }
      }
    }.flowOn(Dispatchers.Default) // Безопасный КМР-пул потоков корутин
}

