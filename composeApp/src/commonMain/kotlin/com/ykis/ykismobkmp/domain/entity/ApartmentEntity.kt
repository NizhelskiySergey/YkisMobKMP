package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ApartmentEntity(
  val uid: String? = null,

  @SerialName("address_id")
  val addressId: Long = 0,

  val address: String = "",
  val email: String = "example@email.com",
  val kod: String = "1111111111",
  val phone: String = "+38111111111",
  val nanim: String = "Иванов Иван Иванович",
  val order: String = "65-2020",

  @SerialName("data")
  val dataOrder: String = "1997-11-23",

  @SerialName("area_full")
  val areaFull: Double = 51.00,

  @SerialName("area_life")
  val areaLife: Double = 31.20,

  @SerialName("area_dop")
  val areaDop: Double = 7.52,

  @SerialName("area_balk")
  val areaBalk: Double = 4.25,

  @SerialName("area_otopl")
  val areaOtopl: Double = 51.00,

  val tenant: Int = 2,
  val podnan: Int = 0,
  val absent: Int = 1,

  @SerialName("tenant_tbo")
  val tenantTbo: Int = 1,

  val room: Int = 2,
  val privat: Byte = 1,
  val lift: Byte = 1,

  @SerialName("raion_id")
  val blockId: Int = 4,

  @SerialName("house_id")
  val houseId: Long = 23,

  val fio: String = "Иванов Иван Иванович",

  val subsidia: Byte = 0,
  val vxvoda: Byte = 0,
  val teplomer: Byte = 0,
  val distributor: Byte = 0,
  val kvartplata: Byte = 0,
  val otoplenie: Byte = 0,
  val ateplo: Byte = 0,
  val podogrev: Byte = 0,
  val voda: Byte = 0,
  val stoki: Byte = 0,
  val avoda: Byte = 0,
  val astoki: Byte = 0,
  val tbo: Byte = 0,

  @SerialName("aggr_kv")
  val aggrKv: Byte = 0,

  @SerialName("aggr_voda")
  val aggrVoda: Byte = 0,

  @SerialName("aggr_teplo")
  val aggrTeplo: Byte = 0,

  @SerialName("aggr_tbo")
  val aggrTbo: Byte = 0,

  val boiler: Byte = 0,
  val enaudit: Int = 0,
  val heated: Byte = 0,
  val ztp: Byte = 0,
  val ovu: Byte = 0,
  val paused: Byte = 0,
  val osmd: Byte = 0,

  @SerialName("osmd_id")
  val osmdId: Int = 0,

  val osbb: String? = null,

  @SerialName("what_change")
  val whatChange: String = "Unknown",

  @SerialName("data_change")
  val dataChange: String = "Unknown",

  @SerialName("enaudit_id")
  val enaudit_id: Int = 0,

  @SerialName("tarif_kv")
  val tarifKv: Double = 0.00,

  @SerialName("tarif_ot")
  val tarifOt: Double = 0.00,

  @SerialName("tarif_aot")
  val tarifAot: Double = 0.00,

  @SerialName("tarif_gv")
  val tarifGv: Double = 0.00,

  @SerialName("tarif_xv")
  val tarifXv: Double = 0.00,

  @SerialName("tarif_st")
  val tarifSt: Double = 0.00,

  @SerialName("tarif_tbo")
  val tarifTbo: Double = 0.00,

  val tne: Double = 0.00,
  val kte: Double = 0.00,
  val length: Double = 0.00,
  val diametr: Double = 0.00,

  @SerialName("dvodomer_id")
  val dvodomerId: Int = 0,

  @SerialName("dteplomer_id")
  val dteplomerId: Int = 0,
  @SerialName("operator")
  val operator: String = "Unknown",

  @SerialName("data_in")
  val dataIn: String = "Unknown",

  val ipay: Int = 0,
  val pb: Int = 0,
  val mtb: Int = 0
)

