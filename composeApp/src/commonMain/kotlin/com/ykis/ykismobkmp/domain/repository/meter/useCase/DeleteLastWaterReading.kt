package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.DeleteLastWaterReading"

/**
 * [DeleteLastWaterReading] — Единый КМР-стандарт интерактора удаления ошибочных показаний водомера.
 * Полностью изолирован от базы данных через функциональные лямбды, застрахован от Return type mismatch.
 */
class DeleteLastWaterReading(
  private val repository: WaterMeterRepository,
  // Настраиваем КМР-лямбду удаления строки из локального кэша SQLDelight по первичному ключу pokId (Long)
  private val deleteLocal: suspend (Long) -> Unit = {}
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ИСПРАВЛЕНО: readingId переведен из Int на Long под КМР-стандарт СУБД.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<GetSimpleResponse?>.
   */
  operator fun invoke(uid: String, readingId: Long): Flow<Resource<GetSimpleResponse?>> =
    flow<Resource<GetSimpleResponse?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // Запрос в сеть через очищенный Ktor репозиторий напрямую
        val response = repository.deleteLastWaterReading(uid, readingId)

        if (response.success == 1) {
          // ИСПРАВЛЕНО: Удаление из локального кэша SQLDelight через реактивную лямбду
          deleteLocal(readingId)
          println("[$tag.$methodName]: Показання $readingId успішно видалено з локальної СУБД")

          // ИСПРАВЛЕНО: Явно типизируем фабрику успеха nullable-аргументом
          emit(Resource.Success<GetSimpleResponse?>(response))
          SnackbarManager.showMessage("Показання успішно видалені")
        } else {
          // ИСПРАВЛЕНО: Явно типизируем ошибку сервера под контракт потока
          emit(Resource.Error<GetSimpleResponse?>(message = response.message ?: "Помилка видалення"))
        }

      } catch (ex: Exception) {
        println("[$tag.$methodName]: [FATAL_ERROR] Сбой удаления Ktor: ${ex.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером водопостачання")

        // ИСПРАВЛЕНО: Принудительно типизируем КМР-фабрику ошибки, закрывая Return type mismatch
        emit(Resource.Error<GetSimpleResponse?>(message = ex.message ?: "Помилка мережі"))
      }
    }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Безопасный КМР-пул потоков вместо Dispatchers.IO
}

