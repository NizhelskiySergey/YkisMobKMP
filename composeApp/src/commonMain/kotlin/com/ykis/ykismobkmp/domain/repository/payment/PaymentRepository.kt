package com.ykis.ykismobkmp.domain.repository.payment

import com.ykis.ykismobkmp.data.responses.GetPaymentResponse

/**
 * [PaymentRepository] — Единый кроссплатформенный контракт репозитория оплат и архива квитанций ЮКИС.
 * Полностью изолирован от платформ и типизирован под Long-идентификаторы биллинга г. Южный.
 */
interface PaymentRepository {

  /**
   * [getPaymentList] — Запрос архива совершенных абонентом оплат по конкретному году.
   * ИСПРАВЛЕНО: addressId переведен из Int на Long под КМР-стандарт СУБД.
   */
  suspend fun getPaymentList(uid: String,addressId: Long, year: String ): GetPaymentResponse

}
