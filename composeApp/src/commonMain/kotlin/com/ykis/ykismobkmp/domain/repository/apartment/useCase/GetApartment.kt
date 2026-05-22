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

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (SQLDelight транслируется через кэш)
      // Приводим Long к Int для соответствия сигнатуре твоего ApartmentCache.getApartmentById
      val cached = cache.getApartmentById(addressId)
      if (cached != null) {
        println("[$className.$methodName]: [LOCAL_HIT] Загружено из локальной БД для ID: $addressId")
        emit(Resource.Success(cached))
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client)
      println("[$className.$methodName]: [NETWORK_START] Запрос ID: $addressId, UID: ${uid.takeLast(5)}")
      val response = repository.getApartment(uid, addressId)

      if (response.success == 1 && response.apartment != null) {
        // Прошиваем актуальный UID пользователя для связки данных
        val remoteApartment = response.apartment.copy(uid = uid)

        // Обновляем локальный кэш: удаляем старую запись (если была) и пишем свежую
        cache.deleteFlat(addressId)
        cache.insertApartmentList(listOf(remoteApartment))

        println("[$className.$methodName]: [NETWORK_SUCCESS] Данные обновлены в локальной БД")
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
