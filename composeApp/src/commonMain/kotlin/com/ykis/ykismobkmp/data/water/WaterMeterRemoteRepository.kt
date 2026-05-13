package com.ykis.ykismobkmp.data.water

import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse


data class AddWaterReadingParams(
  val uid : String,
  val meterId: Int,
  val newValue: Int,
  val currentValue: Int
)
interface WaterMeterRemoteRepository {
  suspend fun getWaterMeterList(uid: String, addressId: Int): GetWaterMeterResponse
  suspend fun getWaterReadings(uid: String, vodomerId: Int): GetWaterReadingsResponse
  suspend fun getLastWaterReading(uid: String, vodomerId: Int): GetLastWaterReadingResponse
  suspend fun addWaterReading(params: AddWaterReadingParams): GetSimpleResponse
  suspend fun deleteLastReading(uid: String, readingId: Int): GetSimpleResponse
}
