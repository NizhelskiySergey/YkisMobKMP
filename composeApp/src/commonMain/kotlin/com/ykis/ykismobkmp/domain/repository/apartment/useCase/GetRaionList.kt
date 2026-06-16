package com.ykis.ykismobkmp.domain.repository.apartment.useCase

import com.ykis.ykismobkmp.cash.apartment.ApartmentCache
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetRaionList] — Сценарий получения списка районов города Южного.
 * ИСПРАВЛЕНО НАМЕРТВО: Использование ApartmentCache напрямую вместо функциональных лямбд.
 * Полностью синхронизирован с кроссплатформенной структурой ответа GetRaionsResponse.
 */
class GetRaionList(
  private val repository: ApartmentRepository,
  private val cache: ApartmentCache
) {
  private val className = "GetRaionList"

  operator fun invoke(uid: String): Flow<Resource<List<RaionEntity>>> = flow {
    val methodName = "invoke"

    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША - Безопасно для Web (с таймаутом)
      var localRaions: List<RaionEntity> = emptyList()
      try {
        if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
            kotlinx.coroutines.withTimeoutOrNull(500) {
                localRaions = cache.getRaionList()
            }
        } else {
            localRaions = cache.getRaionList()
        }

        if (localRaions.isNotEmpty()) {
          println("[$className.$methodName]: [LOCAL_HIT] Знайдено ${localRaions.size} районів")
          emit(Resource.Success(localRaions))
        }
      } catch (e: Exception) {
        println("[$className.$methodName]: Локальна БД недоступна або зависла, йдемо в мережу")
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client через Репозиторий)
      println("[$className.$methodName]: [NETWORK_START] ID: ${uid.takeLast(5)}")
      val response = repository.getRaionList(uid)
      val remoteRaions = response.raions ?: emptyList()

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ
      if (response.success == 1 && remoteRaions.isNotEmpty()) {
        try {
          if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              cache.syncRaionList(remoteRaions)
          }
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Помилка запису в кеш: ${dbEx.message}")
        }

        emit(Resource.Success(remoteRaions))
      } else {
        // Если сервер вернул ошибку/success=0, но локально что-то было — мы это уже отдали.
        // Ошибку кидаем только если в базе пусто и сеть не вернула валидных данных.
        if (localRaions.isEmpty()) {
          val errorMsg = response.message?.ifBlank { "Районів на сервері не знайдено" } ?: "Районів на сервері не знайдено"
          println("[$className.$methodName]: [SERVER_REJECT] $errorMsg")
          emit(Resource.Error(message = errorMsg))
        }
      }

    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сбой загрузки справочника районов: ${ex.message}")
      ex.printStackTrace()

      // ЭТАП 4: OFFLINE RECOVERY: Если произошел сбой сети, аварийно отдаем локальный кэш
      val fallback = try {
        cache.getRaionList()
      } catch (e: Exception) {
        emptyList()
      }

      if (fallback.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_MODE] Network down, переключено на локальные данные районов")
        emit(Resource.Success(fallback))
      } else {
        emit(Resource.Error(message = "Сервіс недоступний. Список районів недоступний."))
      }
    }
  }.flowOn(Dispatchers.Default) // Тяжелый маппинг списков оставляем на фоне, не фризим UI на Mac/Android
}
