package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.GetLastHeatReading"

/**
 * [GetLastHeatReading] — Единый КМР-стандарт интерактора получения последнего показания теплосчетчика г. Южный.
 * Изолирован от СУБД через функциональные лямбды, застрахован от Return type mismatch.
 */
class GetLastHeatReading(
  private val repository: HeatMeterRepository,
  // Настраиваем КМР-лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> HeatReadingEntity? = { null },
  private val saveLocal: suspend (HeatReadingEntity) -> Unit = {}
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: teplomerId переведен из Int на Long под КМР-стандарт СУБД.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<HeatReadingEntity?>.
   */
  operator fun invoke(uid: String, teplomerId: Long): Flow<Resource<HeatReadingEntity?>> =
    flow<Resource<HeatReadingEntity?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов быстрой КМР-лямбды LIMIT 1)
        val cachedReading = getLocal(teplomerId)
        if (cachedReading != null) {
          println("[$tag.$methodName]: [LOCAL_HIT] Загружено последнее показание тепла для счетчика: $teplomerId")
          emit(Resource.Success<HeatReadingEntity?>(cachedReading))
        }

        // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
        println("[$tag.$methodName]: [NETWORK_START] Запрос по ID: $teplomerId, UID: ${uid.takeLast(5)}")
        val response = repository.getLastHeatReading(uid, teplomerId)

        if (response.success == 1) {
          val remoteReading = response.heatReading // Тип из Ktor модели: HeatReadingEntity?

          // ИСПРАВЛЕНО: Явно типизируем фабрику успеха nullable-аргументом
          emit(Resource.Success<HeatReadingEntity?>(remoteReading))

          // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через лямбду при наличии объекта)
          if (remoteReading != null) {
            saveLocal(remoteReading)
            println("[$tag.$methodName]: [NETWORK_SUCCESS] База данных SQLDelight синхронизирована с сетью")
          }
        } else {
          // ИСПРАВЛЕНО: Явно типизируем ошибку сервера под контракт потока
          emit(Resource.Error<HeatReadingEntity?>(message = response.message ?: "Помилка завантаження"))
        }

      } catch (ex: Exception) {
        println("[$tag.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")

        // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
        val lastHope = getLocal(teplomerId)
        if (lastHope != null) {
          println("[$tag.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
          emit(Resource.Success<HeatReadingEntity?>(lastHope))
        } else {
          SnackbarManager.showMessage("Відсутній зв'язок з сервером опалення")
          // ИСПРАВЛЕНО: Принудительно типизируем КМР-фабрику ошибки, закрывая Return type mismatch
          emit(Resource.Error<HeatReadingEntity?>(message = "Відсутній зв'язок та немає збережених даних"))
        }
      }
    }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}

