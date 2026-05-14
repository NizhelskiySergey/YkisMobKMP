package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

data class AddHeatReadingParams(
  val uid: String,
  val meterId: Long,
  val newValue: Double,
  val currentValue: Double
)

interface HeatMeterRepository {
  suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse
  suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse
  suspend fun getLastHeatReading(uid: String, teplomerId: Long): GetLastHeatReadingResponse
  suspend fun addHeatReading(params: AddHeatReadingParams): GetSimpleResponse
  suspend fun deleteLastHeatReading(uid: String, readingId: Long): GetSimpleResponse
}
