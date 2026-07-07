package com.ykis.ykismobkmp.data.api

import com.ykis.ykismobkmp.data.responses.*
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import ykismobkmp.composeapp.generated.resources.*

@Suppress("NOTHING_TO_INLINE")
val jsonWorker = Json { 
    ignoreUnknownKeys = true 
    coerceInputValues = true
    isLenient = true
    allowSpecialFloatingPointValues = true
}

/**
 * [KtorApiService] — Головний кросплатформовий клієнт мережевих служб ЮКІС м. Южне.
 * УНІФІКОВАНО: Додано обробку помилок десеріалізації JSON.
 */
class KtorApiService(private val client: HttpClient) {
  private val baseUrl = "https://is.yuzhny.com/YkisMobileRest/rest_api/"

  private suspend inline fun <reified T> postFormUrlEncoded(path: String, params: Map<String, String>): T {
    val rawFormBodyBuilder = StringBuilder()
    params.forEach { (key, value) ->
      if (rawFormBodyBuilder.isNotEmpty()) rawFormBodyBuilder.append("&")
      rawFormBodyBuilder.append(key).append("=").append(value)
    }
    val finalRawBody = rawFormBodyBuilder.toString()
    val fullUrl = baseUrl + path

    println("[YkisLogKMP.Network]: >>> ОТПРАВКА ЗАПРОСА >>>")
    println("[YkisLogKMP.Network]: URL: $fullUrl")
    println("[YkisLogKMP.Network]: BODY: $finalRawBody")

    val response: HttpResponse = client.post(fullUrl) {
      header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
      header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      setBody(finalRawBody)
    }

    val rawJsonText = response.bodyAsText()
    println("[YkisLogKMP.Network]: <<< ОТВЕТ СЕРВЕРА <<<")
    println("[YkisLogKMP.Network]: TEXT: $rawJsonText")
    
    return try {
        jsonWorker.decodeFromString<T>(rawJsonText)
    } catch (e: Exception) {
        println("[YkisLogKMP.Network_ERROR]: Сбой парсинга JSON для $path: ${e.message}")
        throw Exception("JSON_PARSE_ERROR")
    }
  }

  suspend fun getApartmentList(params: Map<String, String>) = postFormUrlEncoded<GetApartmentsResponse>("getApartmentsByUser.php", params)
  suspend fun getApartment(params: Map<String, String>) = postFormUrlEncoded<GetApartmentResponse>("getFlatById.php", params)
  suspend fun addApartment(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("addMyFlatByUser.php", params)
  suspend fun deleteApartment(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("deleteFlatByUser.php", params)
  suspend fun getOsbbApartmentsList(params: Map<String, String>) = postFormUrlEncoded<GetApartmentsResponse>("getOsbbApartmentsList.php", params)
  suspend fun verifyAdminSecretWord(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("getSecretCode.php", params)
  suspend fun getRaionList(params: Map<String, String>) = postFormUrlEncoded<GetRaionsResponse>("getRaionList.php", params)
  suspend fun getHouseByRaionList(params: Map<String, String>) = postFormUrlEncoded<GetHousesResponse>("getHousesByRaion.php", params)
  suspend fun saveUserUid(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("saveUserUid.php", params)
  suspend fun deleteUserAccount(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("deleteUserAccount.php", params)
  suspend fun updateBti(params: Map<String, String>) = postFormUrlEncoded<GetApartmentsResponse>("updateBti.php", params)
  suspend fun getFamilyList(params: Map<String, String>) = postFormUrlEncoded<GetFamilyResponse>("getFamilyFromFlat.php", params)

  // Счётчики Водоканала
  suspend fun getWaterMeterList(params: Map<String, String>) = postFormUrlEncoded<GetWaterMeterResponse>("getWaterMeter.php", params)
  suspend fun getWaterReadings(params: Map<String, String>) = postFormUrlEncoded<GetWaterReadingsResponse>("getWaterReadings.php", params)
  suspend fun addWaterReading(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("addCurrentWaterReading.php", params)
  suspend fun deleteLastWaterReading(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("deleteCurrentWaterReading.php", params)
  suspend fun getLastWaterReading(params: Map<String, String>) = postFormUrlEncoded<GetLastWaterReadingResponse>("getLastWaterReading.php", params)

  // Счётчики Теплосети
  suspend fun getHeatMeterList(params: Map<String, String>) = postFormUrlEncoded<GetHeatMeterResponse>("getHeatMeter.php", params)
  suspend fun getHeatReadings(params: Map<String, String>) = postFormUrlEncoded<GetHeatReadingResponse>("getHeatReadings.php", params)
  suspend fun addHeatReading(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("addCurrentHeatReading.php", params)
  suspend fun getLastHeatReading(params: Map<String, String>) = postFormUrlEncoded<GetLastHeatReadingResponse>("getLastHeatReading.php", params)
  suspend fun deleteLastHeatReading(params: Map<String, String>) = postFormUrlEncoded<GetSimpleResponse>("deleteCurrentHeatReading.php", params)

  // Финансы
  suspend fun getFlatService(params: Map<String, String>) = postFormUrlEncoded<GetServiceResponse>("getFlatServices.php", params)
  suspend fun getFastpayTokenByOsbb(params: Map<String, String>) = postFormUrlEncoded<GetFastpayTokensResponse>("getFastpayTokenByOsbb.php", params)
}
