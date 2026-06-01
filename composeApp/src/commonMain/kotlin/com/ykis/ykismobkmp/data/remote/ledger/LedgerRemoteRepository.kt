package com.ykis.ykismobkmp.data.remote.ledger

import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse

/**
 * [LedgerRemoteRepository] — Интерфейс удаленного взаимодействия с API начислений и платежей ЮКИС.
 */
interface LedgerRemoteRepository {

  /**
   * Получение детальной информации по начислениям и квитанциям услуг ЖКХ на прямых параметрах.
   */
  suspend fun getFlatDetailServices(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse

  /**
   * Получение суммарной задолженности/баланса для лицевого счета.
   */
  suspend fun getTotalDebtService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse



}
