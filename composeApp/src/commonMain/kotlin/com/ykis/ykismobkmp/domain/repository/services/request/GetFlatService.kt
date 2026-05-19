package com.ykis.ykismobkmp.domain.repository.services.request

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.services.ServiceParams
import com.ykis.ykismobkmp.domain.repository.services.ServiceRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.text.ifBlank

private const val className = "GetFlatServices"

/**
 * [GetFlatServices] — Кроссплатформенный сценарий (Use Case) получения детализации начислений ЮКИС.
 * ИСПРАВЛЕНО: Вызовы конструктора Resource.Error переведены на оригинальный позиционный стандарт.
 */
class GetFlatServices(
  private val repository: ServiceRepository,
  // Явно типизируем пустой КМР-список по умолчанию как List<ServiceEntity> во избежание сбоев Any
  private val getLocal: suspend (Long, String, String) -> List<ServiceEntity> = { _, _, _ -> emptyList<ServiceEntity>() },
  private val saveLocal: suspend (List<ServiceEntity>) -> Unit = {}
) {

  /**
   * [invoke] — Точка реактивного вызова бизнес-сценария.
   * Логирование рантайма согласно правилу [Класс.Метод].
   */
  operator fun invoke(params: ServiceParams): Flow<Resource<List<ServiceEntity>>> = flow {
    val methodName = "invoke"

    // Переводим строковый маркер службы ГИОЦ в читаемый текстовый КМР-индекс
    val currentServiceType = when (params.service) {
      1.toByte() -> "voda"
      2.toByte() -> "teplo"
      3.toByte() -> "tbo"
      else -> "kv"
    }

    try {
      emit(Resource.Loading())

      // 1. БЫСТРЫЙ СТАРТ: Сначала мгновенно выдаем то, что уже закэшировано в локальной базе данных
      val cached: List<ServiceEntity> = getLocal(params.addressId, currentServiceType, params.year)
      if (cached.isNotEmpty()) {
        println("[$className.$methodName]: [LOCAL_HIT] Найдено ${cached.size} записей начислений в кэше")
        emit(Resource.Success(cached))
      }

      // 2. ОБНОВЛЕНИЕ: Запрашиваем свежий список через репозиторий (Ktor-клиент)
      println("[$className.$methodName]: [NETWORK_START] Запрос начислений для квартиры ID: ${params.addressId}")

      val response = repository.getFlatDetailService(
        ServiceParams(
          uid = params.uid,
          addressId = params.addressId,
          houseId = params.houseId,
          year = params.year,
          service = params.service,
          total = params.total,
        )
      )

      // 3. АТОМАРНАЯ СИНХРОНИЗАЦИЯ КЭША СУБД
      if (response.success == 1 && response.services.isNotEmpty()) {
        val remoteServices: List<ServiceEntity> = response.services

        saveLocal(remoteServices)

        println("[$className.$methodName]: [SUCCESS] Локальный кэш услуг успешно обновлен")
        emit(Resource.Success(remoteServices))
      } else {
        if (cached.isEmpty()) {
          val errorMsg = response.message.ifBlank { "Дані про нарахування заборгованостей відсутні" }
          println("[$className.$methodName]: [SERVER_REJECT] $errorMsg")

          // ИСПРАВЛЕНО: Префикс 'errorMessage =' удален. Строка передана позиционно, как требует твой оригинальный Resource.kt
          emit(Resource.Error<List<ServiceEntity>>(errorMsg))
        }
      }

    } catch (e: ResponseException) {
      // Ktor выбрасывает это при HTTP-ошибках статусов сервера 4xx, 5xx и т.д.
      println("[$className.$methodName]: [KTOR_RESPONSE_ERROR] Ошибка сервера биллинга: ${e.response.status.description}")
      SnackbarManager.showMessage(e.response.status.description)
      emit(Resource.Error<List<ServiceEntity>>())

    } catch (e: Exception) {
      // РЕШЕНИЕ: Универсальный КМР-перехват, который нативно ловит оффлайн-режим на Mac, Android и iOS!
      println("[$className.$methodName]: [OFFLINE_OR_EXCEPTION] Сбой сети или процесса: ${e.message}. Проверка кэша...")

      // Аварийно вычитываем локально сохраненный оффлайн-кэш из SQLDelight
      val fallback: List<ServiceEntity> = getLocal(params.addressId, currentServiceType, params.year)

      if (fallback.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_RECOVERY] Сети нет, но выведен локальный кэш ГИОЦ")
        emit(Resource.Success(fallback))
        return@flow
      }

      // Если и в базе пусто, выводим плашку ошибки через SnackbarManager
      SnackbarManager.showMessage("Помилка з'єднання з розрахунковим центром. Перевірте мережу.")

      // ИСПРАВЛЕНО: Твоя фолбэк-строка передается позиционно на первое место
      emit(Resource.Error<List<ServiceEntity>>(e.message?.ifBlank { "Unknown Error" } ?: "Unknown Error"))
    }
  }.flowOn(Dispatchers.Default)
}
