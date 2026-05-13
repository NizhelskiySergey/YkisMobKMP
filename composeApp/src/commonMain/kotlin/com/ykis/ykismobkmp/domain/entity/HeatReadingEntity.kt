package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeatReadingEntity(
  @SerialName("address_id")
  val addressId: Int = 0,

  @SerialName("pok_id")
  val pokId: Int = 0,

  @SerialName("teplomer_id")
  val teplomerId: Int = 0,

  @SerialName("date_readings")
  val dateReading: String = "Unknown",

  @SerialName("date_ot")
  val dateOt: String = "2024-01-01",

  @SerialName("date_do")
  val dateDo: String = "2024-01-01",

  val edizm: String = "Unknown",
  val koef: String = "Unknown",
  val days: Short = 0,
  val last: Double = 0.0,
  val current: Double = 0.0,
  val gkal: Double = 0.0,
  val avg: Byte = 0,
  val tarif: Double = 0.0,
  val qty: Double = 0.0,

  @SerialName("pok_ot")
  val pokOt: String = "Unknown",

  @SerialName("pok_do")
  val pokDo: String = "Unknown",

  @SerialName("gkal_rasch")
  val gkalRasch: String = "Unknown",

  @SerialName("gkal_day")
  val gkalDay: String = "Unknown",

  @SerialName("qty_day")
  val qtyDay: String = "Unknown",

  @SerialName("day_avg")
  val dayAvg: String = "Unknown",

  @SerialName("data_in")
  val dateIn: String = "Unknown",

  val operator: String = "Unknown"
)

