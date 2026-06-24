package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import com.ykis.ykismobkmp.core.utils.SmartDoubleSerializer

/**
 * [HeatReadingEntity] — Доменна модель історії показань тепла.
 * УНІФІКОВАНО: Використовуються Smart-серіалізатори для парсингу JSON.
 */
@Serializable
data class HeatReadingEntity(
  @Serializable(with = SmartLongSerializer::class)
  @SerialName("address_id")
  val addressId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("pok_id")
  val pokId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("teplomer_id")
  val teplomerId: Long = 0L,

  @SerialName("date_readings")
  val dateReading: String = "Unknown",

  @SerialName("date_ot")
  val dateOt: String = "2024-01-01",

  @SerialName("date_do")
  val dateDo: String = "2024-01-01",

  @SerialName("edizm")
  val edizm: String = "Unknown",

  @SerialName("koef")
  val koef: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("days")
  val days: Long = 0L,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("last")
  val last: Double = 0.0,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("current")
  val current: Double = 0.0,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("gkal")
  val gkal: Double = 0.0,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("avg")
  val avg: Long = 0L,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("tarif")
  val tarif: Double = 0.0,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("qty")
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

  @SerialName("operator")
  val operator: String = "Unknown"
)
