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
 * [GetWaterReadings] — Единый КМР-стандарт интерактора истории показаний водомеров г. Южный.
 */
class GetWaterReadings(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetWaterReadings"

  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: vodomerId переведен из Int на Long под КМР-стандарт СУБД и репозиториев.
   */
  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<List<WaterReadingEntity>>> = flow {
    val methodName = "invoke"
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
      var localReadings: List<WaterReadingEntity> = emptyList()
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            kotlinx.coroutines.withTimeoutOrNull(500) {
                localReadings = meterCache.getWaterReadingsByMeter(vodomerId)
            }
        } else {
            localReadings = meterCache.getWaterReadingsByMeter(vodomerId)
        }

        if (localReadings.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Загружено из кэша")
          emit(Resource.Success(localReadings))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
      println("[$className.$methodName]: [NETWORK_START] ID: $vodomerId")
      val response = repository.getWaterReadings(uid, vodomerId)

      if (response.success == 1) {
        val remoteReadings = response.waterReadings ?: emptyList()
        println("[$className.$methodName]: [NETWORK_SUCCESS] Отримано ${remoteReadings.size} записів")

        // Спочатку UI
        emit(Resource.Success(remoteReadings))

        // 3. ПЕРЕЗАПИСЬ КЭША
        try {
          meterCache.deleteWaterReadingsByMeter(vodomerId)
          meterCache.insertWaterReadings(remoteReadings)
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
        }
      } else {
        println("[$className.$methodName]: [NETWORK_REJECT] Сервер вернул ошибку: ${response.message}")
        // Если сеть ответила ошибкой, но у нас уже есть локальный кэш — мы его отдали выше.
        // Ошибку шлем только если данных в базе нет совсем.
        if (localReadings.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Історію показань не знайдено"))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
      val lastHope = meterCache.getWaterReadingsByMeter(vodomerId)
      if (lastHope.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
        emit(Resource.Success(lastHope))
      } else {
        SnackbarManager.showMessage("Відсутній зв'язок з сервером водопостачання")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених показань"))
      }
    }
  }.flowOn(Dispatchers.Default) // Тяжелая обработка и маппинг выполняются на общем КМР пуле потоков
}

