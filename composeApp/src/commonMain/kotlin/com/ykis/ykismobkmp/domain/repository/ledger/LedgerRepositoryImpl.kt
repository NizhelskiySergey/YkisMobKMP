package com.ykis.ykismobkmp.domain.repository.ledger

import androidx.compose.animation.shrinkOut
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
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    println("[$currentClassName.getFlatDetailService]: Запрос деталей ЖЕК/ОСББ для квартиры ID=$addressId")

    return try {
      // Пробрасываем вызов напрямую в сетевой шлюз remote
      remote.getFlatDetailServices(uid = uid, addressId = addressId,houseId=houseId, year = year,service=service,total=total)
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
    houseId: Long,
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
        houseId = houseId ,
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


}

