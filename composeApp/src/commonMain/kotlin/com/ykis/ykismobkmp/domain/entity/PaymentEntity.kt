package com.ykis.ykismobkmp.domain.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentEntity(
  @SerialName("rec_id")
  val recID: Int = 0,

  @SerialName("address_id")
  val addressId: Int = 0,

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

