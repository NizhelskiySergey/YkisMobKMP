package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [FamilyEntity] — Кроссплатформенная доменная модель члена семьи лицевого счета ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Все идентификаторы и числовые флаги переведены на тип Long
 * для 100% устранения ошибок 'Argument type mismatch' при работе со схемами SQLDelight 2.x!
 * Намертво зафиксирован для полной замены.
 */
@Serializable
data class FamilyEntity(
  @SerialName("rec_id")
  val recId: Long = 0L,

  @SerialName("address_id")
  val addressId: Long = 0L,

  @SerialName("rodstvo")
  val rodstvo: String = "Unknown",

  @SerialName("firstname")
  val fistname: String = "Unknown",

  @SerialName("lastname")
  val lastname: String = "Unknown",

  @SerialName("surname")
  val surname: String = "Unknown",

  @SerialName("born")
  val born: String = "Unknown",

  @SerialName("sex")
  val sex: String = "Unknown",

  @SerialName("phone")
  val phone: String = "Unknown",

  @SerialName("subsidia")
  val subsidia: Long = 0L,

  @SerialName("vkl")
  val vkl: Long = 0L,

  @SerialName("inn")
  val inn: String = "Unknown",

  @SerialName("document")
  val document: String = "Unknown",

  @SerialName("seria")
  val seria: String = "Unknown",

  @SerialName("nomer")
  val nomer: String = "Unknown",

  @SerialName("datav")
  val datav: String? = "Unknown",

  @SerialName("organ")
  val organ: String = "Unknown"
)

