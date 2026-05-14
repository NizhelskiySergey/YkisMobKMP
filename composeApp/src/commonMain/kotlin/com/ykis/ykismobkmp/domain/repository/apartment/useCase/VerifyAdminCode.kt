package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class VerifyAdminCode(
  private val repository: ApartmentRepository
) {
  operator fun invoke(code: String, uid: String): Flow<Resource<GetSimpleResponse>> = flow {
      val methodName = "UseCase.VerifyAdminCode"

      try {
          Log.d("YkisLog", "[$methodName]: [START] Проверка кода для UID: $uid")
          emit(Resource.Loading())

          // 1. ЗАПРОС В СЕТЬ (Ktor)
          val response = repository.verifyAdminSecretWord(code, uid)

          Log.d(
              "YkisLog",
              "[$methodName]: [RESPONSE] Success: ${response.success}, Role: ${response.userRole}"
          )

          // 2. ОБРАБОТКА РЕЗУЛЬТАТА
          if (response.success == 1) {
              Log.i(
                  "YkisLog",
                  "[$methodName]: [SUCCESS] Доступ разрешен. Роль: ${response.userRole}"
              )
              emit(Resource.Success(response))
          } else {
              val errorMessage = "Невірний секретний код адміністратора"
              Log.w("YkisLog", "[$methodName]: [REJECT] $errorMessage")
              emit(Resource.Error(message = errorMessage))
          }

      } catch (ex: Exception) {
          Log.e("YkisLog", "[$methodName]: [FATAL] ${ex.message}")
          emit(Resource.Error(message = "Сервіс недоступний. Неможливо перевірити код"))
      }
  }.flowOn(Dispatchers.Default)
}
