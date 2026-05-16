package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetHouseList] — Сценарий получения списка домов по району.
 * Поддерживает стратегию кэширования: сначала кэш из SQLDelight, затем обновление через Ktor.
 * Кроссплатформенно оперирует типами Long для raionId.
 */
class GetHouseList(
    private val repository: ApartmentRepository,
  // Изменили тип аргумента с Int на Long для бесшовной интеграции с SQLDelight
    private val getLocal: suspend (Long) -> List<HouseEntity> = { emptyList() },
    private val saveLocal: suspend (List<HouseEntity>) -> Unit = {}
) {
  operator fun invoke(raionId: Long): Flow<Resource<List<HouseEntity>>> = flow {
      val methodName = "UseCase.GetHouseList"

      try {
          emit(Resource.Loading())

          // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (SQLDelight через лямбду)
          val localHouses = getLocal(raionId)
          if (localHouses.isNotEmpty()) {
              println("[$methodName]: [LOCAL_HIT] Найдено ${localHouses.size} домов в кэше")
              emit(Resource.Success(localHouses))
          }

          // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client)
          println("[$methodName]: [NETWORK_START] Запрос для района ID: $raionId")

          // В репозитории метод getHouseByRaionList также переводим на параметр Long
          val response = repository.getHouseByRaionList(raionId)
          val remoteHouses = response.houses ?: emptyList()

          // 3. ОБНОВЛЕНИЕ БАЗЫ ДАННЫХ И СИНХРОНИЗАЦИЯ
          if (remoteHouses.isNotEmpty()) {
              // Прошиваем raionId перед сохранением
              val housesWithRaion = remoteHouses.map { it.copy(raionId = raionId) }

              // Сохраняем в кэш (в Koin тут будет транзакция SQLDelight)
              saveLocal(housesWithRaion)

              // Отдаем актуальный список (уже отсортированный базой данных)
              val updatedList = getLocal(raionId)
              println("[$methodName]: [NETWORK_SUCCESS] База синхронизирована")
              emit(Resource.Success(updatedList))
          } else if (localHouses.isEmpty()) {
              println("[$methodName]: [EMPTY] Домов на сервере не найдено")
              emit(Resource.Success(emptyList()))
          }

      } catch (ex: Exception) {
          println("[$methodName]: [FATAL_ERROR] ${ex.message}")

          // OFFLINE RECOVERY: Если сеть упала, пробуем выдать локальный кэш
          val fallback = getLocal(raionId)
          if (fallback.isNotEmpty()) {
              println("[$methodName]: [OFFLINE_MODE] Сеть недоступна, используем локальные данные")
              emit(Resource.Success(fallback))
          } else {
              emit(Resource.Error(message = "Сервіс недоступний. Список будинків недоступний."))
          }
      }
  }.flowOn(Dispatchers.Default) // Оставляем выполнение тяжелой фильтрации на пуле корутин
}
