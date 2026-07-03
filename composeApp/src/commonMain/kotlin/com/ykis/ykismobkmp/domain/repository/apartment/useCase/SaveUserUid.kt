package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ykismobkmp.composeapp.generated.resources.*

/**
 * [SaveUserUid] — Доменный сценарий первичной регистрации и сохранения UID/Email пользователя на сервере.
 */
private const val className = "SaveUserUid"

class SaveUserUid(
  private val repository: ApartmentRepository
) {

  /**
   * [invoke] — Первичная КМР-регистрация идентификатора пользователя на сервере ЮКІС.
   */
  operator fun invoke(uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "invoke"
    try {
      println("[YkisLogKMP.$className.$methodName]: [START] Регистрация UID в MySQL: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Мультиплатформенный Ktor через Репозиторий ЮКІС)
      val response = repository.saveUserUid(uid, email)

      println("[YkisLogKMP.$className.$methodName]: [RESPONSE] Сервер вернул статус success: ${response.success}")

      // 2. ОБРАБОТКА РЕЗУЛЬТАТА И МАРШАЛИНГ ОТВЕТОВ
      if (response.success == 1) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Идентификатор пользователя UID успешно запечатан в MySQL.")
        emit(Resource.Success(response))
      } else {
        println("[YkisLogKMP.$className.$methodName]: [REJECT] Сервер отклонил сохранение UID: ${response.message}")
        emit(Resource.Error(message = response.message))
      }

    } catch (ce: CancellationException) {
      // ИСПРАВЛЕНО НАМЕРТВО: Используется явный кроссплатформенный CancellationException,
      // что полностью исключает падение линкера при сборке под iOS и Mac Desktop!
      println("[YkisLogKMP.$className.$methodName]: [CANCELLED] Операция отменена областью видимости корутины Хаба.")
      throw ce
    } catch (ex: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [FATAL_ERROR] Критический сбой регистрации UID в базе Южного: ${ex.message}")

      // ИСПРАВЛЕНО: Платформозависимый printStackTrace заменен на безопасный КМР-вывод логгера
      emit(Resource.Error(message = "Generic error"))
    }
  }.flowOn(Dispatchers.Default) // Сетевой запрос и валидация выполняются в фоновом пуле корутин
}
