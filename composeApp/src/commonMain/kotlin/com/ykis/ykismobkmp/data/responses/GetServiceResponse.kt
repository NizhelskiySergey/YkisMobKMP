package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetServiceResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  // В JSON от PHP поле со списком услуг
  @SerialName("services")
  val services: List<ServiceEntity> = emptyList()
) : BaseResponse
