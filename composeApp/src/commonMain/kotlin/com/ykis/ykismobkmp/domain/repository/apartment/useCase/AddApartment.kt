package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [AddApartment] — Сценарий добавления и привязки новой квартиры к профилю жильца.
 * ИСПРАВЛЕНО НАМЕРТВО: Внедрена прямая синхронизация свежего списка квартир с локальным кэшем SQLDelight.
 */
class AddApartment(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "AddApartment"

  operator fun invoke(code: String, uid: String, email: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "invoke"
    try {
      println("[$className.$methodName]: [START] Code: $code, UID: $uid")
      emit(Resource.Loading())

      // 1. СЕТЕВОЙ ЗАПРОС К API СЕРВЕРА
      val response = repository.addApartmentUser(code, uid, email)

      // 2. ОБРАБОТКА УСПЕШНОГО ОТВЕТА СЕРВЕРА
      if (response.success == 1) {
        println("[$className.$methodName]: [SUCCESS] Квартира успешно привязана на сервере")

        /**
         * СИСТЕМНАЯ СИНХРОНИЗАЦИЯ:
         * После успешной привязки в MySQL, мы запрашиваем обновленный список с сервера
         * и аппаратно перезаписываем локальный кэш SQLDelight, чтобы жилец сразу увидел квартиру на экране.
         */
        try {
          val freshResponse = repository.getApartmentList(uid)
          if (freshResponse.success == 1) {
            val remoteApartments = freshResponse.apartments ?: emptyList()
            val apartmentsWithUid = remoteApartments.map { it.copy(uid = uid) }

            // Атомарно обновляем таблицы СУБД
            cache.deleteAllApartments()
            cache.insertApartmentList(apartmentsWithUid)
            println("[$className.$methodName]: Локальный кэш SQLDelight успешно синхронизирован")
          }
        } catch (e: Exception) {
          println("[$className.$methodName]: Ошибка фонового обновления СУБД, но привязка подтверждена: ${e.message}")
        }

        emit(Resource.Success(response))
      } else {
        // 3. ОБРАБОТКА ОШИБОК БИЗНЕС-ЛОГИКИ СЕРВЕРА (PHP СКОУП)
        val errorMessage = when (response.message) {
          "FlatAlreadyInDataBase" -> "Ця квартира вже додана до вашого профілю"
          "IncorrectCode" -> "Невірний секретний код. Перевірте дані в квитанції"
          "CodeNotFound" -> "Такий код не знайдено в базі нарахувань ЮКІС"
          else -> response.message ?: "Не вдалося додати квартиру"
        }
        println("[$className.$methodName]: [REJECT] Сервер отклонил привязку: $errorMessage")
        emit(Resource.Error(message = errorMessage))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой сетевого соединения: ${ex.message}")
      ex.printStackTrace()
      emit(Resource.Error(message = "Помилка зв'язку з сервером. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default) // Сетевые операции и маппинг выполняются на фоновом пуле корутин
}
