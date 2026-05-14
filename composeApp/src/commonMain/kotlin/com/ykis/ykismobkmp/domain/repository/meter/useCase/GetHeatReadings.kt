package com.ykis.ykismobkmp.domain.repository.meter.useCase


import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetHeatReadings] — Единый КМР-стандарт интерактора счетчиков тепла.
 * Полностью изолирован от базы данных через функциональные лямбды получения и сохранения кэша.
 */
class GetHeatReadings(
  private val repository: HeatMeterRepository,
  // Настраиваем лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> List<HeatReadingEntity> = { emptyList() },
  private val saveLocal: suspend (Long, List<HeatReadingEntity>) -> Unit = { _, _ -> }
) {
  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<List<HeatReadingEntity>>> = flow {
    val methodName = "UseCase.GetHeatReadings"
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов КМР-лямбды)
      val localReadings = getLocal(teplomerId)
      if (localReadings.isNotEmpty()) {
        println("[$methodName]: [LOCAL_HIT] Загружено из кэша для счетчика: $teplomerId")
        emit(Resource.Success(localReadings))
      }

      // 2. ЗАПРОС В СЕТЬ (Через очищенный репозиторий Ktor)
      println("[$methodName]: [NETWORK_START] Запрос счетчика: $teplomerId, UID: ${uid.takeLast(5)}")
      val response = repository.getHeatReadings(uid, teplomerId)

      if (response.success == 1) {
        val remoteReadings = response.heatReadings ?: emptyList()

        // Перезаписываем локальный кэш через лямбду
        saveLocal(teplomerId, remoteReadings)

        println("[$methodName]: [NETWORK_SUCCESS] Показания тепла обновлены в СУБД")
        emit(Resource.Success(remoteReadings))
      } else {
        // Если сеть вернула ошибку, но кэш пустой — шлем ошибку в UI
        if (localReadings.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Помилка завантаження даних"))
        }
      }

    } catch (ex: Exception) {
      println("[$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")

      // OFFLINE RECOVERY: При ошибке сети отдаем кэш как последнюю надежду
      val lastHope = getLocal(teplomerId)
      if (lastHope.isNotEmpty()) {
        println("[$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
        emit(Resource.Success(lastHope))
      } else {
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених показань"))
      }
    }
  }.flowOn(Dispatchers.Default) // Тяжелая обработка и маппинг выполняются на общем КМР пуле потоков
}


