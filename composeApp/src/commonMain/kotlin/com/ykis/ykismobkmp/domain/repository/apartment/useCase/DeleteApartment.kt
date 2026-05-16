package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [DeleteApartment] — Сценарий отвязки/удаления лицевого счета.
 * Синхронизирует удаление квартиры на сервере и каскадно очищает данные в репозитории.
 * Кроссплатформенно оперирует типами Long для addressId.
 */
class DeleteApartment(
  private val repository: ApartmentRepository
) {
  operator fun invoke(addressId: Long, uid: String): Flow<Resource<GetSimpleResponse>> = flow {
      val methodName = "UseCase.DeleteApartment"

      try {
          println("[$methodName]: [START] Видалення ID: $addressId, UID: ${uid.takeLast(5)}")
          emit(Resource.Loading())

          // 1. ЗАПРОС В СЕТЬ (Ktor через Репозиторий)
          // Репозиторий принимает Long и сам каскадно зачистит локальный кэш при успехе
          val response = repository.deleteApartment(uid,addressId, )

          if (response.success == 1) {
              println("[$methodName]: [SUCCESS] Удалено на сервере и в локальной БД")
              emit(Resource.Success(response))
          } else {
              println("[$methodName]: [REJECT] ${response.message}")
              emit(Resource.Error(message = response.message ?: "Помилка видалення"))
          }

      } catch (ex: Exception) {
          println("[$methodName]: [FATAL_ERROR] ${ex.message}")
          emit(Resource.Error(message = "Сервіс недоступний. Перевірте підключення до мережі"))
      }
  }.flowOn(Dispatchers.Default) // Сетевые операции и маппинг ответов выполняем в фоновом пуле
}
