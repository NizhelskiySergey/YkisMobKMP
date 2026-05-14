package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.GetLastWaterReading"

/**
 * [GetLastWaterReading] — Единый КМР-стандарт интерактора получения последнего показания водомера г. Южный.
 * Полностью изолирован от баз данных и типизирован под nullable-генерики для устранения Return type mismatch.
 */
class GetLastWaterReading(
  private val repository: WaterMeterRepository,
  // Настраиваем лямбды работы с локальным кэшем SQLDelight через сквозной тип Long
  private val getLocal: suspend (Long) -> WaterReadingEntity? = { null },
  private val saveLocal: suspend (WaterReadingEntity) -> Unit = {}
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: vodomerId переведен из Int на Long под КМР-стандарт СУБД.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<WaterReadingEntity?>.
   */
  operator fun invoke(uid: String, vodomerId: Long): Flow<Resource<WaterReadingEntity?>> =
    flow<Resource<WaterReadingEntity?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        // Шлем стейт лоадера в UI
        emit(Resource.Loading())

        // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов быстрой КМР-лямбды LIMIT 1)
        val cachedReading = getLocal(vodomerId)
        if (cachedReading != null) {
          println("[$tag.$methodName]: [LOCAL_HIT] Загружено последнее показание для водомера: $vodomerId")
          // ИСПРАВЛЕНО: Явно кастим к nullable для 100% совпадения вложенных генериков KMP
          emit(Resource.Success<WaterReadingEntity?>(cachedReading))
        }

        // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
        println("[$tag.$methodName]: [NETWORK_START] Запрос по ID: $vodomerId, UID: ${uid.takeLast(5)}")
        val response = repository.getLastWaterReading(uid, vodomerId)

        if (response.success == 1) {
          val remoteReading = response.waterReading // Тип из Ktor модели: WaterReadingEntity?

          // ИСПРАВЛЕНО: Явно типизируем фабрику успеха nullable-аргументом
          emit(Resource.Success<WaterReadingEntity?>(remoteReading))

          // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через лямбду при наличии объекта)
          if (remoteReading != null) {
            saveLocal(remoteReading)
            println("[$tag.$methodName]: [NETWORK_SUCCESS] База данных SQLDelight синхронизирована с сетью")
          }
        } else {
          // ИСПРАВЛЕНО: Явно типизируем ошибку сервера под контракт потока
          emit(Resource.Error<WaterReadingEntity?>(message = response.message ?: "Помилка завантаження"))
        }

      } catch (ex: Exception) {
        println("[$tag.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")

        // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
        val lastHope = getLocal(vodomerId)
        if (lastHope != null) {
          println("[$tag.$methodName]: [OFFLINE_RECOVERY] Успешный возврат кэша при сбое сети")
          emit(Resource.Success<WaterReadingEntity?>(lastHope))
        } else {
          SnackbarManager.showMessage("Відсутній зв'язок з сервером водопостачання")
          // ИСПРАВЛЕНО: Принудительно типизируем КМР-фабрику ошибки, закрывая Return type mismatch
          emit(Resource.Error<WaterReadingEntity?>(message = "Відсутній зв'язок та немає збережених даних"))
        }
      }
    }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}
