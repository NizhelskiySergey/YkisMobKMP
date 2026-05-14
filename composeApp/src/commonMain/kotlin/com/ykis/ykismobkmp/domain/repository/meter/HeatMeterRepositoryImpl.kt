package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.core.Constants.ADDRESS_ID
import com.ykis.ykismobkmp.core.Constants.POK_ID
import com.ykis.ykismobkmp.core.Constants.TEPLOMER_ID
import com.ykis.ykismobkmp.core.Constants.UID
import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.*

import com.ykis.ykismobkmp.domain.repository.meter.HeatMeterRepository
import com.ykis.ykismobkmp.domain.repository.meter.AddHeatReadingParams
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
      apiService.getHeatMeterList(addressId, uid)
    } catch (ex: Exception) {
      println("[$className.getHeatMeterList] Error: ${ex.message}")
      GetHeatMeterResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse {
    println("[$className.getHeatReadings]: uid=${uid.takeLast(5)}, teplomerId=$teplomerId")
    return try {
      apiService.getHeatReadings(teplomerId, uid)
    } catch (ex: Exception) {
      println("[$className.getHeatReadings] Error: ${ex.message}")
      GetHeatReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getLastHeatReading(uid: String, teplomerId: Long): GetLastHeatReadingResponse {
    println("[$className.getLastHeatReading]: uid=${uid.takeLast(5)}, teplomerId=$teplomerId")
    return try {
      apiService.getLastHeatReading(teplomerId, uid)
    } catch (ex: Exception) {
      println("[$className.getLastHeatReading] Error: ${ex.message}")
      GetLastHeatReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun addHeatReading(params: AddHeatReadingParams): GetSimpleResponse {
    println("[$className.addHeatReading]: uid=${params.uid.takeLast(5)}, meterId=${params.meterId}")
    return try {
      apiService.addHeatReading(
        teplomerId = params.meterId,
        currentValue = params.currentValue,
        newValue = params.newValue,
        uid = params.uid
      )
    } catch (ex: Exception) {
      println("[$className.addHeatReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun deleteLastHeatReading(uid: String, readingId: Long): GetSimpleResponse {
    println("[$className.deleteLastHeatReading]: uid=${uid.takeLast(5)}, readingId=$readingId")
    return try {
      apiService.deleteLastHeatReading(readingId, uid)
    } catch (ex: Exception) {
      println("[$className.deleteLastHeatReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }
}
