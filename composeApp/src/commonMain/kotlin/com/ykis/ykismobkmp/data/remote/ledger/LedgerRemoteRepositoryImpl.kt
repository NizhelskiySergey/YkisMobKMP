package com.ykis.ykismobkmp.data.remote.ledger

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.CURRENT_VALUE
import com.ykis.ykismobkmp.core.Constants.HOUSE_ID
import com.ykis.ykismobkmp.core.Constants.SERVICE
import com.ykis.ykismobkmp.core.Constants.TOTAL
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.core.Constants.YEAR
import com.ykis.ykismobkmp.core.Constants.KVARTPLATA
import com.ykis.ykismobkmp.core.Constants.NEW_VALUE
import com.ykis.ykismobkmp.core.Constants.RFOND
import com.ykis.ykismobkmp.core.Constants.TEPLO
import com.ykis.ykismobkmp.core.Constants.VODA
import com.ykis.ykismobkmp.core.Constants.TBO
import com.ykis.ykismobkmp.core.Constants.TEPLOMER_ID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse

class LedgerRemoteRepositoryImpl(
  private val ktorApiService: KtorApiService
) : LedgerRemoteRepository {

  override suspend fun getFlatDetailServices(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    return ktorApiService.getFlatService(
      createGetFlatServiceMap(
        uid = uid,
        addressId = addressId,
        houseId = houseId,
        year = year,
        service = service,
        total = total
      )
    )
  }
  override suspend fun getTotalDebtService(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    return ktorApiService.getFlatService(
      createGetFlatServiceMap(
        uid = uid,
        addressId = addressId,
        houseId = houseId,
        year = year,
        service = service,
        total = total
      )
    )
  }
  private fun createGetFlatServiceMap(
    uid: String,
    addressId: Long,
    houseId: Long,
    year: String,
    service: Byte,
    total: Byte,
  ): Map<String, String> {
    return mapOf(
      UID to uid,
      ADDRESS_ID to addressId.toString(),
      HOUSE_ID to houseId.toString(),
      YEAR to year,
      SERVICE to service.toString(),
      TOTAL to total.toString()
    )
  }
}
