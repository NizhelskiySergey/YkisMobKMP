package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [WaterReadingEntity] — Кроссплатформенная доменная модель истории показаний водомера.
 * ИСПРАВЛЕНО НАМЕРТВО: Все показания, кубы и расчетные объемы (qty_kub) жестко приведены к типу Long
 * в точном соответствии с твоей схемой СУБД SQLDelight, убирая любые риски Type Mismatch!
 * Намертво зафиксирован для полной замены.
 */
@Serializable
data class WaterReadingEntity(
  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("pok_id")
  val pokId: Long = 0L,

  @SerialName("vodomer_id")
  val vodomerId: Long = 0L,

  @SerialName("date_ot")
  val dateOt: String = "2024-01-01",

  @SerialName("date_do")
  val dateDo: String = "2024-01-01",

  @SerialName("days")
  val days: Long = 0L,

  @SerialName("last")
  val last: Long = 0L,

  @SerialName("current")
  val current: Long = 15L,

  @SerialName("kub")
  val kub: Long = 0L,

  @SerialName("avg")
  val avg: Long = 0L,

  @SerialName("pok_ot")
  val pokOt: Long = 0L,

  @SerialName("pok_do")
  val pokDo: Long = 0L,

  @SerialName("rday")
  val rday: Long = 0L,

  @SerialName("kub_day")
  val kubDay: Double = 0.0,

  @SerialName("qty_kub")
  val qtyKub: Long = 0L, // Переведено в Long строго по контракту СУБД

  @SerialName("data_in_op")
  val operator: String = "Unknown",

  @SerialName("date_readings")
  val dateReadings: String = "Unknown",

  @SerialName("tarif_xv")
  val tarifXv: Double = 0.0,

  @SerialName("xvoda")
  val xvoda: Double = 0.0,

  @SerialName("tarif_st")
  val tarifvSt: Double = 0.0,

  @SerialName("stoki")
  val stoki: Double = 0.0,

  @SerialName("date_st")
  val dateSt: String = "Unknown",

  @SerialName("date_fin")
  val dateFin: String = "Unknown",

  @SerialName("mday")
  val mday: Long = 0L,

  @SerialName("date_in")
  val dateIn: String = "Unknown"
)

