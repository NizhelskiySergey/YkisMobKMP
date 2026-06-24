package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import com.ykis.ykismobkmp.core.utils.SmartDoubleSerializer

/**
 * [WaterReadingEntity] — Доменна модель історії показань води.
 * УНІФІКОВАНО: Стійкість до форматів JSON через Smart-серіалізатори.
 */
@Serializable
data class WaterReadingEntity(
  @Serializable(with = SmartLongSerializer::class)
  @SerialName("address_id")
  val addressId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("pok_id")
  val pokId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("vodomer_id")
  val vodomerId: Long = 0L,

  @SerialName("date_ot")
  val dateOt: String = "2024-01-01",

  @SerialName("date_do")
  val dateDo: String = "2024-01-01",

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("days")
  val days: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("last")
  val last: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("current")
  val current: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("kub")
  val kub: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("avg")
  val avg: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("pok_ot")
  val pokOt: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("pok_do")
  val pokDo: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("rday")
  val rday: Long = 0L,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("kub_day")
  val kubDay: Double = 0.0,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("qty_kub")
  val qtyKub: Long = 0L,

  @SerialName("data_in_op")
  val operator: String = "Unknown",

  @SerialName("date_readings")
  val dateReadings: String = "Unknown",

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("tarif_xv")
  val tarifXv: Double = 0.0,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("xvoda")
  val xvoda: Double = 0.0,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("tarif_st")
  val tarifvSt: Double = 0.0,

  @Serializable(with = SmartDoubleSerializer::class)
  @SerialName("stoki")
  val stoki: Double = 0.0,

  @SerialName("date_st")
  val dateSt: String = "Unknown",

  @SerialName("date_fin")
  val dateFin: String = "Unknown",

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("mday")
  val mday: Long = 0L,

  @SerialName("data_in")
  val dateIn: String = "Unknown"
)
