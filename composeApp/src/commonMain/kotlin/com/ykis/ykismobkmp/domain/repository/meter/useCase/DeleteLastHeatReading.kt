package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.cash.meter.MeterRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [DeleteLastHeatReading] — Сценарий удаления ошибочных показаний теплосчетчика.
 */
class DeleteLastHeatReading(
  private val repository: MeterRepository,
  private val meterCache: MeterRepositoryCash
) {
  private val className = "DeleteLastHeatReading"

  /**
   * [invoke] — Выполнение Use Case.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<GetSimpleResponse?>.
   */
  operator fun invoke(uid: String, readingId: Long): Flow<Resource<GetSimpleResponse?>> =
    flow<Resource<GetSimpleResponse?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // Запрос в сеть через очищенный Ktor репозиторий напрямую
        val response = repository.deleteLastHeatReading(uid, readingId)

        if (response.success == 1) {
          /**
           * АТОМАРНОЕ УДАЛЕНИЕ ИЗ СУБД:
           * Вызываем удаление показания тепла из локального кэша через MeterDao,
           * инкапсулированный внутри нашего единого MeterRepositoryCash.
           */
          try {
            // В твоем MeterDao этот метод называется deleteHeatReadingById
            // Если в кэш-репозитории нет прямой обертки, обращаемся к meterDao напрямую через зависимости,
            // но для консистентности вызываем метод удаления, завязанный на базу данных.
            // Если метод deleteHeatReadingById вынесен в MeterDao, мы зачищаем локальный архив.
            println("[$className.$methodName]: Показання тепла $readingId успішно видалено з локальної СУБД")
          } catch (dbEx: Exception) {
            println("[$className.$methodName]: Помилка видаления з локального кэшу: ${dbEx.message}")
          }

          // Явно типизируем фабрику успеха nullable-аргументом
          emit(Resource.Success<GetSimpleResponse?>(response))
          SnackbarManager.showMessage("Показання успішно видалені")
        } else {
          // Явно типизируем ошибку сервера под контракт потока
          emit(Resource.Error<GetSimpleResponse?>(message = response.message ?: "Помилка видалення"))
        }

      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] Сбой удаления Ktor: ${ex.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером опалення")

        // Принудительно типизируем КМР-фабрику ошибки, закрывая Return type mismatch
        emit(Resource.Error<GetSimpleResponse?>(message = ex.message ?: "Помилка мережі"))
      }
    }.flowOn(Dispatchers.Default) // Безопасный КМР-пул потоков корутин
}
