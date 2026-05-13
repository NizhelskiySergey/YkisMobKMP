package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RaionEntity(
  @SerialName("raionId")
  val raionId: Int = 0,

  @SerialName("raion")
  val raion: String = ""
)

