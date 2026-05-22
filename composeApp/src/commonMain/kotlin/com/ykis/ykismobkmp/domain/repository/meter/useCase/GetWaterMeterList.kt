package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetWaterMeterList] — Доменный Use Case для загрузки списков водомеров г. Южный.
 */
class GetWaterMeterList(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "GetWaterMeterList"

  /**
   * [invoke] — Выполнение Use Case.
   * addressId и идентификаторы намертво зафиксированы под КМР-стандарт Long.
   */
  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<WaterMeterEntity>>> = flow {
    val methodName = "invoke"
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов выровнен по новому имени контракта)
      val localMeters = meterCache.getWaterMetersByApartment(addressId)
      if (localMeters.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Загружено из кэша для адреса: $addressId")
        emit(Resource.Success(localMeters))
      }

      // 2. ЗАПРОС В СЕТЬ (Через очищенный Ktor-репозиторий напрямую)
      println("[$className.$methodName]: [NETWORK_START] Запрос водомеров по ID: $addressId, UID: ${uid.takeLast(5)}")
      val response = repository.getWaterMeterList(uid, addressId)

      if (response.success == 1) {
        val remoteMeters = response.waterMeters ?: emptyList()
        println("[$className.$methodName]: [NETWORK_SUCCESS] Сеть вернула ${remoteMeters.size} водомеров")

        // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через транзакции базы данных)
        try {
          // ИСПРАВЛЕНО НАМЕРТВО: Передаём чистый одиночный Long напрямую без listOf()!
          meterCache.deleteWaterMetersByApartment(addressId)
          meterCache.insertWaterMeter(remoteMeters)
          println("[$className.$methodName]: Локальная база данных водомеров успешно синхронизирована")
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Ошибка записи водомеров в СУБД: ${dbEx.message}")
        }

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remoteMeters))
      } else {
        println("[$className.$methodName]: [NETWORK_REJECT] Сервер вернул ошибку: ${response.message}")
        // Если сеть ответила ошибкой, но у нас уже есть локальный кэш — мы его отдали выше.
        // Ошибку шлем только если данных в базе нет совсем.
        if (localMeters.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Прилади обліку води не знайдені"))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
      val lastHope = meterCache.getWaterMetersByApartment(addressId)
      if (lastHope.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
        emit(Resource.Success(lastHope))
      } else {
        SnackbarManager.showMessage("Відсутній зв'язок з сервером водопостачання")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // Безопасный КМР-пул потоков корутин
}

