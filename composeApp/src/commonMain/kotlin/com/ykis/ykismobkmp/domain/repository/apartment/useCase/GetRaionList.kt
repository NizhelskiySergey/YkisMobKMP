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

      // ЭТАП 1: ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Запрашиваем районы из SQLDelight через ApartmentCache)
      val localRaions = try {
        cache.getRaionList()
      } catch (e: Exception) {
        println("[$className.$methodName]: Ошибка чтения кэша районов: ${e.message}")
        emptyList()
      }

      if (localRaions.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Найдено ${localRaions.size} районов в кэше")
        emit(Resource.Success(localRaions))
      }

      // ЭТАП 2: ЗАПРОС В СЕТЬ (Ktor HTTP Client через Репозиторий)
      println("[$className.$methodName]: [NETWORK_START] Запрос списка районов для UID: ${uid.takeLast(5)}")

      // Получаем полноценный GetRaionsResponse вместо сырого списка
      val response = repository.getRaionList(uid)
      val remoteRaions = response.raions ?: emptyList()

      // ЭТАП 3: ОБНОВЛЕНИЕ БАЗЫ ДАННЫХ И СИНХРОНИЗАЦИЯ
      if (response.success == 1 && remoteRaions.isNotEmpty()) {
        println("[$className.$methodName]: [DB_WRITE] Синхронизация данных в локальной БД")

        try {
          // Атомарно сохраняем новые районы в кэш SQLDelight
          cache.syncRaionList(remoteRaions)
          println("[$className.$methodName]: Локальная база данных районов успешно синхронизирована")
        } catch (dbEx: Exception) {
          println("[$className.$methodName]: Ошибка записи районов в СУБД: ${dbEx.message}")
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
