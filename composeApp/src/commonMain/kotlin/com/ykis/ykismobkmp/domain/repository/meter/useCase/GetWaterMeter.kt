package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.GetWaterWaterList"

/**
 * [GetWaterMeterList] — Доменный Use Case для загрузки списков водомеров г. Южный.
 * Полностью изолирован от базы данных через функциональные КМР-лямбды локального получения и сохранения кэша.
 */
class GetWaterMeterList(
  private val repository: WaterMeterRepository,
  // Настраиваем лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> List<WaterMeterEntity> = { emptyList() },
  private val saveLocal: suspend (Long, List<WaterMeterEntity>) -> Unit = { _, _ -> }
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: addressId переведен из Int на Long под КМР-стандарт СУБД и баз данных.
   */
  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<WaterMeterEntity>>> = flow {
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов КМР-лямбды)
      val localMeters = getLocal(addressId)
      if (localMeters.isNotEmpty()) {
        println("[$tag]: [LOCAL_HIT] Загружено из кэша для адреса: $addressId")
        emit(Resource.Success(localMeters))
      }

      // 2. ЗАПРОС В СЕТЬ (Через очищенный Ktor-репозиторий напрямую)
      println("[$tag]: [NETWORK_START] Запрос водомеров по ID: $addressId, UID: ${uid.takeLast(5)}")
      val response = repository.getWaterMeterList(uid, addressId)

      if (response.success == 1) {
        val remoteMeters = response.waterMeters ?: emptyList()
        println("[$tag]: [NETWORK_SUCCESS] Сеть вернула ${remoteMeters.size} водомеров")

        // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через транзакционную лямбду)
        saveLocal(addressId, remoteMeters)

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remoteMeters))
      } else {
        // Если сеть ответила ошибкой, но у нас уже есть локальный кэш — мы его отдали выше.
        // Ошибку шлем только если данных в базе нет совсем.
        if (localMeters.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Прилади обліку води не знайдені"))
        }
      }

    } catch (ex: Exception) {
      println("[$tag]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")

      // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
      val lastHope = getLocal(addressId)
      if (lastHope.isNotEmpty()) {
        println("[$tag]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
        emit(Resource.Success(lastHope))
      } else {
        SnackbarManager.showMessage("Відсутній зв'язок з сервером водопостачання")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}
