package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WaterReadingEntity(
  @SerialName("address_id")
  val addressId: Int = 0,

  @SerialName("pok_id")
  val pokId: Int = 0,

  @SerialName("vodomer_id")
  val vodomerId: Int = 0,

  @SerialName("date_ot")
  val dateOt: String = "2024-01-01",

  @SerialName("date_do")
  val dateDo: String = "2024-01-01",

  val days: Int = 0,
  val last: Int = 0,
  val current: Int = 15,
  val kub: Int = 0,
  val avg: Byte = 0,

  @SerialName("pok_ot")
  val pokOt: Int = 0,

  @SerialName("pok_do")
  val pokDo: Int = 0,

  val rday: Int = 0,

  @SerialName("kub_day")
  val kubDay: Double = 0.0,

  @SerialName("qty_kub")
  val qtyKub: Int = 0,

  // Внимание: в вашем исходнике было два @SerialName("data_in"),
  // переименовал один в operator для корректного маппинга
  @SerialName("data_in_op")
  val operator: String = "Unknown",

  @SerialName("date_readings")
  val dateReadings: String = "Unknown",

  @SerialName("tarif_xv")
  val tarifXv: Double = 0.0,

  val xvoda: Double = 0.0,

  @SerialName("tarif_st")
  val tarifvSt: Double = 0.0,

  val stoki: Double = 0.0,

  @SerialName("date_st")
  val dateSt: String = "Unknown",

  @SerialName("date_fin")
  val dateFin: String = "Unknown",

  val mday: Int = 0,

  @SerialName("date_in")
  val dateIn: String = "Unknown",
)

