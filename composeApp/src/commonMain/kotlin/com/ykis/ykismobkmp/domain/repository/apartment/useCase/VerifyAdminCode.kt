package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [VerifyAdminCode] — Сценарий верификации секретного слова администратора ОСМД.
 * Проверяет права доступа в MySQL базе через Ktor-клиент и возвращает верифицированную роль.
 */
class VerifyAdminCode(
  private val repository: ApartmentRepository
) {
  private val className = "VerifyAdminCode"

  operator fun invoke(code: String, uid: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "invoke"

    try {
      println("[$className.$methodName]: [START] Проверка секретного кода для UID: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ ЧЕРЕЗ МУЛЬТИПЛАТФОРМЕННЫЙ РЕПОЗИТОРИЙ
      val response = repository.verifyAdminSecretWord(uid, code)

      println("[$className.$methodName]: [RESPONSE] Success: ${response.success}, Role: ${response.userRole}")

      // 2. ОБРАБОТКА РЕЗУЛЬТАТА ВАЛИДАЦИИ
      if (response.success == 1) {
        println("[$className.$methodName]: [SUCCESS] Административный доступ разрешен. Назначенная роль: ${response.userRole}")
        emit(Resource.Success(response))
      } else {
        val errorMessage = "Невірний секретний код адміністратора"
        println("[$className.$methodName]: [REJECT] Отказ авторизации: $errorMessage")
        emit(Resource.Error(message = errorMessage))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой проверки административного токена: ${ex.message}")
      ex.printStackTrace()
      emit(Resource.Error(message = "Сервіс недоступний. Неможливо перевірити код"))
    }
  }.flowOn(Dispatchers.Default) // Сетевой запрос и парсинг роли выполняются на фоновом пуле корутин
}
