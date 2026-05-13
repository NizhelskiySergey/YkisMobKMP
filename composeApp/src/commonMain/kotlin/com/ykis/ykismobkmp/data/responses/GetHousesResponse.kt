package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.HouseEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetHousesResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  @SerialName("houses")
    val houses: List<HouseEntity> = emptyList()
) : BaseResponse
