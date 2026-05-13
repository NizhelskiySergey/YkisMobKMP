package com.ykis.ykismobkmp.data.heat


import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.domain.entity.AddHeatReadingParams

interface HeatMeterRemoteRepository {
    suspend fun getHeatMeterList(uid: String,addressId: Int) : GetHeatMeterResponse
  suspend fun getHeatReadings(uid: String,teplomerId: Int): GetHeatReadingResponse
  suspend fun getLastHeatReading(uid: String,teplomerId: Int): GetLastHeatReadingResponse
  suspend fun addHeatReading(params : AddHeatReadingParams): GetSimpleResponse
  suspend fun deleteLastHeatReading(uid: String,readingId:Int): GetSimpleResponse
}
