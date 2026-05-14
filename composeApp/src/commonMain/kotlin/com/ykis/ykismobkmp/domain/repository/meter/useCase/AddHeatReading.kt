package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.AddHeatReadingParams
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

private const val tag = "UseCase.AddHeatReading"

/**
 * [AddHeatReading] — Единый КМР-стандарт интерактора передачи новых показаний теплосчетчика в расчетный центр.
 * Полностью автономен, застрахован от Return type mismatch и готов к компиляции под Mac Desktop.
 */
class AddHeatReading(
  private val repository: HeatMeterRepository
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<GetSimpleResponse?>.
   */
  operator fun invoke(addReadingParams: AddHeatReadingParams): Flow<Resource<GetSimpleResponse?>> =
    flow<Resource<GetSimpleResponse?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // Отправка новых гигакалорий через Ktor репозиторий напрямую
        val response = repository.addHeatReading(addReadingParams)

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
        println("[$tag.$methodName]: Сетевой сбой Ktor сервера тепла: ${e.response.status.value}")
        SnackbarManager.showMessage("Помилка сервера тепла: ${e.response.status.value}")
        emit(Resource.Error<GetSimpleResponse?>(message = "Помилка сервера: ${e.response.status.value}"))
      } catch (ex: Exception) {
        println("[$tag.$methodName]: [FATAL_ERROR] Сбой Ktor при добавлении: ${ex.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером опалення")

        // Принудительно типизируем КМР-фабрику ошибки под контракт потока
        emit(Resource.Error<GetSimpleResponse?>(message = ex.message ?: "Помилка мережі"))
      }
    }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Кроссплатформенный пул потоков вместо Dispatchers.IO
}
