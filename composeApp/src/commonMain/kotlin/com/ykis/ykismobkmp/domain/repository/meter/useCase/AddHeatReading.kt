package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

/**
 * [AddHeatReading] — Сценарий передачи новых показаний гигакалорий теплосчетчика в расчетный центр.
 */
class AddHeatReading(
  private val repository: MeterRepository
) {
  private val className = "AddHeatReading"

  /**
   * [invoke] — Выполнение Use Case через передачу индивидуальных параметров.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<GetSimpleResponse?>.
   */
  operator fun invoke(
    uid: String,
    teplomerId: Long,
    currentValue: Double,
    newValue: Double
  ): Flow<Resource<GetSimpleResponse?>> =
    flow<Resource<GetSimpleResponse?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // ИСПРАВЛЕНО НАМЕРТВО: Прямой проброс базовых типов в обновленный репозиторий без упаковки в params!
        val response = repository.addHeatReading(
          uid = uid,
          teplomerId = teplomerId,
          currentValue = currentValue,
          newValue = newValue
        )

        if (response.success == 1) {
          // ЯВНО ТИПИЗИРУЕМ: Указываем генерик для Success, исключая mismatch типов
          emit(Resource.Success<GetSimpleResponse?>(response))
          SnackbarManager.showMessage("Показання успішно додані")
        } else {
          // Показываем сообщение об ошибке от бэкенда расчетного центра г. Южный
          emit(Resource.Error<GetSimpleResponse?>(message = response.message ?: "Помилка додавання"))
          SnackbarManager.showMessage(response.message ?: "Помилка білінгу")
        }

      } catch (e: ResponseException) {
        // Перехват сетевых ошибок Ktor (например, 403 или 500)
        println("[$className.$methodName]: Сетевой сбой Ktor сервера тепла: ${e.response.status.value}")
        SnackbarManager.showMessage("Помилка сервера тепла: ${e.response.status.value}")

        // Передача ошибки штатно через именованный параметр message
        emit(Resource.Error<GetSimpleResponse?>(message = "Помилка сервера: ${e.response.status.value}"))
      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] Сбой Ktor при добавлении: ${ex.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером опалення")

        // Принудительно типизируем КМР-фабрику ошибки под контракт потока
        emit(Resource.Error<GetSimpleResponse?>(message = ex.message ?: "Помилка мережі"))
      }
    }.flowOn(Dispatchers.Default) // Кроссплатформенный пул потоков корутин
}
