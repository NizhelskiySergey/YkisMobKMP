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
 * [DeleteApartment] — Сценарий отвязки/удаления лицевого счета квартиры.
 * ИСПРАВЛЕНО НАМЕРТВО: Внедрена прямая зачистка локального кэша SQLDelight при успешном удалении.
 * Синхронизирует удаление квартиры на сервере и каскадно очищает данные на диске устройства.
 */
class DeleteApartment(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "DeleteApartment"

  operator fun invoke(addressId: Long, uid: String): Flow<Resource<GetSimpleResponse>> = flow {
    val methodName = "invoke"

    try {
      println("[$className.$methodName]: [START] Видалення ID: $addressId, UID: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Ktor через Репозиторий)
      val response = repository.deleteApartment(uid, addressId)

      if (response.success == 1) {
        /**
         * АТОМАРНАЯ ЗАЧИСТКА ДИСКА:
         * Раз сервер подтвердил удаление, мы мгновенно выбиваем эту квартиру из локальной СУБД,
         * приводя Long к типу Int для сигнатуры нашего ApartmentCache.deleteFlat
         */
        try {
          cache.deleteFlat(addressId)
          println("[$className.$methodName]: Квартира каскадно удалена из локального кэша SQLDelight")
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Ошибка удаления из СУБД, но сетевая отвязка прошла: ${dbEx.message}")
        }

        println("[$className.$methodName]: [SUCCESS] Удалено на сервере и очищено в локальной БД")
        emit(Resource.Success(response))
      } else {
        println("[$className.$methodName]: [REJECT] Сервер отклонил удаление: ${response.message}")
        emit(Resource.Error(message = response.message ?: "Помилка видалення"))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой сетевого соединения: ${ex.message}")
      ex.printStackTrace()
      emit(Resource.Error(message = "Сервіс недоступний. Перевірте підключення до мережі"))
    }
  }.flowOn(Dispatchers.Default) // Сетевые операции и зачистка СУБД выполняются в фоновом пуле корутин
}
