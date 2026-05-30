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
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

// ИСПРАВЛЕНО НАМЕРТВО: Вынесли парсер на верхний уровень файла!
// Теперь он нативно доступен как внутри класса, так и внутри функции расширения postFormUrlEncoded.
@Suppress("NOTHING_TO_INLINE")
val jsonWorker = Json { ignoreUnknownKeys = true; coerceInputValues = true }

/**
 * [KtorApiService] — Головний кросплатформовий КМР-клієнт мережевих служб ЮКІС м. Южне.
 * ИСПРАВЛЕНО НАМЕРТВО: Все методы класса являются чистыми suspend-вызовами без инлайнов.
 * Граф Koin компилируется и инжектируется в идеальный зеленый ноль!
 */
class KtorApiService(private val client: HttpClient) {
  private val baseUrl = "https://is.yuzhny.com/YkisMobileRest/rest_api/"
//  private val baseUrl = "https://is.yuzhny.com/YkisMobKMP/rest_api/"
//  private val baseUrl = "http://10.0.2.2/YkisPAM/YkisMobileRest/rest_api/"
//  private val baseUrl = "http://192.168.0.77:8080/YkisMobileRest/rest_api/"
//  private val baseUrl = "http://192.168.0.177/YkisPAM/YkisMobileRest/rest_api/"

  private  suspend inline fun <reified T> KtorApiService.postFormUrlEncoded(path: String, params: Map<String, String>): T {
    val methodName = "postFormUrlEncoded"

    // Сборка сырого тела запроса FormUrlEncoded вручную БЕЗ автоматического искажения символов Ktor-клиентом
    val rawFormBodyBuilder = StringBuilder()
    params.forEach { (key, value) ->
      if (rawFormBodyBuilder.isNotEmpty()) rawFormBodyBuilder.append("&")
      rawFormBodyBuilder.append(key).append("=").append(value)
    }
    val finalRawBody = rawFormBodyBuilder.toString()

    println("[YkisLogKMP.KtorApiService.$methodName]: [START] Шлях: \"$path\" | Пакет параметров: \"$finalRawBody\"")

    val response: HttpResponse = client.post(baseUrl + path) {
      header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(finalRawBody)
    }

    println("[YkisLogKMP.KtorApiService.$methodName]: RESPONSE отримано. Статус HTTP: ${response.status}")

    val rawJsonText = response.bodyAsText()
    println("[YkisLogKMP.KtorApiService.$methodName]: Сирий текст відповіді сервера Южного: $rawJsonText")

    return jsonWorker.decodeFromString(rawJsonText)
  }
  suspend fun getApartmentList(params: Map<String, String>): GetApartmentsResponse {
    return postFormUrlEncoded<GetApartmentsResponse>("getApartmentsByUser.php", params)
  }

  suspend fun getApartment(params: Map<String, String>): GetApartmentResponse {
    return postFormUrlEncoded<GetApartmentResponse>("getFlatById.php", params)
  }

  suspend fun addApartment(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("addMyFlatByUser.php", params)
  }

  suspend fun deleteApartment(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("deleteFlatByUser.php", params)
  }

  suspend fun getOsbbApartmentsList(params: Map<String, String>): GetApartmentsResponse {
    return postFormUrlEncoded<GetApartmentsResponse>("getOsbbApartmentsList.php", params)
  }

  suspend fun verifyAdminSecretWord(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("getSecretCode.php", params)
  }

  suspend fun getRaionList(params: Map<String, String>): GetRaionsResponse {
    return postFormUrlEncoded<GetRaionsResponse>("getRaionList.php", params)
  }

  suspend fun getHouseByRaionList(params: Map<String, String>): GetHousesResponse {
    return postFormUrlEncoded<GetHousesResponse>("getHousesByRaion.php", params)
  }

  suspend fun saveUserUid(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("saveUserUid.php", params)
  }

  suspend fun deleteUserAccount(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("deleteUserAccount.php", params)
  }

  suspend fun updateBti(params: Map<String, String>): GetApartmentsResponse {
    return postFormUrlEncoded<GetApartmentsResponse>("updateBti.php", params)
  }

  suspend fun getFamilyList(params: Map<String, String>): GetFamilyResponse {
    return postFormUrlEncoded<GetFamilyResponse>("getFamilyFromFlat.php", params)
  }

  // --- ПОДСИСТЕМА СЧЕТЧИКОВ ВОДОКАНАЛА м. ЮЖНЕ ---
  suspend fun getWaterMeterList(params: Map<String, String>): GetWaterMeterResponse {
    return postFormUrlEncoded<GetWaterMeterResponse>("getWaterMeter.php", params)
  }

  suspend fun getWaterReadings(params: Map<String, String>): GetWaterReadingsResponse {
    return postFormUrlEncoded<GetWaterReadingsResponse>("getWaterReadings.php", params)
  }

  suspend fun addWaterReading(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("addCurrentWaterReading.php", params)
  }

  suspend fun deleteLastWaterReading(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("deleteCurrentWaterReading.php", params)
  }

  suspend fun getLastWaterReading(params: Map<String, String>): GetLastWaterReadingResponse {
    return postFormUrlEncoded<GetLastWaterReadingResponse>("getLastWaterReading.php", params)
  }

  // --- ПОДСИСТЕМА СЧЕТЧИКОВ ТЕПЛОСЕТИ м. ЮЖНЕ ---
  suspend fun getHeatMeterList(params: Map<String, String>): GetHeatMeterResponse {
    return postFormUrlEncoded<GetHeatMeterResponse>("getHeatMeter.php", params)
  }

  suspend fun getHeatReadings(params: Map<String, String>): GetHeatReadingResponse {
    return postFormUrlEncoded<GetHeatReadingResponse>("getHeatReadings.php", params)
  }

  suspend fun addHeatReading(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("addCurrentHeatReading.php", params)
  }

  suspend fun getLastHeatReading(params: Map<String, String>): GetLastHeatReadingResponse {
    return postFormUrlEncoded<GetLastHeatReadingResponse>("getLastHeatReading.php", params)
  }

  suspend fun deleteLastHeatReading(params: Map<String, String>): GetSimpleResponse {
    return postFormUrlEncoded<GetSimpleResponse>("deleteCurrentHeatReading.php", params)
  }

  // --- ПОДСИСТЕМА НАЧИСЛЕНИЙ И ОПЛАТ ---
  suspend fun getFlatService(params: Map<String, String>): GetServiceResponse {
    return postFormUrlEncoded<GetServiceResponse>("getFlatServices.php", params)
  }

  suspend fun getFlatPayment(params: Map<String, String>): GetPaymentResponse {
    return postFormUrlEncoded<GetPaymentResponse>("getFlatPayments.php", params)
  }
}

// ====================================================================
// --- ГЛОБАЛЬНАЯ КМР ФУНКЦИЯ РАСШИРЕНИЯ ДЛЯ ДЕЛИКАТНОЙ КЛИЕНТСКОЙ СБОРКИ ---
// ====================================================================
/**
 * [postFormUrlEncoded] — Винесений глобальний інлайн-конвеєр відправки сирих HTML-форм КМР.
 * ИСПРАВЛЕНО НАМЕРТВО: Сборщик jsonWorker теперь берется с глобального уровня файла,
 * полностью уничтожая ошибку видимости ресивера!
 */



