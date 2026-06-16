package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetApartment] — Сценарий получения данных о конкретной квартире.
 * ИСПРАВЛЕНО НАМЕРТВО: Прямая работа с ApartmentCache вместо функциональных лямбд.
 * Поддерживает стратегию кэширования (Сначала Кэш -> Запрос в сеть -> Обновление кэша в SQLDelight).
 */
class GetApartment(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetApartment"

  operator fun invoke(uid: String, addressId: Long): Flow<Resource<ApartmentEntity>> = flow {
    val methodName = "invoke"

    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
      var cached: ApartmentEntity? = null
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            kotlinx.coroutines.withTimeoutOrNull(500) {
                cached = cache.getApartmentById(addressId)
            }
        } else {
            cached = cache.getApartmentById(addressId)
        }

        if (cached != null) {
          println("[$className.$methodName]: [LOCAL_HIT] Загружено из кэша")
          emit(Resource.Success(cached!!))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client)
      println("[$className.$methodName]: [NETWORK_START] ID: $addressId")
      val response = repository.getApartment(uid, addressId)

      if (response.success == 1 && response.apartment != null) {
        val remoteApartment = response.apartment.copy(uid = uid)

        // 3. ПЕРЕЗАПИСЬ КЭША
        try {
          if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              cache.deleteFlat(addressId)
              cache.insertApartmentList(listOf(remoteApartment))
          }
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Помилка запису в кеш: ${dbEx.message}")
        }

        emit(Resource.Success(remoteApartment))
      } else {
        println("[$className.$methodName]: [SERVER_ERROR] Сервер вернул ошибку или пустой объект: ${response.message}")
        // Если сеть ответила ошибкой, но у нас уже есть кэш — мы его уже отдали выше.
        // Ошибку шлем только если данных в базе нет совсем.
        if (cached == null) {
          val errorMsg = response.message ?: "Дані про квартиру не знайдено"
          emit(Resource.Error(message = errorMsg))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой сети или базы данных: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: Если произошел сбой сети (IOException/Timeout),
      // пробуем еще раз достать из кэша (на случай если Loading его перекрыл в UI)
      val lastHope = cache.getApartmentById(addressId)
      if (lastHope != null) {
        println("[$className.$methodName]: [OFFLINE_RECOVERY] Используем локальный кэш из-за ошибки сети")
        emit(Resource.Success(lastHope))
      } else {
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // Фильтрация и маппинг выполняются в фоновом пуле корутин
}
