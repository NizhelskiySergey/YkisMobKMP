package com.ykis.ykismobkmp.data.water

import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse
import com.ykis.ykismobkmp.domain.repository.meter.AddWaterReadingParams


interface WaterMeterRemoteRepository {
  suspend fun getWaterMeterList(uid: String, addressId: Int): GetWaterMeterResponse
  suspend fun getWaterReadings(uid: String, vodomerId: Int): GetWaterReadingsResponse
  suspend fun getLastWaterReading(uid: String, vodomerId: Int): GetLastWaterReadingResponse
  suspend fun addWaterReading(params: AddWaterReadingParams): GetSimpleResponse
  suspend fun deleteLastReading(uid: String, readingId: Int): GetSimpleResponse
}
