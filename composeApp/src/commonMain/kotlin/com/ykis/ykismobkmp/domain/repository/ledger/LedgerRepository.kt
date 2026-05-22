package com.ykis.ykismobkmp.domain.repository.ledger

import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse

/**
 * [ServiceRepository] — Главный доменный контракт взаимодействия с данными начислений и платежей (Ledger) ЮКИС.
 */
interface LedgerRepository {

  /**
   * Получение детальной информации по начислениям услуг ЖКХ для конкретной квартиры.
   */
  suspend fun getFlatDetailService(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse

  /**
   * Получение итоговой задолженности по коммунальным услугам расчетного центра г. Южный.
   */
  suspend fun getTotalDebtService(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse

  /**
   * Получение истории платежей (оплат) жильца по выбранному лицевому счету за указанный год.
   */
  suspend fun getPaymentList(uid: String,addressId: Long,year: String): GetPaymentResponse
}
