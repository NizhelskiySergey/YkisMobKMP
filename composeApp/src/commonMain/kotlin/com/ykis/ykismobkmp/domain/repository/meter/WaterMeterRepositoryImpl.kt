package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.*

private const val className = "WaterMeterRepositoryImpl"

/**
 * [WaterMeterRepositoryImpl] — Очищенная КМР-реализация репозитория счетчиков воды.
 * Общается напрямую со строго типизированным KtorApiServiceImpl.
 */
class WaterMeterRepositoryImpl(
  private val apiService: KtorApiService
) : WaterMeterRepository {

  override suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse {
    println("[$className.getWaterMeterList]: uid=${uid.takeLast(5)}, addressId=$addressId")
    return try {
      apiService.getWaterMeterList(addressId, uid)
    } catch (ex: Exception) {
      println("[$className.getWaterMeterList] Error: ${ex.message}")
      GetWaterMeterResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse {
    println("[$className.getWaterReadings]: uid=${uid.takeLast(5)}, vodomerId=$vodomerId")
    return try {
      apiService.getWaterReadings(vodomerId, uid)
    } catch (ex: Exception) {
      println("[$className.getWaterReadings] Error: ${ex.message}")
      GetWaterReadingsResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getLastWaterReading(uid: String, vodomerId: Long): GetLastWaterReadingResponse {
    println("[$className.getLastWaterReading]: uid=${uid.takeLast(5)}, vodomerId=$vodomerId")
    return try {
      apiService.getLastWaterReading(vodomerId, uid)
    } catch (ex: Exception) {
      println("[$className.getLastWaterReading] Error: ${ex.message}")
      GetLastWaterReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun addWaterReading(params: AddWaterReadingParams): GetSimpleResponse {
    println("[$className.addWaterReading]: uid=${params.uid.takeLast(5)}, meterId=${params.meterId}")
    return try {
      apiService.addWaterReading(
        vodomerId = params.meterId,
        currentValue = params.currentValue,
        newValue = params.newValue,
        uid = params.uid
      )
    } catch (ex: Exception) {
      println("[$className.addWaterReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun deleteLastWaterReading(uid: String, readingId: Long): GetSimpleResponse {
    println("[$className.deleteLastWaterReading]: uid=${uid.takeLast(5)}, readingId=$readingId")
    return try {
      apiService.deleteLastWaterReading(readingId, uid)
    } catch (ex: Exception) {
      println("[$className.deleteLastWaterReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }
}
