package com.ykis.ykismobkmp.domain.repository.ledger.request

import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// Временная заглушка кастомного исключения, если оно объявлено в твоем core-пакете
class ExceptionWithResourceMessage(val resourceMessage: String) : Exception()

private val className = "GetTotalDebtServices"

/**
 * [GetTotalDebtServices] — Кроссплатформенный сценарий (Use Case) получения суммарной задолженности ГИОЦ Южного.
 * ИСПРАВЛЕНО НАМЕРТВО: Класс ServiceParams удален, внедрена прямая работа с LedgerRepositoryCash и ServiceRepository!
 * Намертво зафиксирован для полной замены.
 */
class GetTotalDebtServices(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {

  /**
   * [invoke] — Точка реактивного вызова бизнес-сценария на базовых параметрах.
   */
  operator fun invoke(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): Flow<Resource<ServiceEntity>> = flow {
    val methodName = "GetTotalDebt"
    try {
      println("[$className.$methodName]: [START] AddrID: $addressId, UID: ${uid.takeLast(5)}")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Через Ktor клиент репозитория биллинга г. Южного)
      val response = repository.getTotalDebtService(
        uid = uid,
        addressId = addressId,
        year = year,
        service = service,
        total = total
      )

      if (response.success == 1 && response.services.isNotEmpty()) {
        val serviceData = response.services[0]
        println("[$className.$methodName]: [NETWORK_SUCCESS] Debt: ${serviceData.dolg}")

        // Вызываем атомарное кэширование пакета в СУБД через LedgerRepositoryCash
        try {
          ledgerCache.addService(response.services)
          println("[$className.$methodName]: Итоговый баланс успешно обновлен в SQLDelight")
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Ошибка записи баланса в СУБД: ${dbEx.message}")
        }

        emit(Resource.Success(serviceData))
      } else {
        println("[$className.$methodName]: [SERVER_REJECT] Success=0 или список пуст")
        // Пытаемся взять из локального SQLite кэша, если сеть ответила отказом
        val totalDebt = ledgerCache.getTotalDebt(addressId)
        if (totalDebt != null) {
          println("[$className.$methodName]: [DB_FALLBACK] Найдено в базе после отказа сети")
          emit(Resource.Success(totalDebt))
        } else {
          // Позиционный аргумент без именованного префикса message =
          emit(Resource.Error<ServiceEntity>("Дані відсутні"))
        }
      }

    } catch (e: ResponseException) {
      val errorDescription = e.response.status.description
      println("[$className.$methodName]: [HTTP_ERROR] Код статуса Ktor: ${e.response.status}")
      SnackbarManager.showMessage(errorDescription)
      emit(Resource.Error<ServiceEntity>("Помилка сервера: $errorDescription"))

    } catch (e: Exception) {
      // Универсальный КМР-перехват, нативно отрабатывающий оффлайн-режим на Mac, Android и iOS
      println("[$className.$methodName]: [EXCEPTION_OR_OFFLINE] Сбой связи Ktor: ${e.message}. Проверка локального кэша.")

      // Чтение из локальной базы данных SQLDelight при отсутствии интернета
      val totalDebt = ledgerCache.getTotalDebt(addressId)
      if (totalDebt != null) {
        println("[$className.$methodName]: [OFFLINE_MODE] Выведены оффлайн данные из SQLite кэша")
        emit(Resource.Success(totalDebt))
      } else {
        // Позиционный аргумент без именованного префикса message =
        emit(Resource.Error<ServiceEntity>("Перевірте підключення до інтернету"))
      }
    }
  }.flowOn(Dispatchers.Default) // Безопасное КМР-переключение потоков для всех ОС
}
