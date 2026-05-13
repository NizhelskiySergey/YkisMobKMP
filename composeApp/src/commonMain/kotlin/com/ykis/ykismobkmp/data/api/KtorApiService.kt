package com.ykis.ykismobkmp.data.api

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.data.responses.GetApartmentResponse
import com.ykis.ykismobkmp.data.responses.GetApartmentsResponse
import com.ykis.ykismobkmp.data.responses.GetFamilyResponse
import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetHousesResponse
import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.GetRaionsResponse
import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters

class KtorApiService(private val client: HttpClient) {
  private val baseUrl = "https://is.yuzhny.com/YkisMobileRest/rest_api/"
  //  private val baseUrl = "http://10.0.2.2/YkisPAM/YkisMobileRest/rest_api/"
//  private val baseUrl = "http://192.168.0.77:8080/YkisMobileRest/rest_api/"
//  private val baseUrl = "http://192.168.0.177/YkisPAM/YkisMobileRest/rest_api/"
  private val tag = "KtorApiService"

  /**
   * Универсальный метод для POST FormUrlEncoded (Кроссплатформенный аналог submitForm)
   */
  private suspend inline fun <reified T> postForm(path: String, params: Map<String, String>): T {
    val fullUrl = baseUrl + path
    println("[$tag.postForm]: Sending request to $fullUrl with params: $params")

    return client.submitForm(
      url = fullUrl,
      formParameters = parameters {
        params.forEach { (key, value) -> append(key, value) }
      }
    ) {
      header(HttpHeaders.Accept, ContentType.Application.Json)
    }.body()
  }

  // --- БЛОК КВАРТИР ---

  // ИСПРАВЛЕНО: Теперь принимает готовую мапу из репозитория, убирая ошибку 'String expected'
  suspend fun getApartmentList(map: Map<String, String>): GetApartmentsResponse {
    return postForm("getApartmentsByUser.php", map)
  }

  // ИСПРАВЛЕНО: Принимает адрес как Long и мапу параметров
  suspend fun getApartment(addressId: Long, map: Map<String, String>): GetApartmentResponse {
    return postForm("getFlatById.php", map)
  }

  // ИСПРАВЛЕНО: Принимает готовую мапу
  suspend fun addApartmentUser(code: String, uid: String, map: Map<String, String>): GetSimpleResponse {
    return postForm("addMyFlatByUser.php", map)
  }

  // ИСПРАВЛЕНО: Принимает адрес как Long и мапу параметров
  suspend fun deleteApartment(addressId: Long, map: Map<String, String>): GetSimpleResponse {
    return postForm("deleteFlatByUser.php", map)
  }

  // --- БЛОК АДМИНИСТРАТОРА ---

  // ИСПРАВЛЕНО: Принимает targetId как Long и мапу параметров
  suspend fun getOsbbApartmentsList(targetId: Long, map: Map<String, String>): GetApartmentsResponse {
    return postForm("getOsbbApartmentsList.php", map)
  }

  suspend fun verifyAdminSecretWord(code: String, map: Map<String, String>): GetSimpleResponse {
    return postForm("getSecretCode.php", map)
  }

  // --- СПРАВОЧНИКИ ---

  suspend fun getRaionList(map: Map<String, String>): GetRaionsResponse {
    return postForm("getRaionList.php", map)
  }

  // ИСПРАВЛЕНО: Принимает raionId как Long и мапу параметров
  // [KtorApiService.kt]
  suspend fun getHouseByRaionList(raionId: Long, map: Map<String, String>): GetHousesResponse {
    return postForm("getHousesByRaion.php", map)
  }


  // --- ПРОФИЛЬ И СЕРВИСНЫЕ ---

  suspend fun saveUserUid(map: Map<String, String>): GetSimpleResponse {
    return postForm("saveUserUid.php", map)
  }

  suspend fun deleteUserAccount(map: Map<String, String>): GetSimpleResponse {
    return postForm("deleteUserAccount.php", map)
  }

  // ИСПРАВЛЕНО: Принимает готовую мапу из репозитория
  suspend fun updateBti(map: Map<String, String>): GetSimpleResponse {
    return postForm("updateBti.php", map)
  }

  // --- ЖИЛЬЦЫ И СЧЕТЧИКИ (Перевод всех ИД из Int в Long) ---

  // [KtorApiService.kt]
// Теперь Ktor автоматически десериализует JSON в объект GetFamilyResponse
  suspend fun getFamilyList(map: Map<String, String>): GetFamilyResponse {
    return postForm("getFamilyFromFlat.php", map)
  }


  suspend fun getWaterMeterList(addressId: Long): GetWaterMeterResponse {
    return postForm("getWaterMeter.php", mapOf("address_id" to addressId.toString()))
  }

  suspend fun getWaterReadings(vodomerId: Long): GetWaterReadingsResponse {
    return postForm("getWaterReadings.php", mapOf("vodomer_id" to vodomerId.toString()))
  }

  suspend fun addWaterReading(vodomerId: Long, value: Double, date: String): GetSimpleResponse {
    return postForm("addCurrentWaterReading.php", mapOf(
      "vodomer_id" to vodomerId.toString(),
      "current_value" to value.toString(),
      "date" to date
    ))
  }

  suspend fun deleteLastWaterReading(readingId: Long): GetSimpleResponse {
    return postForm("deleteCurrentWaterReading.php", mapOf("pok_id" to readingId.toString()))
  }

  suspend fun getLastWaterReading(vodomerId: Long): GetLastWaterReadingResponse {
    return postForm("getLastWaterReading.php", mapOf("vodomer_id" to vodomerId.toString()))
  }

  // --- ТЕПЛО (Перевод всех ИД из Int в Long) ---

  suspend fun getHeatMeterList(addressId: Long): GetHeatMeterResponse {
    return postForm("getHeatMeter.php", mapOf("address_id" to addressId.toString()))
  }

  suspend fun getHeatReadings(teplomerId: Long): GetHeatReadingResponse {
    return postForm("getHeatReadings.php", mapOf("teplomer_id" to teplomerId.toString()))
  }

  suspend fun addHeatReading(teplomerId: Long, value: Double, date: String): GetSimpleResponse {
    return postForm("addCurrentHeatReading.php", mapOf(
      "teplomer_id" to teplomerId.toString(),
      "current_value" to value.toString(),
      "date" to date
    ))
  }

  suspend fun deleteLastHeatReading(readingId: Long): GetSimpleResponse {
    return postForm("deleteCurrentHeatReading.php", mapOf("pok_id" to readingId.toString()))
  }

  // --- УСЛУГИ И ПЛАТЕЖИ ---

  suspend fun getFlatService(addressId: Long): GetServiceResponse {
    return postForm("getFlatServices.php", mapOf("address_id" to addressId.toString()))
  }

  suspend fun getFlatPayment(addressId: Long): GetPaymentResponse {
    return postForm("getFlatPayments.php", mapOf("address_id" to addressId.toString()))
  }

  suspend fun insertPayment(params: Map<String, String>): InsertPaymentResponse {
    return postForm("newPaymentXpay.php", params)
  }
}

