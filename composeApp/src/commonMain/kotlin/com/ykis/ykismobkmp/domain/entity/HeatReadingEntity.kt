package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [HeatReadingEntity] — Кроссплатформенная доменная модель показаний счетчика тепла.
 * Полностью типизирована под Long для бесшовной интеграции с SQLDelight и Ktor.
 */
@Serializable
data class HeatReadingEntity(
  // ИСПРАВЛЕНО: Все ключевые ЖКХ-идентификаторы переведены на тип Long
  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("pok_id")
  val pokId: Long = 0L,

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

  // ИСПРАВЛЕНО: Платформозависимый Short изменен на универсальный Int
  @SerialName("days")
  val days: Long = 0L,

  @SerialName("last")
  val last: Double = 0.0,

  @SerialName("current")
  val current: Double = 0.0,

  @SerialName("gkal")
  val gkal: Double = 0.0,

  // ИСПРАВЛЕНО: Платформозависимый Byte изменен на универсальный Int
  @SerialName("avg")
  val avg: Long = 0L,

  @SerialName("tarif")
  val tarif: Double = 0.0,

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


