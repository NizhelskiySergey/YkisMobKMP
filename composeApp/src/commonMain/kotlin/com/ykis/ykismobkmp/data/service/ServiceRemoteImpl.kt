package com.ykis.ykismobkmp.data.service


import com.ykis.mob.data.remote.service.ServiceParams
import com.ykis.mob.data.remote.service.ServiceRemote
import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.HOUSE_ID
import com.ykis.ykismobkmp.core.Constants.SERVICE
import com.ykis.ykismobkmp.core.Constants.TOTAL
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.core.Constants.YEAR
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetServiceResponse

class ServiceRemoteImpl(
  private val ktorApiService: KtorApiService
) : ServiceRemote {

  override suspend fun getFlatDetailServices(params: ServiceParams): GetServiceResponse {
    val methodName = "ServiceRemote.getFlatDetail"
    val map = createGetFlatServiceMap(
      params.uid, params.addressId, params.houseId, params.year, params.service, params.total
    )

    Log.d("YkisLog", "$methodName: [SEND] Params: $map")

    return try {
      val response = ktorApiService.getFlatService(map)
      Log.d("YkisLog", "$methodName: [RECV] Success: ${response.success}, Count: ${response.services.size}")
      response
    } catch (e: Exception) {
      Log.e("YkisLog", "$methodName: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun getTotalDebtService(params: ServiceParams): GetServiceResponse {
    val methodName = "ServiceRemote.getTotalDebt"
    val map = createGetFlatServiceMap(
      params.uid, params.addressId, params.houseId, params.year, params.service, params.total
    )

    Log.d("YkisLog", "$methodName: [SEND] Params: $map")

    return try {
      val response = ktorApiService.getFlatService(map)
      Log.d("YkisLog", "$methodName: [RECV] Success: ${response.success}, Debt: ${response.services.firstOrNull()?.dolg}")
      response
    } catch (e: Exception) {
      Log.e("YkisLog", "$methodName: [ERROR] ${e.message}")
      throw e
    }
  }

  private fun createGetFlatServiceMap(
    uid: String,
    addressId: Int,
    houseId: Int,
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

