package com.ykis.ykismobkmp.domain.repository.family

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FamilyEntity(
  @SerialName("rec_id")
  val recId: Int = 0,

  @SerialName("address_id")
  val addressId: Int = 0,

  val rodstvo: String = "Unknown",

  @SerialName("firstname")
  val fistname: String = "Unknown",

  @SerialName("lastname")
  val lastname: String = "Unknown",

  @SerialName("surname")
  val surname: String = "Unknown",

  val born: String = "Unknown",

  val sex: String = "Unknown",

  // В KMP используем val для неизменяемости
  val phone: String = "Unknown",

  // Заменяем Byte на Int для совместимости с SQLite INTEGER без адаптеров
  val subsidia: Int = 0,

  val vkl: Int = 0,

  val inn: String = "Unknown",

  val document: String = "Unknown",

  val seria: String = "Unknown",

  val nomer: String = "Unknown",

  val datav: String? = "Unknown",

  val organ: String = "Unknown"
)

