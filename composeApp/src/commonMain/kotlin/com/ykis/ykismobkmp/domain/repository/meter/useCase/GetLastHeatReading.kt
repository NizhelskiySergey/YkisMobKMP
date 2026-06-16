package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetLastHeatReading] — Единый КМР-стандарт интерактора получения последнего показания теплосчетчика г. Южный.
 */
class GetLastHeatReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetLastHeatReading"

  /**
   * [invoke] — Выполнение Use Case.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<HeatReadingEntity?>.
   */
  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<HeatReadingEntity?>> =
    flow<Resource<HeatReadingEntity?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
        var cachedReading: HeatReadingEntity? = null
        try {
          if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              kotlinx.coroutines.withTimeoutOrNull(500) {
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
          println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
        }

        // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
        println("[$className.$methodName]: [NETWORK_START] ID: $teplomerId")
        val response = repository.getLastHeatReading(uid, teplomerId)

        if (response.success == 1) {
          val remoteReading = response.heatReading 

          emit(Resource.Success<HeatReadingEntity?>(remoteReading))

          // 3. ПЕРЕЗАПИСЬ КЭША
          if (remoteReading != null) {
            try {
              if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
                  meterCache.insertHeatReadings(listOf(remoteReading))
              }
            } catch (dbEx: Exception) {
              println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
            }
          }
        } else {
          println("[$className.$methodName]: [NETWORK_REJECT] Сервер повернул ошибку: ${response.message}")
          // Явно типизируем ошибку сервера под контракт потока
          emit(Resource.Error<HeatReadingEntity?>(message = response.message ?: "Помилка завантаження"))
        }

      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")
        ex.printStackTrace()

        // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
        val lastHope = meterCache.getLastHeatReadingByMeter(teplomerId)
        if (lastHope != null) {
          println("[$className.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
          emit(Resource.Success<HeatReadingEntity?>(lastHope))
        } else {
          SnackbarManager.showMessage("Відсутній зв'язок з сервером опалення")
          // Принудительно типизируем КМР-фабрику ошибки, закрывая Return type mismatch
          emit(Resource.Error<HeatReadingEntity?>(message = "Відсутній зв'язок та немає збережених даних"))
        }
      }
    }.flowOn(Dispatchers.Default) // Безопасный КМР-пул потоков корутин
}

