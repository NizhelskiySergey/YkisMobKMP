package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetApartmentList] — Доменный сценарий получения и синхронизации списка квартир жильца.
 * Реализует паттерн «Сначала локальный кэш -> Запрос в сеть -> Атомарная перезапись SQLDelight».
 */
class GetApartmentList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetApartmentList"

  operator fun invoke(uid: String): Flow<Resource<List<ApartmentEntity>>> = flow {
    val methodName = "invoke"

    if (uid.isBlank()) {
      println("[$className.$methodName]: [ABORT] Передан пустой идентификатор пользователя UID")
      emit(Resource.Error("Помилка авторизації"))
      return@flow
    }

    try {
      // ЭТАП 1: Выставляем стейт загрузки для UI
      emit(Resource.Loading())

      // ЭТАП 2: БЫСТРЫЙ СТАРТ — Извлекаем из SQLDelight кэш и фильтруем по UID пользователя
      val localList = cache.getApartmentsByUser().filter { it.uid == uid }
      if (localList.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Выведено ${localList.size} квартир из локального кэша")
        emit(Resource.Success(localList))
      }

      // ЭТАП 3: ОБНОВЛЕНИЕ — Идем через Ktor HTTP-клиент на удаленный сервер ЮКИС
      println("[$className.$methodName]: [NETWORK_START] Запрос свежих данных для UID: ${uid.takeLast(5)}")
      val response: GetApartmentsResponse = repository.getApartmentList(uid)

      if (response.success == 1) {
        val remoteApartments = response.apartments ?: emptyList()

        // Прошиваем UID пользователя для каждой полученной квартиры для корректной фильтрации в БД
        val apartmentsWithUid = remoteApartments.map { it.copy(uid = uid) }

        // ЭТАП 4: АТОМАРНАЯ СИНХРОНИЗАЦИЯ — Очищаем старые записи и сохраняем новые через трансляцию DAO
        cache.deleteAllApartments()
        cache.insertApartmentList(apartmentsWithUid)

        println("[$className.$methodName]: [SYNC_SUCCESS] Кэш СУБД успешно обновлен (${apartmentsWithUid.size} кв.)")
        emit(Resource.Success(apartmentsWithUid))
      } else {
        println("[$className.$methodName]: [NETWORK_REJECT] Сервер вернул статус ошибки: ${response.message}")
        // Если сервер ответил отказом, но на первом этапе мы уже нашли кэш — UI останется заполненным
        if (localList.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Не вдалося отримати список"))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Каскадный сбой сети или парсера: ${ex.message}")
      ex.printStackTrace()

      // ЭТАП 5: OFFLINE MODE — Если интернета нет, экстренно поднимаем данные из SQLDelight повторно
      val fallbackList = cache.getApartmentsByUser().filter { it.uid == uid }
      if (fallbackList.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_MODE] Сеть недоступна, приложение переведено на локальный архив")
        emit(Resource.Success(fallbackList))
      } else {
        emit(Resource.Error(message = "Сервіс недоступний. Перевірте підключення до інтернету"))
      }
    }
  }.flowOn(Dispatchers.Default) // Фильтрация и маппинг списков выполняются в фоновом пуле корутин
}
