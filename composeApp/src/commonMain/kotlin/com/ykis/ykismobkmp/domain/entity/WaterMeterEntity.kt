package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WaterMeterEntity(
  @SerialName("vodomer_id")
  val vodomerId: Int = 0,

  @SerialName("dvodomer_id")
  val dvodomerId: Int = 0,

  @SerialName("address_id")
  val addressId: Int = 0,

  val nomer: String = "Unknown",
  val model: String = "Unknown",
  val st: Byte = 1,
  val voda: String = "Unknown",
  val place: String = "Unknown",
  val position: String = "Unknown",
  val sdate: String = "Unknown",
  val fpdate: String = "Unknown",
  val pdate: String = "Unknown",
  val pp: Byte = 0,
  val zdate: String = "Unknown",
  val avg: Byte = 0,
  val spisan: Byte = 0,
  val out: Byte = 0,
  val paused: Byte = 0,
  @SerialName("data_spis")
  val dataSpis: String = "Unknown",
  val work: Byte = 0
)

