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

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web
      var localList: List<ApartmentEntity> = emptyList()
      try {
        localList = cache.getApartmentsByUser()
        if (localList.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Найдено ${localList.size} кв. в локальной базе данных")
          emit(Resource.Success(localList))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна (Web mode), завантажуємо з мережі")
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client через Репозиторий)
      val response = repository.getOsbbApartmentsList(targetId, isHouseSearch)
      val remoteApartments = response.apartments ?: emptyList()
      println("[$className.$methodName]: Парсинг успішно завершено: ${remoteApartments.size} квартир")

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ
      if (remoteApartments.isNotEmpty()) {
        try {
          // ИСПРАВЛЕНО: В Web-версии пока пропускаем запись в БД, так как воркер - заглушка
          if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              cache.deleteAllApartments()
              cache.insertApartmentList(remoteApartments)
          } else {
              println("[$className.$methodName]: Web mode: пропуск запису в БД (працюємо в ОЗУ)")
          }
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Ошибка перезаписи кэша: ${dbEx.message}")
        }
        emit(Resource.Success(remoteApartments))
      } else {
        println("[$className.$methodName]: [NETWORK_EMPTY] Получен пустой ответ от сервера")
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
