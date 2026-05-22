package com.ykis.ykismobkmp.domain.repository.ledger


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [LedgerParams] — Единый КМР-стандарт параметров для работы с финансово-бухгалтерскими
 * начислениями, балансами и квитанциями (Ledger) расчетного центра ЮКИС.
 */
@Serializable
data class LedgerParams(
  @SerialName("uid")
  val uid: String,

  @SerialName("address_id")
  val addressId: Long,

  @SerialName("house_id")
  val houseId: Long,

  @SerialName("service")
  val service: Byte,

  @SerialName("total")
  val total: Byte,

  @SerialName("year")
  val year: String
)

