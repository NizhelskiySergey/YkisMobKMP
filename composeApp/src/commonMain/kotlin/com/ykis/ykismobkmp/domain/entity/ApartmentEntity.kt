package com.ykis.ykismobkmp.domain.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [ApartmentEntity] — Кроссплатформенная доменная модель лицевого счета квартиры биллинга г. Южный.
 * Полностью очищена от платформозависимых типов и готова к нативной десериализации на любой ОС.
 */
@Serializable
data class ApartmentEntity(
  val uid: String? = null,

  // ИСПРАВЛЕНО: Все ключевые ЖКХ-идентификаторы приведены к единому сквозному стандарту Long
  @SerialName("address_id")
  val addressId: Long = 0L,

  val address: String = "",
  val email: String = "example@email.com",
  val kod: String = "1111111111",
  val phone: String = "+38111111111",
  val nanim: String = "Unknown",
  val order: String = "Unknown",

  @SerialName("data")
  val dataOrder: String = "1997-11-23",

  @SerialName("area_full")
  val areaFull: Double = 0.00,

  @SerialName("area_life")
  val areaLife: Double = 0.00,

  @SerialName("area_dop")
  val areaDop: Double = 0.00,

  @SerialName("area_balk")
  val areaBalk: Double = 0.00,

  @SerialName("area_otopl")
  val areaOtopl: Double = 0.00,

  val tenant: Int = 0,
  val podnan: Int = 0,
  val absent: Int = 0,

  @SerialName("tenant_tbo")
  val tenantTbo: Int = 0,

  val room: Int = 0,

  // ИСПРАВЛЕНО: Все платформозависимые Byte изменены на Int для стабильности ОЗУ на Mac/iOS
  val privat: Int = 0,
  val lift: Int = 0,

  // ИСПРАВЛЕНО: Идентификаторы регионов и домов переведены на тип Long под СУБД SQLDelight
  @SerialName("raion_id")
  val blockId: Long = 0L,

  @SerialName("house_id")
  val houseId: Long = 0L,

  val fio: String = "Unknown",

  val subsidia: Int = 0,
  val vxvoda: Int = 0,
  val teplomer: Int = 0,
  val distributor: Int = 0,
  val kvartplata: Int = 0,
  val otoplenie: Int = 0,
  val ateplo: Int = 0,
  val podogrev: Int = 0,
  val voda: Int = 0,
  val stoki: Int = 0,
  val avoda: Int = 0,
  val astoki: Int = 0,
  val tbo: Int = 0,

  @SerialName("aggr_kv")
  val aggrKv: Int = 0,

  @SerialName("aggr_voda")
  val aggrVoda: Int = 0,

  @SerialName("aggr_teplo")
  val aggrTeplo: Int = 0,

  @SerialName("aggr_tbo")
  val aggrTbo: Int = 0,

  val boiler: Int = 0,
  val enaudit: Int = 0,
  val heated: Int = 0,
  val ztp: Int = 0,
  val ovu: Int = 0,
  val paused: Int = 0,
  val osmd: Int = 0,

  @SerialName("osmd_id")
  val osmdId: Long = 0L,

  val osbb: String? = null,

  @SerialName("what_change")
  val whatChange: String = "Unknown",

  @SerialName("data_change")
  val dataChange: String = "Unknown",

  @SerialName("enaudit_id")
  val enaudit_id: Long = 0L,

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

  // ИСПРАВЛЕНО: Идентификаторы связанных приборов учета переведены на тип Long
  @SerialName("dvodomer_id")
  val dvodomerId: Long = 0L,

  @SerialName("dteplomer_id")
  val dteplomerId: Long = 0L,

  @SerialName("operator")
  val operator: String = "Unknown",

  @SerialName("data_in")
  val dataIn: String = "Unknown",

  val ipay: Int = 0,
  val pb: Int = 0,
  val mtb: Int = 0
)


