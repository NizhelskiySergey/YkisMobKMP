package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import com.ykis.ykismobkmp.core.utils.SmartDoubleSerializer

/**
 * [HeatMeterEntity] — Доменна модель теплолічильника ЮТКЕ.
 * ВІДНОВЛЕНО: Всі поля для коректної роботи Мапперів та СУБД.
 */
@Serializable
data class HeatMeterEntity(
  @Serializable(with = SmartLongSerializer::class)
  @SerialName("teplomer_id")
  val teplomerId: Long = 0L,

  @SerialName("num")
  val number: String = "Unknown",

  val model: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("model_id")
  val modelId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("address_id")
  val addressId: Long = 0L,

  val edizm: String = "Unknown",
  val koef: String = "Unknown",

  @Serializable(with = SmartDoubleSerializer::class)
  val area: Double = 0.0,

  val sdate: String = "Unknown",
  val fpdate: String = "Unknown",
  val pdate: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("out")
  val isOut: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  val spisan: Long = 0L,

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
