package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.api.KtorApiService
import com.ykis.ykismobkmp.data.responses.*

/**
 * [MeterRepositoryImpl] — Имплементация репозитория счетчиков воды и тепла для API ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Устранено затенение переменных params, класс увязан с актуальным контрактом MeterRepository.
 * Намертво зафиксирован для полной замены.
 */
class MeterRepositoryImpl(
  private val apiService: KtorApiService
) : MeterRepository {

  private val currentClassName = "MeterRepositoryImpl"

  override suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse {
    println("[$currentClassName.getWaterMeterList]: uid=${uid.takeLast(5)}, addressId=$addressId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "addressId" to addressId.toString()
      )
      apiService.getWaterMeterList(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.getWaterMeterList] Error: ${ex.message}")
      GetWaterMeterResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse {
    println("[$currentClassName.getWaterReadings]: uid=${uid.takeLast(5)}, vodomerId=$vodomerId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "vodomerId" to vodomerId.toString()
      )
      apiService.getWaterReadings(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.getWaterReadings] Error: ${ex.message}")
      GetWaterReadingsResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getLastWaterReading(
    uid: String,
    vodomerId: Long
  ): GetLastWaterReadingResponse {
    println("[$currentClassName.getLastWaterReading]: uid=${uid.takeLast(5)}, vodomerId=$vodomerId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "vodomerId" to vodomerId.toString()
      )
      apiService.getLastWaterReading(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.getLastWaterReading] Error: ${ex.message}")
      GetLastWaterReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun addWaterReading(
    uid: String,
    vodomerId: Long,
    currentValue: Long,
    newValue: Long
  ): GetSimpleResponse {
    println(
      "[$currentClassName.addWaterReading]: uid=${uid.takeLast(5)}," +
        " meterId=${vodomerId}, currentValue = ${currentValue} ,newValue = ${currentValue}"
    )
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "vodomerId" to vodomerId.toString(),
        "currentValue" to currentValue.toString(),
        "newValue" to newValue.toString()
      )
      apiService.addWaterReading(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.addWaterReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun deleteLastWaterReading(uid: String, pokId: Long): GetSimpleResponse {
    println("[$currentClassName.deleteLastWaterReading]: uid=${uid.takeLast(5)}, readingId=$pokId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "pokId" to pokId.toString()
      )
      apiService.deleteLastWaterReading(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.deleteLastWaterReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse {
    println("[$currentClassName.getHeatMeterList]: uid=${uid.takeLast(5)}, addressId=$addressId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "addressId" to addressId.toString()
      )
      apiService.getHeatMeterList(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.getHeatMeterList] Error: ${ex.message}")
      GetHeatMeterResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse {
    println("[$currentClassName.getHeatReadings]: uid=${uid.takeLast(5)}, teplomerId=$teplomerId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "teplomerId" to teplomerId.toString()
      )
      apiService.getHeatReadings(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.getHeatReadings] Error: ${ex.message}")
      GetHeatReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun getLastHeatReading(
    uid: String,
    teplomerId: Long
  ): GetLastHeatReadingResponse {
    println("[$currentClassName.getLastHeatReading]: uid=${uid.takeLast(5)}, teplomerId=$teplomerId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "teplomerId" to teplomerId.toString()
      )
      apiService.getLastHeatReading(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.getLastHeatReading] Error: ${ex.message}")
      GetLastHeatReadingResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun addHeatReading(
    uid: String,
    teplomerId: Long,
    currentValue: Double,
    newValue: Double
  ): GetSimpleResponse {
    println(
      "[$currentClassName.addHeatReading]: uid=${uid.takeLast(5)}," +
        " meterId=${teplomerId}, currentValue = ${currentValue} ,newValue = ${currentValue}"
    )
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "teplomerId" to teplomerId.toString(),
        "currentValue" to currentValue.toString(),
        "newValue" to newValue.toString()
      )
      apiService.addHeatReading(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.addHeatReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }

  override suspend fun deleteLastHeatReading(uid: String, pokId: Long): GetSimpleResponse {
    println("[$currentClassName.deleteLastHeatReading]: uid=${uid.takeLast(5)}, readingId=$pokId")
    return try {
      val requestMap = mapOf(
        "uid" to uid,
        "pokId" to pokId.toString()
      )
      apiService.deleteLastHeatReading(requestMap)
    } catch (ex: Exception) {
      println("[$currentClassName.deleteLastHeatReading] Error: ${ex.message}")
      GetSimpleResponse(success = 0, message = ex.message ?: "Невідома помилка мережі")
    }
  }
}
