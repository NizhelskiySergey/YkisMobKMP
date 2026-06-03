package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RaionEntity(
  @SerialName("raionId")
  val raionId: Long = 0L,

  @SerialName("raion")
  val raion: String = ""
)

