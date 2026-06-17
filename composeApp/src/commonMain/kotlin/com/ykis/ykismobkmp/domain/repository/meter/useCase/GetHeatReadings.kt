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
 * [GetHeatReadings] — Единый КМР-стандарт интерактора счетчиков тепла.
 */
class GetHeatReadings(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetHeatReadings"

  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<List<HeatReadingEntity>>> = flow {
    val methodName = "invoke"
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
      var localReadings: List<HeatReadingEntity> = emptyList()
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            kotlinx.coroutines.withTimeoutOrNull(500) {
                localReadings = meterCache.getHeatReadingsByMeter(teplomerId)
            }
        } else {
            localReadings = meterCache.getHeatReadingsByMeter(teplomerId)
        }

        if (localReadings.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Загружено из кэша")
          emit(Resource.Success(localReadings))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ЗАПРОС В СЕТЬ (Через очищенный репозиторий Ktor)
      println("[$className.$methodName]: [NETWORK_START] ID: $teplomerId")
      val response = repository.getHeatReadings(uid, teplomerId)

      if (response.success == 1) {
        val remoteReadings = response.heatReadings ?: emptyList()
        println("[$className.$methodName]: [NETWORK_SUCCESS] Отримано ${remoteReadings.size} записів")

        // Спочатку UI
        emit(Resource.Success(remoteReadings))

        // 3. ПЕРЕЗАПИСЬ КЭША
        try {
          meterCache.deleteHeatReadingsByMeter(teplomerId)
          meterCache.insertHeatReadings(remoteReadings)
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
        }
      } else {
        println("[$className.$methodName]: [NETWORK_REJECT] Server rejected request: ${response.message}")
        // Если сеть вернула ошибку, но кэш пустой — шлем ошибку в UI
        if (localReadings.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Помилка завантаження даних"))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: При ошибке сети отдаем кэш как последнюю надежду
      val lastHope = meterCache.getHeatReadingsByMeter(teplomerId)
      if (lastHope.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
        emit(Resource.Success(lastHope))
      } else {
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених показань"))
      }
    }
  }.flowOn(Dispatchers.Default) // Тяжелая обработка и маппинг выполняются на общем КМР пуле потоков
}

