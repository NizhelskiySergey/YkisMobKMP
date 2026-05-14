package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [WaterReadingEntity] — Кроссплатформенная доменная модель истории показаний водомера.
 * Полностью типизирована под сквозной КМР-стандарт Long-идентификаторов и Double-показаний.
 */
@Serializable
data class WaterReadingEntity(
  // ИСПРАВЛЕНО: Все ключевые ЖКХ-идентификаторы переведены на тип Long под СУБД SQLDelight
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
  val days: Int = 0,

  // ИСПРАВЛЕНО: Показания кубометров переведены на Double во избежание SerializationException
  @SerialName("last")
  val last: Double = 0.0,

  @SerialName("current")
  val current: Double = 15.0,

  @SerialName("kub")
  val kub: Double = 0.0,

  // ИСПРАВЛЕНО: Платформозависимый Byte изменен на универсальный Kotlin Int
  @SerialName("avg")
  val avg: Int = 0,

  @SerialName("pok_ot")
  val pokOt: Int = 0,

  @SerialName("pok_do")
  val pokDo: Int = 0,

  @SerialName("rday")
  val rday: Int = 0,

  @SerialName("kub_day")
  val kubDay: Double = 0.0,

  @SerialName("qty_kub")
  val qtyKub: Double = 0.0,

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
  val mday: Int = 0,

  @SerialName("date_in")
  val dateIn: String = "Unknown",
)

