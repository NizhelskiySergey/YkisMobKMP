package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetApartment] — Сценарий получения данных о квартире.
 * Поддерживает стратегию кэширования (Сначала Кэш -> Запрос в сеть -> Обновление кэша).
 * Кроссплатформенно оперирует типами Long для addressId.
 */
class GetApartment(
    private val repository: ApartmentRepository,
  // Изменили тип аргумента с Int на Long, чтобы исключить Argument type mismatch в Koin
    private val getLocal: suspend (Long) -> ApartmentEntity? = { null },
    private val saveLocal: suspend (ApartmentEntity) -> Unit = {}
) {
  operator fun invoke(addressId: Long, uid: String): Flow<Resource<ApartmentEntity>> = flow {
      val methodName = "UseCase.GetApartment"

      try {
          emit(Resource.Loading())

          // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (SQLDelight)
          val cached = getLocal(addressId)
          if (cached != null) {
              println("[$methodName]: [LOCAL_HIT] Загружено из локальной БД для ID: $addressId")
              emit(Resource.Success(cached))
          }

          // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client)
          println("[$methodName]: [NETWORK_START] Запрос ID: $addressId, UID: ${uid.takeLast(5)}")

          // В репозитории метод getApartment тоже должен принимать Long для консистентности
          val response = repository.getApartment(addressId, uid)

          if (response.success == 1 && response.apartment != null) {
              // Прошиваем актуальный UID пользователя для связки данных
              val remoteApartment = response.apartment.copy(uid = uid)

              // Обновляем локальный кэш
              saveLocal(remoteApartment)

              println("[$methodName]: [NETWORK_SUCCESS] Данные обновлены в БД")
              emit(Resource.Success(remoteApartment))
          } else {
              // Если сеть ответила ошибкой, но у нас уже есть кэш — мы его уже отдали выше.
              // Ошибку шлем только если данных в базе нет совсем.
              if (cached == null) {
                  val errorMsg = response.message ?: "Дані про квартиру не знайдено"
                  println("[$methodName]: [SERVER_ERROR] $errorMsg")
                  emit(Resource.Error(message = errorMsg))
              }
          }

      } catch (ex: Exception) {
          println("[$methodName]: [FATAL_ERROR] ${ex.message}")

          // OFFLINE RECOVERY: Если произошел сбой сети (IOException/Timeout),
          // пробуем еще раз достать из кэша (на случай если Loading его перекрыл в UI)
          val lastHope = getLocal(addressId)
          if (lastHope != null) {
              println("[$methodName]: [OFFLINE_RECOVERY] Используем кэш из-за ошибки сети")
              emit(Resource.Success(lastHope))
          } else {
              emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
          }
      }
  }.flowOn(Dispatchers.Default) // Оставляем выполнение тяжелой фильтрации на пуле корутин
}
