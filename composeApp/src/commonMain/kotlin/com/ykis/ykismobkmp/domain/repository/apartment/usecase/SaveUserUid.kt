package com.ykis.ykismobkmp.domain.repository.apartment.usecase

// composeApp/src/commonMain/kotlin/com/ykis/ykismobkmp/domain/usecase/SaveUserUid.kt

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SaveUserUid(
  private val repository: ApartmentRepository,
) {
  operator fun invoke(uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "UseCase.SaveUserUid"
    try {
      Log.d("YkisLog", "[$methodName]: [START] Регистрация UID: $uid")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Мультиплатформенный Ktor)
      val response = repository.saveUserUid(uid, email)

      Log.d("YkisLog", "[$methodName]: [RESPONSE] Success: ${response.success}")

      // 2. ОБРАБОТКА РЕЗУЛЬТАТА
      if (response.success == 1) {
        Log.i("YkisLog", "[$methodName]: [SUCCESS] UID сохранен")
        emit(Resource.Success(response))
      } else {
        val errorMessage = when (response.message) {
          "UserUIdExist" -> "Цей ідентифікатор вже зареєстрований"
          "SaveUserUidError" -> "Помилка збереження на сервері"
          else -> response.message ?: "Помилка реєстрації пристрою"
        }
        Log.w("YkisLog", "[$methodName]: [REJECT] $errorMessage")
        emit(Resource.Error(message = errorMessage))
      }

    } catch (ce: CancellationException) {
      // В KMP важно пробрасывать для корректной отмены Flow в Compose
      Log.w("YkisLog", "[$methodName]: [CANCELLED]")
      throw ce
    } catch (ex: Exception) {
      Log.e("YkisLog", "[$methodName]: [FATAL] ${ex.message}")
      emit(Resource.Error(message = "Сервіс недоступний. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default)
}







