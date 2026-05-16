package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.*

private const val className = "WaterMeterRepositoryImpl"

class WaterMeterRepositoryImpl(
  private val apiService: KtorApiService
) : WaterMeterRepository {

  override suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse {
    println("[$className.getWaterMeterList]: uid=${uid.takeLast(5)}, addressId=$addressId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "addressId" to addressId.toString()
      )
      apiService.getWaterMeterList(params )
    } catch (ex: Exception) {
      println("[$className.getWaterMeterList] Error: ${ex.message}")
      GetWaterMeterResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse {
    println("[$className.getWaterReadings]: uid=${uid.takeLast(5)}, vodomerId=$vodomerId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "vodomerId" to vodomerId.toString()
      )
      apiService.getWaterReadings(params )
    } catch (ex: Exception) {
      println("[$className.getWaterReadings] Error: ${ex.message}")
      GetWaterReadingsResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getLastWaterReading(uid: String, vodomerId: Long): GetLastWaterReadingResponse {
    println("[$className.getLastWaterReading]: uid=${uid.takeLast(5)}, vodomerId=$vodomerId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "vodomerId" to vodomerId.toString()
      )
      apiService.getLastWaterReading(params )
    } catch (ex: Exception) {
      println("[$className.getLastWaterReading] Error: ${ex.message}")
      GetLastWaterReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun addWaterReading(params: MeterReadingsParams): GetSimpleResponse {
    println("[$className.addWaterReading]: uid=${params.uid.takeLast(5)}, meterId=${params.meterId}")
    return try {
      val params = mapOf(
        "uid" to params.uid,
        "vodomerId" to params.meterId.toString(),
        "currentValue" to params.currentValue.toString(),
        "newValue" to params.newValue.toString()
      )
      apiService.addWaterReading(params)
    } catch (ex: Exception) {
      println("[$className.addWaterReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }


  override suspend fun deleteLastWaterReading(uid: String, pokId: Long): GetSimpleResponse {
    println("[$className.deleteLastWaterReading]: uid=${uid.takeLast(5)}, readingId=$pokId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "pokId" to pokId.toString()
      )
      apiService.deleteLastWaterReading(params, )
    } catch (ex: Exception) {
      println("[$className.deleteLastWaterReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }
}
