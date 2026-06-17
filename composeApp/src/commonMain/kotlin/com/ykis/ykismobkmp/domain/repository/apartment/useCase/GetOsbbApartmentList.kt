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
 * [GetOsbbApartmentsList] — Сценарий получения списка квартир для ОСББ или конкретного дома ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Прямое использование ApartmentCache вместо функциональных лямбд.
 * Реализует глубокую синхронизацию (Очистка устаревших связанных таблиц + Массовая вставка в SQLDelight).
 */
class GetOsbbApartmentsList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetOsbbApartmentsList"

  operator fun invoke(targetId: Long, isHouseSearch: Boolean = false): Flow<Resource<List<ApartmentEntity>>> = flow {
    val type = if (isHouseSearch) "HOUSE" else "OSBB"
    val methodName = "invoke[$type]"

    try {
      println("[$className.$methodName]: [START] TargetID: $targetId")
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
      var localList: List<ApartmentEntity> = emptyList()
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            // В вебе ограничиваем ожидание БД, чтобы не вешать UI
            kotlinx.coroutines.withTimeoutOrNull(500) {
                localList = cache.getApartmentsByUser()
            }
        } else {
            localList = cache.getApartmentsByUser()
        }

        if (localList.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Найдено ${localList.size} кв.")
          emit(Resource.Success(localList))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ЗАПРОС В СЕТЬ
      val response = repository.getOsbbApartmentsList(targetId, isHouseSearch)
      val remoteApartments = response.apartments ?: emptyList()
      println("[$className.$methodName]: Парсинг успішно завершено: ${remoteApartments.size} квартир")

      if (remoteApartments.isNotEmpty()) {
        // КРИТИЧНИЙ ФІКС: Спочатку віддаємо дані в UI, щоб загасити лоадер!
        emit(Resource.Success(remoteApartments))

        println("[$className.$methodName]: Початок фонової синхронізації...")
        // Потім фоново оновлюємо кеш
        try {
          cache.deleteAllApartments()
          cache.insertApartmentList(remoteApartments)
          println("[$className.$methodName]: Локальна база даних успішно оновлена")
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Помилка кешування: ${dbEx.message}")
        }
      } else {
        println("[$className.$methodName]: [NETWORK_EMPTY] Порожня відповідь")
        if (localList.isEmpty()) emit(Resource.Success(emptyList()))
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой загрузки реестра квартир ОСББ: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: Если нет связи (offline-режим в городе Южном), аварийно возвращаем локальный список
      val fallback = cache.getApartmentsByUser()
      if (fallback.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_MODE] Сеть недоступна, переведено на локальный список")
        emit(Resource.Success(fallback))
      } else {
        emit(Resource.Error(message = "Відсутній зв'язок. Список мешканців недоступний."))
      }
    }
  }.flowOn(Dispatchers.Default) // Тяжелая дисковая очистка и маппинг выполняются на фоновом пуле корутин
}
