package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.GetWaterReadings"

/**
 * [GetWaterReadings] — Единый КМР-стандарт интерактора истории показаний водомеров г. Южный.
 * Полностью изолирован от базы данных через функциональные лямбды получения и сохранения кэша.
 */
class GetWaterReadings(
  private val repository: WaterMeterRepository,
  // Настраиваем лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> List<WaterReadingEntity> = { emptyList() },
  private val saveLocal: suspend (Long, List<WaterReadingEntity>) -> Unit = { _, _ -> }
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: vodomerId переведен из Int на Long под КМР-стандарт СУБД и репозиториев.
   */
  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<List<WaterReadingEntity>>> = flow {
    val methodName = "invoke"
    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов КМР-лямбды)
      val localReadings = getLocal(vodomerId)
      if (localReadings.isNotEmpty()) {
        println("[$tag.$methodName]: [LOCAL_HIT] Загружено из кэша для водомера: $vodomerId")
        emit(Resource.Success(localReadings))
      }

      // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
      println("[$tag.$methodName]: [NETWORK_START] Запрос истории по ID: $vodomerId, UID: ${uid.takeLast(5)}")
      val response = repository.getWaterReadings(uid, vodomerId)

      if (response.success == 1) {
        val remoteReadings = response.waterReadings ?: emptyList()
        println("[$tag.$methodName]: [NETWORK_SUCCESS] Сеть вернула ${remoteReadings.size} записей истории")

        // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через транзакционную лямбду)
        saveLocal(vodomerId, remoteReadings)

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remoteReadings))
      } else {
        // Если сеть ответила ошибкой, но у нас уже есть локальный кэш — мы его отдали выше.
        // Ошибку шлем только если данных в базе нет совсем.
        if (localReadings.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Історію показань не знайдено"))
        }
      }

    } catch (ex: Exception) {
      println("[$tag.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")

      // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
      val lastHope = getLocal(vodomerId)
      if (lastHope.isNotEmpty()) {
        println("[$tag.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
        emit(Resource.Success(lastHope))
      } else {
        SnackbarManager.showMessage("Відсутній зв'язок з сервером водопостачання")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених показань"))
      }
    }
  }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}

