package com.ykis.ykismobkmp.data.payment

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.KVARTPLATA
import com.ykis.ykismobkmp.core.Constants.RFOND
import com.ykis.ykismobkmp.core.Constants.TBO
import com.ykis.ykismobkmp.core.Constants.TEPLO
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.core.Constants.VODA
import com.ykis.ykismobkmp.core.Constants.YEAR
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse

class PaymentRemoteImpl (
    private val ktorApiService: KtorApiService
) : PaymentRemote {

  override suspend fun getPaymentList(addressId: Int, year: String, uid: String): GetPaymentResponse {
    // Убрали .await(), KtorApiService сразу возвращает GetPaymentResponse
    return ktorApiService.getFlatPayment(
      createGetPaymentListMap(
        addressId, year, uid
      )
    )
  }

  override suspend fun insertPayment(params: InsertPaymentParams): InsertPaymentResponse {
    // Убрали .await(), KtorApiService сразу возвращает InsertPaymentResponse
    return ktorApiService.insertPayment(
      createInsertPaymentMap(params)
    )
  }


  private fun createGetPaymentListMap(
        addressId: Int,
        year: String,
        uid: String
    ): Map<String, String> {
        val map = HashMap<String, String>()
        map[ADDRESS_ID] = addressId.toString()
        map[YEAR] = year
        map[UID] = uid
        return map
    }

    private fun createInsertPaymentMap(
        params: InsertPaymentParams
    ): Map<String, String> {
        val map = HashMap<String, String>()
        map[UID] = params.uid
        map[ADDRESS_ID] = params.addressId.toString()
        map[KVARTPLATA] = params.kvartplata.toString()
        map[RFOND] = params.rfond.toString()
        map[TEPLO] = params.teplo.toString()
        map[VODA] = params.voda.toString()
        map[TBO] = params.tbo.toString()

        return map
    }
}
