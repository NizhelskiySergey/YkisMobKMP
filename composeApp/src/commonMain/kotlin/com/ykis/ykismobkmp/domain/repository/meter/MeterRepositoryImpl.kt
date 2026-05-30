package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.remote.meter.MeterRemoteRepository
import com.ykis.ykismobkmp.data.responses.*

class MeterRepositoryImpl(
  private val MeterRemoteRepository: MeterRemoteRepository,
) : MeterRepository {

  override suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse {
    return MeterRemoteRepository.getWaterMeterList(uid, addressId)
  }

  override suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse {
    return MeterRemoteRepository.getWaterReadings(uid, vodomerId)
  }

  override suspend fun getLastWaterReading(
    uid: String,
    vodomerId: Long
  ): GetLastWaterReadingResponse {
    return MeterRemoteRepository.getLastWaterReading(uid, vodomerId)
  }



  override suspend fun addWaterReading(
    uid: String,
    vodomerId: Long,
    currentValue: Long,
    newValue: Long
  ): GetSimpleResponse {
    return MeterRemoteRepository.addWaterReading(uid, vodomerId, currentValue, newValue)
  }

  override suspend fun deleteLastWaterReading(uid: String, readingId: Long): GetSimpleResponse {
    return MeterRemoteRepository.deleteLastWaterReading(uid, readingId)
  }
  override suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse {
    return MeterRemoteRepository.getHeatMeterList(uid, addressId)
  }

  override suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse {
    return MeterRemoteRepository.getHeatReadings(uid, teplomerId)
  }

  override suspend fun getLastHeatReading(uid: String,teplomerId: Long): GetLastHeatReadingResponse {
    return MeterRemoteRepository.getLastHeatReading(uid, teplomerId)
  }

  override suspend fun addHeatReading(
    uid: String,
    teplomerId: Long,
    currentValue: Double,
    newValue: Double
  ): GetSimpleResponse {
    return MeterRemoteRepository.addHeatReading(uid, teplomerId, currentValue, newValue)
  }

  override suspend fun deleteLastHeatReading(uid: String, readingId: Long): GetSimpleResponse {
    return MeterRemoteRepository.deleteLastHeatReading(uid, readingId)
  }



}
