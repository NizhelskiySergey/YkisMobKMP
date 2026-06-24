package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import com.ykis.ykismobkmp.core.utils.SmartDoubleSerializer

/**
 * [PaymentEntity] — Доменна модель здійсненого платежу ЮКІС.
 * УНІФІКОВАНО: Стійкість до форматів JSON через Smart-серіалізатори.
 */
@Serializable
data class PaymentEntity(
  @Serializable(with = SmartLongSerializer::class)
  @SerialName("rec_id")
  val recID: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("data")
  val data: String = "Unknown",

  @Serializable(with = SmartDoubleSerializer::class)
  val kvartplata: Double = 0.00,
  @Serializable(with = SmartDoubleSerializer::class)
  val remont: Double = 0.00,
  @Serializable(with = SmartDoubleSerializer::class)
  val otoplenie: Double = 0.00,
  @Serializable(with = SmartDoubleSerializer::class)
  val voda: Double = 0.00,
  @Serializable(with = SmartDoubleSerializer::class)
  val tbo: Double = 0.00,
  @Serializable(with = SmartDoubleSerializer::class)
  val summa: Double = 0.00,

  val prixod: String = "Unknown",
  val kassa: String = "Unknown",
  val nomer: String = "Unknown",

  @SerialName("data_in")
  val dataIn: String = "Unknown"
)
