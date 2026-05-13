package com.ykis.ykismobkmp.domain.entity


import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse

data class AddHeatReadingParams(
  val uid: String,
  val meterId: Int,
  val newValue: Double,
  val currentValue: Double

)

interface HeatMeterRepository {
  suspend fun getHeatMeterList(uid: String, addressId: Int): GetHeatMeterResponse
  suspend fun getHeatReadings(uid: String, teplomerId: Int): GetHeatReadingResponse
  suspend fun getLastHeatReading(uid: String, teplomerId: Int): GetLastHeatReadingResponse
  suspend fun addHeatReading(params: AddHeatReadingParams): GetSimpleResponse
  suspend fun deleteLastHeatReading(uid: String, readingId: Int): GetSimpleResponse
}
