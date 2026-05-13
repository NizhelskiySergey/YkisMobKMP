package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetApartmentResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  @SerialName("apartment")
  val apartment: ApartmentEntity? = null
) : BaseResponse
