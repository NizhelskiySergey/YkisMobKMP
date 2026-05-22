package com.ykis.ykismobkmp.domain.repository.ledger

import com.ykis.ykismobkmp.data.remote.ledger.LedgerRemoteRepository
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse

/**
 * [LedgerRepositoryImpl] — Оптимизированная КМР-реализация репозитория коммунальных служб ЮКИС.
 */
class LedgerRepositoryImpl(
  private val remote: LedgerRemoteRepository
) : LedgerRepository {

  private val currentClassName = "LedgerRepositoryImpl"

  /**
   * [getFlatDetailService] — Получение детальной информации по начислениям и квитанциям.
   */
  override suspend fun getFlatDetailService(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    println("[$currentClassName.getFlatDetailService]: Запрос деталей ЖЕК/ОСББ для квартиры ID=$addressId")

    return try {
      // Пробрасываем вызов напрямую в сетевой шлюз remote
      remote.getFlatDetailServices(uid = uid, addressId = addressId, year = year)
    } catch (ex: Exception) {
      println("[$currentClassName.getFlatDetailService] Критическая ошибка сети Ktor: ${ex.message}")
      GetServiceResponse(
        success = 0,
        message = ex.message ?: "Невідома помилка мережі розрахункового центру"
      )
    }
  }

  /**
   * [getTotalDebtService] — Получение суммарной задолженности по коммунальным предприятиям Южного.
   */
  override suspend fun getTotalDebtService(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    println("[$currentClassName.getTotalDebtService]: Запрос баланса ГИОЦ для лицевого счета ID=$addressId")
    return try {
      // Пробрасываем вызов напрямую в сетевой шлюз remote на прямых аргументах
      remote.getTotalDebtService(
        uid = uid,
        addressId = addressId,
        year = year,
        service = service,
        total = total
      )
    } catch (ex: Exception) {
      println("[$currentClassName.getTotalDebtService] Критическая ошибка сети Ktor: ${ex.message}")
      GetServiceResponse(
        success = 0,
        message = ex.message ?: "Помилка синхронізації заборгованостей"
      )
    }
  }

  /**
   * [getPaymentList] — Получение архива оплат жильца.
   */
  override suspend fun getPaymentList(
    uid: String,
    addressId: Long,
    year: String
  ): GetPaymentResponse {
    println("[$currentClassName.getPaymentList]: Запрос архива оплат за рік $year для о/р: $addressId")
    return try {
      // Пробрасываем вызов напрямую в сетевой шлюз remote
      remote.getPaymentList(uid = uid, addressId = addressId, year = year)
    } catch (ex: Exception) {
      println("[$currentClassName.getPaymentList] Critical Fatal Error: ${ex.message}")
      GetPaymentResponse(success = 0, message = ex.message ?: "Невідома помилка мережі оплат")
    }
  }
}

