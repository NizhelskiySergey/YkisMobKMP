package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AddApartment(
    private val repository: ApartmentRepository,
) {
  operator fun invoke(code: String, uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
      val methodName = "UseCase.AddApartment"
      try {
          Log.d("YkisLog", "[$methodName]: [START] Code: $code, UID: $uid")
          emit(Resource.Loading())

          // 1. СЕТЕВОЙ ЗАПРОС
          val response = repository.addApartmentUser(code, uid, email)

          // 2. ОБРАБОТКА УСПЕХА
          if (response.success == 1) {
              Log.i("YkisLog", "[$methodName]: [SUCCESS] Квартира привязана")

              /**
               * СИСТЕМНАЯ ВСТАВКА:
               * После успешной привязки в MySQL, мы запрашиваем обновленный список,
               * чтобы ApartmentRepositoryImpl сохранил новую квартиру в SQLDelight.
               */
              try {
                  repository.getApartmentList(uid)
                  Log.d("YkisLog", "[$methodName]: Локальная база синхронизирована")
              } catch (e: Exception) {
                  Log.w("YkisLog", "[$methodName]: Ошибка фоновой синхронизации, но привязка ок")
              }

              emit(Resource.Success(response))
          } else {
              // 3. ОБРАБОТКА ОШИБОК ЛОГИКИ (из PHP)
              val errorMessage = when (response.message) {
                  "FlatAlreadyInDataBase" -> "Ця квартира вже додана до вашого профілю"
                  "IncorrectCode" -> "Невірний секретний код. Перевірте дані в квитанції"
                  "CodeNotFound" -> "Такий код не знайдено в базі нарахувань"
                  else -> response.message ?: "Не вдалося додати квартиру"
              }
              Log.w("YkisLog", "[$methodName]: [REJECT] $errorMessage")
              emit(Resource.Error(message = errorMessage))
          }

      } catch (ex: Exception) {
          Log.e("YkisLog", "[$methodName]: [FATAL] ${ex.message}")
          emit(Resource.Error(message = "Помилка зв'язку з сервером. Спробуйте пізніше"))
      }
  }.flowOn(Dispatchers.Default)
}
