package com.ykis.ykismobkmp.domain.repository.ledger.request

import com.ykis.ykismobkmp.cash.ledger.LedgerRepositoryCash
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [GetPaymentList] — Доменный Use Case для загрузки и локального кэширования архива оплат абонента ГИОЦ Южного.
 */
class GetPaymentList(
  private val repository: LedgerRepository,
  private val ledgerCache: LedgerRepositoryCash
) {
  private val className = "GetPaymentList"

  /**
   * [invoke] — Выполнение Use Case.
   * addressId и идентификаторы намертво зафиксированы под КМР-стандарт Long.
   */
  operator fun invoke(addressId: Long, year: String, uid: String): Flow<Resource<List<PaymentEntity>>> = flow {
    val methodName = "invoke"
    try {
      println("[$className.$methodName]: [START] AddrID: $addressId, Year: $year")
      emit(Resource.Loading())

      // 1. ПРОВЕРКА ЛОКАЛЬНОГО КЭША (Вызов через LedgerRepositoryCash)
      val localPayments = ledgerCache.getPaymentsFromFlat(addressId)
      if (localPayments.isNotEmpty()) {
        println("[$className.$methodName]: [DB_HIT] Знайдено в базі: ${localPayments.size}")
        emit(Resource.Success(localPayments))
      } else {
        println("[$className.$methodName]: [DB_MISS] Локальна база порожня")
      }

      // 2. ЗАПРОС В СЕТЬ (Через Ktor репозиторий напрямую)
      println("[$className.$methodName]: [NETWORK_REQ] Відправка запиту до сервера оплат...")
      val response = repository.getPaymentList(uid = uid, addressId = addressId, year = year)

      if (response.success == 1) {
        val remotePayments = response.payments ?: emptyList()
        println("[$className.$methodName]: [NETWORK_SUCCESS] Отримано з мережі: ${remotePayments.size}")

        // 3. ПЕРЕЗАПИСЬ КЭША (Выполняется атомарно через транзакции базы данных)
        try {
          ledgerCache.deletePaymentByApartment(addressId)
          ledgerCache.addPayments(remotePayments)
          println("[$className.$methodName]: Локальная база данных оплат успешно синхронизирована")
        } catch (dbEx: Exception) {
          println("[$className.${methodName}_WARN]: Ошибка записи оплат в СУБД: ${dbEx.message}")
        }

        // Отдаем финальный актуальный список в UI
        emit(Resource.Success(remotePayments))
      } else {
        println("[$className.$methodName]: [SERVER_REJECT] Success=0, Message: ${response.message}")
        if (localPayments.isEmpty()) {
          emit(Resource.Error(message = response.message ?: "Помилка завантаження архіву оплат"))
        }
      }

    } catch (e: ResponseException) {
      val errorDescription = e.response.status.description
      println("[$className.$methodName]: [HTTP_ERROR] $errorDescription")
      SnackbarManager.showMessage(errorDescription)
      emit(Resource.Error(message = "Помилка сервера оплат: $errorDescription"))
    } catch (ex: Exception) {
      println("[$className.$methodName]: [FATAL_ERROR] Сетевой сбой или таймаут Ktor: ${ex.message}")
      ex.printStackTrace()

      // OFFLINE RECOVERY: При любой ошибке сети/сервера пробуем еще раз отдать локальный кэш
      val lastHope = ledgerCache.getPaymentsFromFlat(addressId)
      if (lastHope.isNotEmpty()) {
        println("[$className.$methodName]: [OFFLINE_MODE] Показ даних з кэшу при збої мережі")
        emit(Resource.Success(lastHope))
      } else {
        SnackbarManager.showMessage("Відсутній зв'язок з сервером ГІОЦ")
        emit(Resource.Error(message = "Відсутній зв'язок та немає збережених даних"))
      }
    }
  }.flowOn(Dispatchers.Default) // Безопасный КМР-пул потоков корутин
}
