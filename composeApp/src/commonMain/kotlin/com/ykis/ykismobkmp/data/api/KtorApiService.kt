package com.ykis.ykismobkmp.data.api

import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetLastHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters

class KtorApiService(private val client: HttpClient) {
  private val baseUrl = "https://is.yuzhny.com/YkisMobKMP/rest_api/"
  //  private val baseUrl = "http://10.0.2.2/YkisPAM/YkisMobileRest/rest_api/"
//  private val baseUrl = "http://192.168.0.77:8080/YkisMobileRest/rest_api/"
//  private val baseUrl = "http://192.168.0.177/YkisPAM/YkisMobileRest/rest_api/"

  private val tag = "KtorApiService"
  private val className = "KtorApiServiceImpl"

  /**
   * [postForm] — Универсальный приватный метод для POST FormUrlEncoded (Кроссплатформенный)
   */
  private suspend inline fun <reified T> postForm(path: String, params: Map<String, String>): T {
    return client.submitForm(
      url = baseUrl + path,
      formParameters = parameters {
        params.forEach { (key, value) -> append(key, value) }
      }
    ) {
      // Указываем серверу и клиенту, что мы работаем с JSON
      header(HttpHeaders.Accept, ContentType.Application.Json)
    }.body()
  }

  // ==========================================
  // --- МОДУЛЬ КВАРТИР И АДМИН-ФУНКЦИЙ ---
  // ==========================================

  suspend fun getApartmentList(params: Map<String, String>): GetApartmentsResponse {
    return postForm("getApartmentsByUser.php", params)
  }

  suspend fun getApartment(params: Map<String, String>): GetApartmentResponse {
    return postForm("getFlatById.php", params)
  }

  suspend fun addApartment(params: Map<String, String>): GetSimpleResponse {
    return postForm("addMyFlatByUser.php", params)
  }

  suspend fun deleteApartment(params: Map<String, String>): GetSimpleResponse {
    return postForm("deleteFlatByUser.php", params)
  }


  suspend fun getOsbbApartmentsList(params: Map<String, String>): GetApartmentsResponse {
    return postForm("getOsbbApartmentsList.php", params)

  }

  suspend fun verifyAdminSecretWord(params: Map<String, String>): GetSimpleResponse {
    return postForm("getSecretCode.php", params)
  }

  suspend fun getRaionList(params: Map<String, String>): GetRaionsResponse {
    return postForm("getRaionList.php", params)
  }

  suspend fun getHouseByRaionList(params: Map<String, String>): GetHousesResponse {
    return postForm("getHousesByRaion.php", params)
  }

  suspend fun saveUserUid(params: Map<String, String>): GetSimpleResponse {
    return postForm("saveUserUid.php", params)
  }

  suspend fun deleteUserAccount(params: Map<String, String>): GetSimpleResponse {
    return postForm("deleteUserAccount.php", params)
  }

  suspend fun updateBti(params: Map<String, String>): GetApartmentsResponse {
    return postForm("updateBti.php", params)
  }

  suspend fun getFamilyList(params: Map<String, String>): GetFamilyResponse {
    return postForm("getFamilyFromFlat.php",params)
  }

  // ==========================================
  // --- МОДУЛЬ СЧЕТЧИКОВ ВОДЫ ---
  // ==========================================

  suspend fun getWaterMeterList(params: Map<String, String>): GetWaterMeterResponse {
    return postForm("getWaterMeter.php", params)
  }

  suspend fun getWaterReadings(params: Map<String, String>): GetWaterReadingsResponse {
    return postForm("getWaterReadings.php", params)
  }

  suspend fun addWaterReading(params: Map<String, String>): GetSimpleResponse {
    return postForm("addCurrentWaterReading.php", params)
  }

  suspend fun deleteLastWaterReading(params: Map<String, String>): GetSimpleResponse {
    return postForm("deleteCurrentWaterReading.php", params)
  }

  suspend fun getLastWaterReading(params: Map<String, String>): GetLastWaterReadingResponse {
    return postForm("getLastWaterReading.php", params)
  }

  // ==========================================
  // --- МОДУЛЬ СЧЕТЧИКОВ ТЕПЛА ---
  // ==========================================

  suspend fun getHeatMeterList(params: Map<String, String>): GetHeatMeterResponse {
    return postForm("getHeatMeter.php", params)
  }

  suspend fun getHeatReadings(params: Map<String, String>): GetHeatReadingResponse {
    return postForm("getHeatReadings.php", params)
  }

  suspend fun addHeatReading(params: Map<String, String>): GetSimpleResponse {
    return postForm("addCurrentHeatReading.php", params)
  }

  suspend fun getLastHeatReading(params: Map<String, String>): GetLastHeatReadingResponse {
    return postForm("getLastHeatReading.php", params)
  }

  suspend fun deleteLastHeatReading(params: Map<String, String>): GetSimpleResponse {
    return postForm("deleteCurrentHeatReading.php", params)

  }

  // ==========================================
  // --- МОДУЛЬ НАЧИСЛЕНИЙ И ОПЛАТ ---
  // ==========================================

  suspend fun getFlatService(params: Map<String, String>): GetServiceResponse {
    return postForm("getFlatServices.php",  params)

  }

  suspend fun getFlatPayment(params: Map<String, String>): GetPaymentResponse {
    return postForm("getFlatPayments.php", params)

  }


}


