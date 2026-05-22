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
class SaveUserUid(
  private val repository: ApartmentRepository
) {
  private val className = "SaveUserUid"

  /**
   * [invoke] — Первичная КМР-регистрация идентификатора пользователя на сервере ЮКИС.
   */
  operator fun invoke(uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "invoke"
    try {
      println("[$className.$methodName]: [START] Регистрация UID: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Мультиплатформенный Ktor через Репозиторий)
      val response = repository.saveUserUid(uid, email)

      println("[$className.$methodName]: [RESPONSE] Success: ${response.success}")

      // 2. ОБРАБОТКА РЕЗУЛЬТАТА
      if (response.success == 1) {
        println("[$className.$methodName]: [SUCCESS] Идентификатор пользователя UID успешно сохранен")
        emit(Resource.Success(response))
      } else {
        val errorMessage = when (response.message) {
          "UserUIdExist" -> "Цей ідентифікатор вже зареєстрований"
          "SaveUserUidError" -> "Помилка збереження на сервері"
          else -> response.message ?: "Помилка реєстрації пристрою"
        }
        println("[$className.$methodName]: [REJECT] Сервер отклонил сохранение UID: $errorMessage")

        // Использован твой оригинальный именованный параметр message =
        emit(Resource.Error(message = errorMessage))
      }

    } catch (ce: CancellationException) {
      // КРИТИЧНО ДЛЯ КОРУТИН: CancellationException обязательно пробрасываем дальше!
      println("[$className.$methodName]: [CANCELLED] Операция отменена областью видимости корутины")
      throw ce
    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Критический сбой регистрации UID: ${ex.message}")
      ex.printStackTrace()

      // Использован твой оригинальный именованный параметр message =
      emit(Resource.Error(message = "Сервіс недоступний. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default) // Сетевой запрос и валидация выполняются в фоновом пуле корутин
}
