package com.ykis.ykismobkmp.domain.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [PaymentEntity] — Чистая кроссплатформенная доменная модель записи совершенного платежа ГИОЦ г. Южный.
 * Полностью типизирована под Long-идентификаторы для бесшовной стыковки со схемами SQLDelight 2.x.
 */
@Serializable
data class PaymentEntity(
  // ИСПРАВЛЕНО: recID изменен на Long, так как он является INTEGER PRIMARY KEY в СУБД
  @SerialName("rec_id")
  val recID: Long = 0L,

  // ИСПРАВЛЕНО: addressId переведен на Long под сквозной стандарт лицевых счетов ЮКИС
  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("data")
  val data: String = "Unknown",

  val kvartplata: Double = 0.00,
  val remont: Double = 0.00,
  val otoplenie: Double = 0.00,
  val voda: Double = 0.00,
  val tbo: Double = 0.00,
  val summa: Double = 0.00,

  val prixod: String = "Unknown",
  val kassa: String = "Unknown",
  val nomer: String = "Unknown",

  @SerialName("data_in")
  val dataIn: String = "Unknown"
)


