package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.*

private const val className = "HeatMeterRepositoryImpl"

/**
 * [HeatMeterRepositoryImpl] — Очищенная КМР-реализация репозитория счетчиков тепла.
 * Вызывает методы KtorApiServiceImpl напрямую без упаковки в Map.
 */
class HeatMeterRepositoryImpl(
  private val apiService: KtorApiService
) : HeatMeterRepository {

  override suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse {
    println("[$className.getHeatMeterList]: uid=${uid.takeLast(5)}, addressId=$addressId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "addressId" to addressId.toString()
      )
      apiService.getHeatMeterList(params)
    } catch (ex: Exception) {
      println("[$className.getHeatMeterList] Error: ${ex.message}")
      GetHeatMeterResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse {
    println("[$className.getHeatReadings]: uid=${uid.takeLast(5)}, teplomerId=$teplomerId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "teplomerId" to teplomerId.toString()
      )
      apiService.getHeatReadings(params)
    } catch (ex: Exception) {
      println("[$className.getHeatReadings] Error: ${ex.message}")
      GetHeatReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getLastHeatReading(
    uid: String,
    teplomerId: Long
  ): GetLastHeatReadingResponse {
    println("[$className.getLastHeatReading]: uid=${uid.takeLast(5)}, teplomerId=$teplomerId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "teplomerId" to teplomerId.toString()
      )
      apiService.getLastHeatReading(params)
    } catch (ex: Exception) {
      println("[$className.getLastHeatReading] Error: ${ex.message}")
      GetLastHeatReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun addHeatReading(params: MeterReadingsParams): GetSimpleResponse {
    println("[$className.addHeatReading]: uid=${params.uid.takeLast(5)}, meterId=${params.meterId}")
    return try {
      val params = mapOf(
        "uid" to params.uid,
        "teplomerId" to params.meterId.toString(),
        "currentValue" to params.currentValue.toString(),
        "newValue" to params.newValue.toString()
      )
      apiService.addHeatReading(params)
    } catch (ex: Exception) {
      println("[$className.addHeatReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun deleteLastHeatReading(uid: String, pokId: Long): GetSimpleResponse {
    println("[$className.deleteLastHeatReading]: uid=${uid.takeLast(5)}, readingId=$pokId")
    return try {
      val params = mapOf(
        "uid" to uid,
        "pokId" to pokId.toString()
      )
      apiService.deleteLastHeatReading(params)
    } catch (ex: Exception) {
      println("[$className.deleteLastHeatReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }
}
