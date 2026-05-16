package com.ykis.ykismobkmp.domain.repository.meter


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * [MeterReadingsParams] — Единый КМР-стандарт параметров передачи новых показаний счетчиков (Вода / Тепло).
 * Полностью автономен, застрахован от SerializationException и готов к десериализации Ktor.
 */
@Serializable
data class MeterReadingsParams(
  @SerialName("uid")
  val uid: String,

  @SerialName("meter_id") // Универсальный ключ для vodomer_id или teplomer_id в API Южного
  val meterId: Long,

  @SerialName("new_value")
  val newValue: Double,

  @SerialName("current_value")
  val currentValue: Double
)

