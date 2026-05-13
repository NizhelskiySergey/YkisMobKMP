package com.ykis.ykismobkmp.domain.repository.apartment.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.core.utils.Resource // Убедись в точности пути к твоему Resource
import com.ykis.ykismobkmp.domain.repository.apartment.ApartmentRepository

/**
 * [GetRaionList] — Сценарий получения списка районов.
 * Полностью синхронизирован с кроссплатформенной структурой ответа GetRaionsResponse.
 */
class GetRaionList(
  private val repository: ApartmentRepository,
  // Лямбды для изоляции бизнес-логики от деталей реализации БД (внедряются через Koin)
  private val getLocal: suspend () -> List<RaionEntity> = { emptyList() },
  private val saveLocal: suspend (List<RaionEntity>) -> Unit = {}
) {
  operator fun invoke(uid: String): Flow<Resource<List<RaionEntity>>> = flow {
    val methodName = "UseCase.GetRaionList"

    try {
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (SQLDelight)
      val localRaions = getLocal()
      if (localRaions.isNotEmpty()) {
        println("[$methodName]: [LOCAL_HIT] Найдено ${localRaions.size} районов в кэше")
        emit(Resource.Success(localRaions))
      }

      // 2. ЗАПРОС В СЕТЬ (Ktor HTTP Client)
      println("[$methodName]: [NETWORK_START] Запрос списка районов для UID: ${uid.takeLast(5)}")

      // Получаем полноценный GetRaionsResponse вместо сырого списка
      val response = repository.getRaionList(uid)
      val remoteRaions = response.raions ?: emptyList()

      // 3. ОБНОВЛЕНИЕ БАЗЫ ДАННЫХ И СИНХРОНИЗАЦИЯ
      if (response.success == 1 && remoteRaions.isNotEmpty()) {
        println("[$methodName]: [DB_WRITE] Синхронизация данных в локальной БД")

        // Сохраняем в кэш (в Koin эта операция обернута в SQLDelight транзакцию)
        saveLocal(remoteRaions)

        // Отдаем актуальный отсортированный список, перечитав его из БД
        val updatedList = getLocal()
        emit(Resource.Success(updatedList))
      } else {
        // Если сервер вернул ошибку/success=0, но локально что-то было — мы это уже отдали.
        // Ошибку кидаем только если в базе пусто и сеть не вернула валидных данных.
        if (localRaions.isEmpty()) {
          val errorMsg = response.message.ifBlank { "Районів на сервере не знайдено" }
          println("[$methodName]: [SERVER_REJECT] $errorMsg")
          emit(Resource.Error(message = errorMsg))
        }
      }

    } catch (ex: Exception) {
      println("[$methodName]: [FATAL_ERROR] ${ex.message}")

      // OFFLINE RECOVERY: Если произошел сбой сети, аварийно отдаем локальный кэш
      val fallback = getLocal()
      if (fallback.isNotEmpty()) {
        println("[$methodName]: [OFFLINE_MODE] Сеть недоступна, используем локальные данные")
        emit(Resource.Success(fallback))
      } else {
        emit(Resource.Error(message = "Сервіс недоступний. Список районів недоступний."))
      }
    }
  }.flowOn(Dispatchers.Default) // Тяжелый маппинг списков оставляем на фоне, не фризим UI на Mac/Android
}
