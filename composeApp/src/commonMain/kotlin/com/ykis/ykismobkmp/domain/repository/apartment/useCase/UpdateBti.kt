package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [UpdateBti] — Сценарий обновления анкетных данных БТИ для лицевого счета.
 */
class UpdateBti(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "UpdateBti"

  operator fun invoke(uid:String,addressId: Long,phone: String,email: String): Flow<Resource<GetApartmentsResponse>> = flow {
    val methodName = "invoke"
    try {
      println("[$className.$methodName]: [START] ID лицевого счета: $addressId")
      emit(Resource.Loading())

      // 1. ВАЛИДАЦИЯ ПЕРЕД ЗАПРОСОМ (Принцип входного якоря)
      if (phone.isBlank() || email.isBlank()) {
        println("[$className.$methodName]: [ABORT] Попытка отправки пустых полей email/Контактов")
        emit(Resource.Error(message = "email та номер телефону не можуть бути порожніми"))
        return@flow
      }
      val response = repository.updateBti(
        uid= uid,
        addressId = addressId,
        phone = phone,
        email = email
      )

      if (response.success == 1) {
        println("[$className.$methodName]: [SUCCESS] Данные БТИ успешно обновлены на сервере")

        try {
          val oldApartment = cache.getApartmentById(addressId)
          if (oldApartment != null) {
            val updatedApartment = oldApartment.copy(
              addressId = addressId,
              phone = phone,
              email = email
            )
            cache.deleteFlat(addressId)
            cache.insertApartmentList(listOf(updatedApartment))
            println("[$className.$methodName]: Локальный дисковый кэш SQLDelight успешно синхронизирован")
          }
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Ошибка записи в СУБД, но сервер данные принял: ${dbEx.message}")
        }

        emit(Resource.Success(response))
      } else {
        println("[$className.$methodName]: [REJECT] Сервер отклонил изменения: ${response.message}")
        emit(Resource.Error(message = response.message ?: "Помилка оновлення даних БТІ"))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [EXCEPTION] Критическая ошибка сети или парсера: ${ex.message}")
      ex.printStackTrace()
      emit(Resource.Error(message = "Помилка мережі або сервера. Спробуйте пізніше"))
    }
  }.flowOn(Dispatchers.Default) // Фоновые транзакции и маппинг выполняются в Default пуле корутин
}
