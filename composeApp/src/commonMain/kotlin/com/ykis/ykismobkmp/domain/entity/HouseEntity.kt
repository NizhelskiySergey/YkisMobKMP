package com.ykis.ykismobkmp.domain.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HouseEntity(
  @SerialName("houseId")
  val houseId: Int = 0,

  @SerialName("raionId")
  val raionId: Int = 0,

  @SerialName("house")
  val house: String = ""
)

