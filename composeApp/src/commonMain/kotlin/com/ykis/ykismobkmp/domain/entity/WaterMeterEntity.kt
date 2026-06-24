package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import com.ykis.ykismobkmp.core.utils.SmartDoubleSerializer

/**
 * [WaterMeterEntity] — Доменна модель лічильника води ЮКІС.
 * ВІДНОВЛЕНО: Всі поля для коректної роботи Мапперів та СУБД.
 */
@Serializable
data class WaterMeterEntity(
  @Serializable(with = SmartLongSerializer::class)
  @SerialName("vodomer_id")
  val vodomerId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("dvodomer_id")
  val dvodomerId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("num")
  val nomer: String = "Unknown",
  
  val model: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  val st: Long = 1L,

  val voda: String = "Unknown",
  val place: String = "Unknown",
  val position: String = "Unknown",

  @SerialName("sdate")
  val sdate: String = "Unknown",

  @SerialName("fpdate")
  val fpdate: String = "Unknown",

  @SerialName("pdate")
  val pdate: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  val pp: Long = 0L,

  @SerialName("zdate")
  val zdate: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  val avg: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  val spisan: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("out")
  val isOut: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  val paused: Long = 0L,

  @SerialName("date_spisan")
  val dataSpis: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  val work: Long = 0L,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("last_pok")
  val lastReading: Double = 0.0,

  @SerialName("last_data")
  val lastDate: String? = null
)
