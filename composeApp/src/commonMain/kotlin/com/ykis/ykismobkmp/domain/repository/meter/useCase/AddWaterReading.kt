package com.ykis.ykismobkmp.domain.repository.meter.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.meter.MeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [AddWaterReading] — Сценарий передачи новых показаний кубометров водомера в расчетный центр.
 */
class AddWaterReading(
  private val repository: MeterRepository
) {
  private val className = "AddWaterReading"

  /**
   * [invoke] — Выполнение Use Case через передачу индивидуальных базовых параметров.
   * ЯВНО ТИПИЗИРОВАНО: Возвращает Flow строго с типом Resource<GetSimpleResponse?>.
   */
  operator fun invoke(
    uid: String,
    vodomerId: Long,
    currentValue: Long,
    newValue: Long
  ): Flow<Resource<GetSimpleResponse?>> =
    flow<Resource<GetSimpleResponse?>> { // Принудительно задаем тип контекста всего потока
      val methodName = "invoke"
      try {
        emit(Resource.Loading())

        // ИСПРАВЛЕНО НАМЕРТВО: Прямой проброс базовых типов в обновленный репозиторий без упаковки в params!
        val response = repository.addWaterReading(
          uid = uid,
          vodomerId = vodomerId,
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

      } catch (ex: Exception) {
        println("[$className.$methodName]: [FATAL_ERROR] Сбой Ktor при добавлении показаний воды: ${ex.message}")
        SnackbarManager.showMessage("Помилка зв'язку з сервером водопостачання")

        // Принудительно типизируем КМР-фабрику ошибки под контракт потока
        emit(Resource.Error<GetSimpleResponse?>(message = ex.message ?: "Помилка мережі"))
      }
    }.flowOn(Dispatchers.Default) // Кроссплатформенный пул потоков корутин
}
