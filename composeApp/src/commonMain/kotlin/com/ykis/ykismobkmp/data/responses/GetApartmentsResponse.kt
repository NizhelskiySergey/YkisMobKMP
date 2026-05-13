package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetApartmentsResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  @SerialName("apartments")
    val apartments: List<ApartmentEntity> = emptyList()
) : BaseResponse
