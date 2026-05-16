package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.meter.MeterReadingsParams
import com.ykis.ykismobkmp.domain.repository.meter.WaterMeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val tag = "UseCase.AddWaterReading"

/**
 * [AddWaterReading] — Единый КМР-стандарт интерактора передачи новых показаний водомера в расчетный центр.
 * Полностью автономен, застрахован от Return type mismatch и готов к компиляции под Mac Desktop.
 */
class AddWaterReading(
  private val repository: WaterMeterRepository
) {
  /**
   * [invoke] — Выполнение Use Case.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<GetSimpleResponse?>.
   */
  operator fun invoke(meterReadingsParams: MeterReadingsParams): Flow<Resource<GetSimpleResponse?>> =
    flow<Resource<GetSimpleResponse?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // Отправка новых кубометров через Ktor репозиторий напрямую
        val response = repository.addWaterReading(meterReadingsParams)

        if (response.success == 1) {
          // ЯВНО ТИПИЗИРУЕМ: Указываем генерик для Success, исключая mismatch типов
          emit(Resource.Success<GetSimpleResponse?>(response))
          SnackbarManager.showMessage("Показання успішно додані")
        } else {
          // Показываем сообщение об ошибке от бэкенда расчетного центра г. Южный
          emit(Resource.Error<GetSimpleResponse?>(message = response.message ?: "Помилка додавання"))
          SnackbarManager.showMessage(response.message ?: "Помилка білінгу")
        }

      } catch (ex: Exception) {
        println("[$tag.$methodName]: [FATAL_ERROR] Сбой Ktor при добавлении: ${ex.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером водопостачання")

        // Принудительно типизируем КМР-фабрику ошибки под контракт потока
        emit(Resource.Error<GetSimpleResponse?>(message = ex.message ?: "Помилка мережі"))
      }
    }.flowOn(Dispatchers.Default) // ИСПРАВЛЕНО: Кроссплатформенный пул потоков вместо Dispatchers.IO
}
