package com.ykis.ykismobkmp.data.api

import com.ykis.ykismobkmp.core.utils.Log
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
    private val className = "KtorApiServiceImpl"

    /**
     * [postForm] — Универсальный приватный метод для POST FormUrlEncoded (Кроссплатформенный)
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

    // ==========================================
    // --- МОДУЛЬ КВАРТИР И АДМИН-ФУНКЦИЙ ---
    // ==========================================

    suspend fun getApartmentList(uid: String): GetApartmentsResponse {
      return postForm("getApartmentsByUser.php", mapOf("uid" to uid))
    }

    suspend fun getApartment(addressId: Long, uid: String): GetApartmentResponse {
      return postForm("getFlatById.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    suspend fun addApartmentUser(code: String, uid: String): GetSimpleResponse {
      return postForm("addMyFlatByUser.php", mapOf("code" to code, "uid" to uid))
    }

    suspend fun deleteApartment(addressId: Long, uid: String): GetSimpleResponse {
      return postForm("deleteFlatByUser.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    suspend fun getOsbbApartmentsList(targetId: Long, uid: String): GetApartmentsResponse {
      return postForm("getOsbbApartmentsList.php", mapOf("target_id" to targetId.toString(), "uid" to uid))
    }

    suspend fun verifyAdminSecretWord(code: String, uid: String): GetSimpleResponse {
      return postForm("getSecretCode.php", mapOf("code" to code, "uid" to uid))
    }

    suspend fun getRaionList(uid: String): GetRaionsResponse {
      return postForm("getRaionList.php", mapOf("uid" to uid))
    }

    suspend fun getHouseByRaionList(raionId: Long, uid: String): GetHousesResponse {
      return postForm("getHousesByRaion.php", mapOf("raion_id" to raionId.toString(), "uid" to uid))
    }

    suspend fun saveUserUid(uid: String, email: String): GetSimpleResponse {
      return postForm("saveUserUid.php", mapOf("uid" to uid, "email" to email))
    }

    suspend fun deleteUserAccount(uid: String): GetSimpleResponse {
      return postForm("deleteUserAccount.php", mapOf("uid" to uid))
    }

    suspend fun updateBti(addressId: Long, area: Double, uid: String): GetSimpleResponse {
      return postForm("updateBti.php", mapOf("address_id" to addressId.toString(), "area" to area.toString(), "uid" to uid))
    }

    suspend fun getFamilyList(addressId: Long, uid: String): GetFamilyResponse {
      return postForm("getFamilyFromFlat.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    // ==========================================
    // --- МОДУЛЬ СЧЕТЧИКОВ ВОДЫ ---
    // ==========================================

    suspend fun getWaterMeterList(addressId: Long, uid: String): GetWaterMeterResponse {
      println("[$className.getWaterMeterList]: addressId=$addressId, uid=${uid.takeLast(5)}")
      return postForm("getWaterMeter.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    suspend fun getWaterReadings(vodomerId: Long, uid: String): GetWaterReadingsResponse {
      println("[$className.getWaterReadings]: vodomerId=$vodomerId, uid=${uid.takeLast(5)}")
      return postForm("getWaterReadings.php", mapOf("vodomer_id" to vodomerId.toString(), "uid" to uid))
    }

    suspend fun addWaterReading(vodomerId: Long, currentValue: Double, newValue: Double, uid: String): GetSimpleResponse {
      println("[$className.addWaterReading]: vodomerId=$vodomerId, cur=$currentValue, new=$newValue")
      return postForm(
        "addCurrentWaterReading.php",
        mapOf(
          "vodomer_id" to vodomerId.toString(),
          "current_value" to currentValue.toString(),
          "new_value" to newValue.toString(),
          "uid" to uid
        )
      )
    }

    suspend fun deleteLastWaterReading(readingId: Long, uid: String): GetSimpleResponse {
      println("[$className.deleteLastWaterReading]: readingId=$readingId, uid=${uid.takeLast(5)}")
      return postForm("deleteCurrentWaterReading.php", mapOf("pok_id" to readingId.toString(), "uid" to uid))
    }

    suspend fun getLastWaterReading(vodomerId: Long, uid: String): GetLastWaterReadingResponse {
      println("[$className.getLastWaterReading]: vodomerId=$vodomerId, uid=${uid.takeLast(5)}")
      return postForm("getLastWaterReading.php", mapOf("vodomer_id" to vodomerId.toString(), "uid" to uid))
    }

    // ==========================================
    // --- МОДУЛЬ СЧЕТЧИКОВ ТЕПЛА ---
    // ==========================================

    suspend fun getHeatMeterList(addressId: Long, uid: String): GetHeatMeterResponse {
      println("[$className.getHeatMeterList]: addressId=$addressId, uid=${uid.takeLast(5)}")
      return postForm("getHeatMeter.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    suspend fun getHeatReadings(teplomerId: Long, uid: String): GetHeatReadingResponse {
      println("[$className.getHeatReadings]: teplomerId=$teplomerId, uid=${uid.takeLast(5)}")
      return postForm("getHeatReadings.php", mapOf("teplomer_id" to teplomerId.toString(), "uid" to uid))
    }

    suspend fun addHeatReading(teplomerId: Long, currentValue: Double, newValue: Double, uid: String): GetSimpleResponse {
      println("[$className.addHeatReading]: teplomerId=$teplomerId, cur=$currentValue, new=$newValue")
      return postForm(
        "addCurrentHeatReading.php",
        mapOf(
          "teplomer_id" to teplomerId.toString(),
          "current_value" to currentValue.toString(),
          "new_value" to newValue.toString(),
          "uid" to uid
        )
      )
    }
  suspend fun getLastHeatReading(teplomerId: Long, uid: String): GetLastHeatReadingResponse {
    println("[$className.getLastHeatReading]: teplomerId=$teplomerId, uid=${uid.takeLast(5)}")
    return postForm("getLastHeatReading.php", mapOf("teplomer_id" to teplomerId.toString(), "uid" to uid))
  }
    suspend fun deleteLastHeatReading(readingId: Long, uid: String): GetSimpleResponse {
      println("[$className.deleteLastHeatReading]: readingId=$readingId, uid=${uid.takeLast(5)}")
      return postForm("deleteCurrentHeatReading.php", mapOf("pok_id" to readingId.toString(), "uid" to uid))
    }

    // ==========================================
    // --- МОДУЛЬ НАЧИСЛЕНИЙ И ОПЛАТ ---
    // ==========================================

    suspend fun getFlatService(addressId: Long, uid: String): GetServiceResponse {
      return postForm("getFlatServices.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    suspend fun getFlatPayment(addressId: Long, uid: String): GetPaymentResponse {
      return postForm("getFlatPayments.php", mapOf("address_id" to addressId.toString(), "uid" to uid))
    }

    suspend fun insertPayment(paymentId: String, amount: Double, uid: String): InsertPaymentResponse {
      return postForm(
        "newPaymentXpay.php",
        mapOf("payment_id" to paymentId, "amount" to amount.toString(), "uid" to uid)
      )
    }
  }


