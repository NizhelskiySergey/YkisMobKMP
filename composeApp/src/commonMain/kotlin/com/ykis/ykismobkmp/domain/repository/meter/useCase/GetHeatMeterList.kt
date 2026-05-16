package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.GetHeatMeterList"

/**
 * [GetHeatMeterList] — Доменный Use Case для загрузки списков счетчиков тепла г. Южный.
 * Изолирован от баз данных через функциональные КМР-лямбды локального получения и сохранения кэша.
 */
class GetHeatMeterList(
  private val repository: HeatMeterRepository,
  // Настраиваем лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> List<HeatMeterEntity> = { emptyList() },
  private val saveLocal: suspend (Long, List<HeatMeterEntity>) -> Unit = { _, _ -> }
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: addressId переведен из Int на Long под КМР-стандарт СУБД.
   */
  operator fun invoke(uid: String, addressId: Long): Flow<Resource<List<HeatMeterEntity>>> = flow {
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов КМР-лямбды)
      val localMeters = getLocal(addressId)
      if (localMeters.isNotEmpty()) {
        println("[$tag]: [LOCAL_HIT] Загружено из кэша для адреса: $addressId")
        emit(Resource.Success(localMeters))
      }

      // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий)
      println("[$tag]: [NETWORK_START] Запрос ID: $addressId, UID: ${uid.takeLast(5)}")
      val response = repository.getHeatMeterList(uid, addressId)

      if (response.success == 1) {
        val remoteMeters = response.heatMeters ?: emptyList()
        println("[$tag]: [NETWORK_SUCCESS] Сеть вернула ${remoteMeters.size} счетчиков")

        // Перезаписываем локальный кэш через лямбду
        saveLocal(addressId, remoteMeters)

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remoteMeters))
      } else {
        // Если сеть вернула ошибку, но у нас уже есть локальный кэш — мы его уже отдали выше.
        // Ошибку шлем только если данных в базе нет совсем.
        if (localMeters.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Прилади обліку не знайдені"))
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
        SnackbarManager.showMessage("Відсутній зв'язок з сервером тепла")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}

