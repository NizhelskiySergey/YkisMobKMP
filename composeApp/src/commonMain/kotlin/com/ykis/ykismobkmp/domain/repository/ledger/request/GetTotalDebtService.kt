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
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): Flow<Resource<ServiceEntity>> = flow {
    val methodName = "GetTotalDebt"
    try {
      println("[$className.$methodName]: [START] AddrID: $addressId")
      emit(Resource.Loading())

      // 1. ЗАПРОС В СЕТЬ (Через Ktor клиент репозитория биллинга г. Южного)
      val response = repository.getTotalDebtService(
        uid = uid,
        addressId = addressId,
        houseId = houseId,
        year = year,
        service = service,
        total = total
      )

      if (response.success == 1 && response.services.isNotEmpty()) {
        val serviceData = response.services[0]
        println("[$className.$methodName]: [NETWORK_SUCCESS] Debt: ${serviceData.dolg}")

        // 2. АТОМАРНАЯ СИНХРОНИЗАЦИЯ
        try {
          if (!com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              ledgerCache.addService(response.services)
              println("[$className.$methodName]: Баланс оновлено в кеші")
          }
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Помилка запису в кеш: ${dbEx.message}")
        }

        emit(Resource.Success(serviceData))
      } else {
        println("[$className.$methodName]: [SERVER_REJECT] Success=0 або список порожній")
        
        // 3. FALLBACK НА КЕШ (Безопасно для Web)
        var totalDebt: ServiceEntity? = null
        try {
            if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
                kotlinx.coroutines.withTimeoutOrNull(500) {
                    totalDebt = ledgerCache.getTotalDebt(addressId)
                }
            } else {
                totalDebt = ledgerCache.getTotalDebt(addressId)
            }
        } catch (e: Exception) { }

        if (totalDebt != null) {
          println("[$className.$methodName]: [DB_FALLBACK] Знайдено в базі")
          emit(Resource.Success(totalDebt!!))
        } else {
          emit(Resource.Error<ServiceEntity>("Дані відсутні"))
        }
      }

    } catch (e: ResponseException) {
      val errorDescription = e.response.status.description
      println("[$className.$methodName]: [HTTP_ERROR] $errorDescription")
      emit(Resource.Error<ServiceEntity>("Помилка сервера: $errorDescription"))

    } catch (e: Exception) {
      println("[$className.$methodName]: [OFFLINE] ${e.message}. Перевірка кешу.")

      var totalDebt: ServiceEntity? = null
      try {
          if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
              kotlinx.coroutines.withTimeoutOrNull(500) {
                  totalDebt = ledgerCache.getTotalDebt(addressId)
              }
          } else {
              totalDebt = ledgerCache.getTotalDebt(addressId)
          }
      } catch (ex: Exception) { }

      if (totalDebt != null) {
        emit(Resource.Success(totalDebt!!))
      } else {
        emit(Resource.Error<ServiceEntity>("Перевірте підключення до інтернету"))
      }
    }
  }.flowOn(Dispatchers.Default) // Безопасное КМР-переключение потоков для всех ОС
}
