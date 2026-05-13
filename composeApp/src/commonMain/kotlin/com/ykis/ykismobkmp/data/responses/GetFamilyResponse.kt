package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetFamilyResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  // Поле из JSON от PHP обычно называется 'family'
  @SerialName("family")
  val family: List<FamilyEntity> = emptyList()
) : BaseResponse
