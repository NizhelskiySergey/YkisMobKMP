package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.remote.meter.MeterRemoteRepository
import com.ykis.ykismobkmp.data.responses.*
import org.jetbrains.compose.resources.getString
import com.ykis.ykismobkmp.*

/**
 * [MeterRepositoryImpl] — Реалізація репозиторію лічильників.
 * УНІФІКОВАНО: Обробка помилок через Res.string.
 */
class MeterRepositoryImpl(
  private val remote: MeterRemoteRepository,
) : MeterRepository {

  private val className = "MeterRepositoryImpl"

  override suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse {
    return try {
      remote.getWaterMeterList(uid, addressId)
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.getWaterMeterList]: [ERROR] ${e.message}")
      GetWaterMeterResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse {
    return try {
      remote.getWaterReadings(uid, vodomerId)
    } catch (e: Exception) {
      GetWaterReadingsResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun getLastWaterReading(uid: String, meterId: Long): GetLastWaterReadingResponse {
    return try {
      remote.getLastWaterReading(uid, meterId)
    } catch (e: Exception) {
      GetLastWaterReadingResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun addWaterReading(uid: String, vodomerId: Long, currentValue: Long, newValue: Long): GetSimpleResponse {
    return try {
      remote.addWaterReading(uid, vodomerId, currentValue, newValue)
    } catch (e: Exception) {
      GetSimpleResponse(success = 0, message = getString(Res.string.error_add_reading))
    }
  }

  override suspend fun deleteLastWaterReading(uid: String, pokId: Long): GetSimpleResponse {
    return try {
      remote.deleteLastWaterReading(uid, pokId)
    } catch (e: Exception) {
      GetSimpleResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse {
    return try {
      remote.getHeatMeterList(uid, addressId)
    } catch (e: Exception) {
      GetHeatMeterResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse {
    return try {
      remote.getHeatReadings(uid, teplomerId)
    } catch (e: Exception) {
      GetHeatReadingResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun getLastHeatReading(uid: String, teplomerId: Long): GetLastHeatReadingResponse {
    return try {
      remote.getLastHeatReading(uid, teplomerId)
    } catch (e: Exception) {
      GetLastHeatReadingResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }

  override suspend fun addHeatReading(uid: String, teplomerId: Long, currentValue: Double, newValue: Double): GetSimpleResponse {
    return try {
      remote.addHeatReading(uid, teplomerId, currentValue, newValue)
    } catch (e: Exception) {
      GetSimpleResponse(success = 0, message = getString(Res.string.error_add_reading))
    }
  }

  override suspend fun deleteLastHeatReading(uid: String, pokId: Long): GetSimpleResponse {
    return try {
      remote.deleteLastHeatReading(uid, pokId)
    } catch (e: Exception) {
      GetSimpleResponse(success = 0, message = getString(Res.string.error_network_request_failed))
    }
  }
}
