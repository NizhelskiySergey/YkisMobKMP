package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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
      println("[YkisLogKMP.$className.$methodName]: [START] Реєстрація UID в MySQL: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Мультиплатформенный Ktor через Репозиторий ЮКІС)
      val response = repository.saveUserUid(uid, email)

      println("[YkisLogKMP.$className.$methodName]: [RESPONSE] Сервер повернув статус success: ${response.success}")

      // 2. ОБРАБОТКА РЕЗУЛЬТАТА И МАРШАЛИНГ ОТВЕТОВ
      if (response.success == 1) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Ідентифікатор користувача UID успішно запечатано в MySQL.")
        emit(Resource.Success(response))
      } else {
        val errorMessage = when (response.message) {
          "UserUIdExist"     -> "Цей ідентифікатор вже зареєстрований"
          "SaveUserUidError" -> "Помилка збереження на сервері"
          else               -> response.message ?: "Помилка реєстрації пристрою"
        }
        println("[YkisLogKMP.$className.$methodName]: [REJECT] Сервер відхилив збереження UID: $errorMessage")
        emit(Resource.Error(message = errorMessage))
      }

    } catch (ce: kotlinx.coroutines.CancellationException) {
      // ИСПРАВЛЕНО НАМЕРТВО: Используется явный кроссплатформенный CancellationException,
      // что полностью исключает падение линкера при сборке под iOS и Mac Desktop!
      println("[YkisLogKMP.$className.$methodName]: [CANCELLED] Операція скасована областю видимості корутини Хаба.")
      throw ce
    } catch (ex: Exception) {
      println("[YkisLogKMP.$className.$methodName]: [FATAL_ERROR] Критичний збій реєстрації UID у базі Южного: ${ex.message}")

      // ИСПРАВЛЕНО: Платформозависимый printStackTrace заменен на безопасный КМР-вывод логгера
      emit(Resource.Error(message = "Сервіс недоступний. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default) // Сетевой запрос и валидация выполняются в фоновом пуле корутин
}
