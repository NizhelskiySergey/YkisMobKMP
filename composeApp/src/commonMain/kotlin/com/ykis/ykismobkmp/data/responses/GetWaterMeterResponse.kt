package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetWaterMeterResponse(
  @SerialName("success") override val success: Int,
  @SerialName("message") override val message: String,
  @SerialName("water_meters")
  val waterMeters: List<WaterMeterEntity> = emptyList()
) : BaseResponse

@Serializable
class GetLastWaterReadingResponse(
  @SerialName("success") override val success: Int,
  @SerialName("message") override val message: String,
  @SerialName("water_reading")
  val waterReading: WaterReadingEntity? = null
) : BaseResponse

@Serializable
class GetWaterReadingsResponse(
  @SerialName("success") override val success: Int,
  @SerialName("message") override val message: String,
  @SerialName("water_readings")
  val waterReadings: List<WaterReadingEntity> = emptyList()
) : BaseResponse
