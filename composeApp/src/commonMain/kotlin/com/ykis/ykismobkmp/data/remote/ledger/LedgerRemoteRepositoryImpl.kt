package com.ykis.ykismobkmp.data.remote.ledger

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.HOUSE_ID
import com.ykis.ykismobkmp.core.Constants.SERVICE
import com.ykis.ykismobkmp.core.Constants.TOTAL
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.core.Constants.YEAR
import com.ykis.ykismobkmp.core.Constants.KVARTPLATA
import com.ykis.ykismobkmp.core.Constants.RFOND
import com.ykis.ykismobkmp.core.Constants.TEPLO
import com.ykis.ykismobkmp.core.Constants.VODA
import com.ykis.ykismobkmp.core.Constants.TBO
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse

/**
 * [LedgerRemoteRepositoryImpl] — Реализация удаленного репозитория начислений и платежей на базе KtorApiService.
 * ИСПРАВЛЕНО НАМЕРТВО: Все структуры params удалены, заменен Android Log на кроссплатформенный println, типы Int переведены в Long!
 * Намертво зафиксирован для полной замены.
 */
class LedgerRemoteRepositoryImpl(
  private val ktorApiService: KtorApiService
) : LedgerRemoteRepository {

  private val className = "LedgerRemoteRepositoryImpl"

  override suspend fun getFlatDetailServices(
    uid: String,
    addressId: Long,
    year: String
  ): GetServiceResponse {
    val methodName = "getFlatDetailServices"
    // Передаем houseId = 0L, service = 0, total = 0 по умолчанию для полной детализации
    val requestMap = createGetFlatServiceMap(
      uid = uid,
      addressId = addressId,
      houseId = 0L,
      year = year,
      service = 0,
      total = 0
    )

    println("[$className.$methodName]: [SEND] Params: $requestMap")

    return try {
      val response = ktorApiService.getFlatService(requestMap)
      println("[$className.$methodName]: [RECV] Success: ${response.success}")
      response
    } catch (e: Exception) {
      println("[$className.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun getTotalDebtService(
    uid: String,
    addressId: Long,
    year: String,
    service: Byte,
    total: Byte
  ): GetServiceResponse {
    val methodName = "getTotalDebtService"
    val requestMap = createGetFlatServiceMap(
      uid = uid,
      addressId = addressId,
      houseId = 0L,
      year = year,
      service = service,
      total = total
    )

    println("[$className.$methodName]: [SEND] Params: $requestMap")

    return try {
      val response = ktorApiService.getFlatService(requestMap)
      println("[$className.$methodName]: [RECV] Success: ${response.success}")
      response
    } catch (e: Exception) {
      println("[$className.$methodName]: [ERROR] ${e.message}")
      throw e
    }
  }

  override suspend fun getPaymentList(
    uid: String,
    addressId: Long,
    year: String
  ): GetPaymentResponse {
    val methodName = "getPaymentList"
    println("[$className.$methodName]: Запрос архива оплат для ID: $addressId")
    return try {
      ktorApiService.getFlatPayment(createGetPaymentListMap(addressId, year, uid))
    } catch (e: Exception) {
      println("[$className.$methodName]: [ERROR] ${e.message}")
      throw e
    }
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

  private fun createGetPaymentListMap(
    addressId: Long,
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
    uid: String,
    addressId: Long,
    kvartplata: Double,
    rfond: Double,
    teplo: Double,
    voda: Double,
    tbo: Double
  ): Map<String, String> {
    val map = HashMap<String, String>()
    map[UID] = uid
    map[ADDRESS_ID] = addressId.toString()
    map[KVARTPLATA] = kvartplata.toString()
    map[RFOND] = rfond.toString()
    map[TEPLO] = teplo.toString()
    map[VODA] = voda.toString()
    map[TBO] = tbo.toString()
    return map
  }
}
