package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [WaterMeterEntity] — Кроссплатформенная доменная модель водомера г. Южный.
 * Полностью типизирована под Long идентификаторы и готова к десериализации на любой ОС.
 */
@Serializable
data class WaterMeterEntity(
  // ИСПРАВЛЕНО: Все ключевые системные ID переведены на тип Long под стандарты SQLDelight и Ktor
  @SerialName("vodomer_id")
  val vodomerId: Long = 0L,

  @SerialName("dvodomer_id")
  val dvodomerId: Long = 0L,

  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("nomer")
  val nomer: String = "Unknown",

  @SerialName("model")
  val model: String = "Unknown",

  // ИСПРАВЛЕНО: Платформозависимый Byte изменен на универсальный Int для стабильности на Mac/iOS
  @SerialName("st")
  val st: Long = 1L,

  @SerialName("voda")
  val voda: String = "Unknown",

  @SerialName("place")
  val place: String = "Unknown",

  @SerialName("position")
  val position: String = "Unknown",

  @SerialName("sdate")
  val sdate: String = "Unknown",

  @SerialName("fpdate")
  val fpdate: String = "Unknown",

  @SerialName("pdate")
  val pdate: String = "Unknown",

  @SerialName("pp")
  val pp: Long = 0L,

  @SerialName("zdate")
  val zdate: String = "Unknown",

  @SerialName("avg")
  val avg: Long = 0L,

  @SerialName("spisan")
  val spisan: Long = 0L,

  @SerialName("out")
  val isOut: Long = 0L,

  @SerialName("paused")
  val paused: Long = 0L,

  @SerialName("data_spis")
  val dataSpis: String = "Unknown",

  @SerialName("work")
  val work: Long = 0L
)

