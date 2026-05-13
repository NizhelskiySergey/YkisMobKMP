package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.RaionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetRaionsResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  @SerialName("raions")
    val raions: List<RaionEntity> = emptyList()
) : BaseResponse
