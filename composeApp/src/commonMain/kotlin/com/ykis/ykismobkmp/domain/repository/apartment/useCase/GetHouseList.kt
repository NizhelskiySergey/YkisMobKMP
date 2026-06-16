package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [GetHouseList] — Доменный сценарий получения списка домов по району ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Использование ApartmentCache напрямую вместо функциональных лямбд.
 * Поддерживает стратегию кэширования: сначала локальный кэш из SQLDelight, затем обновление через Ktor.
 */
class GetHouseList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetHouseList"

  operator fun invoke(raionId: Long): Flow<Resource<List<HouseEntity>>> = flow {
    val methodName = "invoke"

    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
      var localHouses: List<HouseEntity> = emptyList()
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            withTimeoutOrNull(500) {
                localHouses = cache.getHousesByRaion(raionId)
            }
        } else {
            localHouses = cache.getHousesByRaion(raionId)
        }

        if (localHouses.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Знайдено ${localHouses.size} будинків")
          emit(Resource.Success(localHouses))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client через Репозиторий)
      println("[$className.$methodName]: [NETWORK_START] ID: $raionId")
      val response = repository.getHouseByRaionList(raionId)
      val remoteHouses = response.houses ?: emptyList()

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ
      if (remoteHouses.isNotEmpty()) {
        val housesWithRaion = remoteHouses.map { it.copy(raionId = raionId) }
        try {
          if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              cache.syncHouseList(housesWithRaion)
          }
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Помилка запису в кеш: ${dbEx.message}")
        }
        emit(Resource.Success(housesWithRaion))
      } else if (localHouses.isEmpty()) {
        println("[$className.$methodName]: [EMPTY] Домов для района $raionId на сервере не найдено")
        emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой загрузки справочника домов: ${ex.message}")
      ex.printStackTrace()

      // ЭТАП 4: OFFLINE RECOVERY
      val fallback = try {
        cache.getHousesByRaion(raionId)
      } catch (e: Exception) {
        emptyList()
      }

      if (fallback.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_MODE] Сеть недоступна, используются локальные данные")
        emit(Resource.Success(fallback))
      } else {
        emit(Resource.Error(message = "Сервіс недоступний. Список будинків недоступний."))
      }
    }
  }.flowOn(Dispatchers.Default)
}
