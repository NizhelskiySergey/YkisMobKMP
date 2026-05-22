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

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов выровнен по новому имени контракта)
      val localReadings = meterCache.getWaterReadingsByMeter(vodomerId)
      if (localReadings.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Загружено из кэша для водомера: $vodomerId")
        emit(Resource.Success(localReadings))
      }

      // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
      println("[$className.$methodName]: [NETWORK_START] Запрос истории по ID: $vodomerId, UID: ${uid.takeLast(5)}")
      val response = repository.getWaterReadings(uid, vodomerId)

      if (response.success == 1) {
        val remoteReadings = response.waterReadings ?: emptyList()
        println("[$className.$methodName]: [NETWORK_SUCCESS] Сеть вернула ${remoteReadings.size} записей истории")

        // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через транзакции базы данных)
        try {
          // ИСПРАВЛЕНО НАМЕРТВО: Передаём чистый одиночный Long напрямую без listOf()!
          meterCache.deleteWaterReadingsByMeter(vodomerId)
          meterCache.insertWaterReadings(remoteReadings)
          println("[$className.$methodName]: Локальные показания воды успешно синхронизированы в СУБД")
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Ошибка записи показаний воды в СУБД: ${dbEx.message}")
        }

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remoteReadings))
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

