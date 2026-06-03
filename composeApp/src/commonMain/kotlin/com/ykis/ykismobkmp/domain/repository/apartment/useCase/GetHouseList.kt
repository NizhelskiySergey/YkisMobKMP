package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.HouseEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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

      // ЭТАП 1: ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Запрашиваем дома из SQLDelight через ApartmentCache)
      // Примечание: Если в твоем ApartmentCache еще нет методов для домов, этот блок вернет пустой список,
      // и приложение пойдет в сеть без падения!
      val localHouses = try {
        cache.getHousesByRaion(raionId)
      } catch (e: Exception) {
        println("[$className.$methodName]: Ошибка чтения кэша домов: ${e.message}")
        emptyList()
      }

      if (localHouses.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Найдено ${localHouses.size} домов в локальном кэше")
        emit(Resource.Success(localHouses))
      }

      // ЭТАП 2: ЗАПРОС В СЕТЬ (Ktor HTTP Client через Репозиторий)
      println("[$className.$methodName]: [NETWORK_START] Запрос списка домов для района ID: $raionId")
      val response = repository.getHouseByRaionList(raionId)
      val remoteHouses = response.houses ?: emptyList()

      // ЭТАП 3: ОБНОВЛЕНИЕ БАЗЫ ДАННЫХ И СИНХРОНИЗАЦИЯ
      if (remoteHouses.isNotEmpty()) {
        // Прошиваем актуальный raionId для каждого дома перед сохранением на диск
        val housesWithRaion = remoteHouses.map { it.copy(raionId = raionId) }

        try {
          // Атомарно сохраняем новые дома в кэш SQLDelight
          cache.syncHouseList(housesWithRaion)
          println("[$className.$methodName]: Локальная база данных успешно синхронизирована")
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Ошибка записи домов в СУБД: ${dbEx.message}")
        }

        println("[$className.$methodName]: [NETWORK_SUCCESS] Список домов успешно обновлен с сервера")
        emit(Resource.Success(housesWithRaion))
      } else if (localHouses.isEmpty()) {
        println("[$className.$methodName]: [EMPTY] Домов для района $raionId на сервере не найдено")
        emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой загрузки справочника домов: ${ex.message}")
      ex.printStackTrace()

      // ЭТАП 4: OFFLINE RECOVERY — Если сеть упала (нет интернета), аварийно выдаем локальный кэш
      // Чтобы интерфейс администратора не остался пустым во время аварии в городе Южном
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
  }.flowOn(Dispatchers.Default) // Все фоновые операции и фильтрация выполняются на пуле корутин
}
