package com.ykis.ykismobkmp.domain.repository.payment

import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse


private const val className = "PaymentRepositoryImpl"

/**
 * [PaymentRepositoryImpl] — Очищенная КМР-реализация репозитория оплат и инвойсов биллинга Южного.
 * Общается со строго типизированным KtorApiServiceImpl напрямую без лишних прослоек RemoteDataSource.
 */
class PaymentRepositoryImpl(
  private val apiService: KtorApiService
) : PaymentRepository {

  /**
   * [getPaymentList] — Извлечение архива совершенных оплат абонента ГИОЦ из Ktor API.
   * ИСПРАВЛЕНО: addressId переведен из Int на Long под КМР-стандарт СУБД.
   */
  override suspend fun getPaymentList(
    uid: String,
    addressId: Long,
    year: String
  ): GetPaymentResponse {
    println("[$className.getPaymentList]: Запрос архива оплат за рік $year для о/р: $addressId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "addressId" to addressId.toString(),
        "year" to year
      )
      apiService.getFlatPayment(params)
    } catch (ex: Exception) {
      println("[$className.getPaymentList] Critical Fatal Error: ${ex.message}")
      GetPaymentResponse(success = 0, message = ex.message ?: "Невідома помилка мережі оплат")
    }
  }
}



