package com.ykis.ykismobkmp.data.responses


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [GetSimpleResponse] — Кроссплатформенная КМР-модель простого ответа API биллинга Южного.
 * Полностью типизирована под Long для исключения ошибок Return/Argument type mismatch в Use Cases.
 */
@Serializable
data class GetSimpleResponse(
  @SerialName("success")
  val success: Int = 0,

  @SerialName("message")
  val message: String = "",

  // ИСПРАВЛЕНО: addressId переведен на тип Long под схемы SQLDelight и контракты Firebase
  @SerialName("addressId")
  val addressId: Long = 0L,

  // ИСПРАВЛЕНО: Безопасное nullable-поле для защиты от SerializationException при ошибках PHP
  @SerialName("address")
  val address: String? = null,

  @SerialName("userRole")
  val userRole: String = "StandardUser",

  // ИСПРАВЛЕНО: osbbId переведен на тип Long под сквозную архитектуру ЮКИС
  @SerialName("osbbId")
  val osbbId: Long = 0L,

  // ИСПРАВЛЕНО: Безопасное nullable-поле для защиты от пустых ответов бэкенда
  @SerialName("osbb")
  val osbb: String? = null,

  @SerialName("nanim")
  val nanim: String? = null
)

