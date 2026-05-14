package com.ykis.ykismobkmp.domain.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [HeatMeterEntity] — Чистая кроссплатформенная доменная модель счетчика тепла г. Южный.
 * ПОЛНОСТЬЮ ОЧИЩЕНА от Android Room аннотаций для стабильной сборки на Mac Desktop и iOS.
 */
@Serializable
data class HeatMeterEntity(
  // ИСПРАВЛЕНО: Ключевые идентификаторы переведены на тип Long согласно сквозному КМР-стандарту
  @SerialName("teplomer_id")
  val teplomerId: Long = 0L,

  @SerialName("nomer")
  val number: String = "Unknown",

  @SerialName("model")
  val model: String = "Unknown",

  // ИСПРАВЛЕНО: modelId переведен на Long для бесшовной стыковки с СУБД SQLDelight
  @SerialName("model_id")
  val modelId: Long = 0L,

  // ИСПРАВЛЕНО: addressId переведен на Long под архитектуру Use Case и биллинга ЮЖНОГО
  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("edizm")
  val edizm: String = "Unknown",

  @SerialName("koef")
  val koef: String = "Unknown",

  @SerialName("area")
  val area: Double = 0.0,

  @SerialName("sdate")
  val sdate: String = "Unknown",

  @SerialName("fpdate")
  val fpdate: String = "Unknown",

  @SerialName("pdate")
  val pdate: String = "Unknown",

  // ИСПРАВЛЕНО: Платформозависимый Byte изменен на универсальный Kotlin Int
  @SerialName("out")
  val out: Int = 0,

  @SerialName("spisan")
  val spisan: Int = 0,

  @SerialName("data_spis")
  val dataSpis: String = "Unknown",

  @SerialName("work")
  val work: Int = 0
)

