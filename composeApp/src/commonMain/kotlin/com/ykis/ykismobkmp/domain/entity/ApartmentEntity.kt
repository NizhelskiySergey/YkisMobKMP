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

  // ИСПРАВЛЕНО НАМЕРТВО: Все числовые ЖКХ-поля и счетчики переведены на тип Long под стандарты СУБД
  val tenant: Long = 0L,
  val podnan: Long = 0L,
  val absent: Long = 0L,

  @SerialName("tenant_tbo")
  val tenantTbo: Long = 0L,

  val room: Long = 0L,

  // ИСПРАВЛЕНО НАМЕРТВО: Поля флагов БТИ изменены на Long для исключения mismatch ошибок в when-блоках
  val privat: Long = 0L,
  val lift: Long = 0L,

  // ИСПРАВЛЕНО: Идентификаторы регионов и домов переведены на тип Long под СУБД SQLDelight
  @SerialName("house_id")
  val houseId: Long = 0L,

  @SerialName("raion_id")
  val blockId: Long = 0L,

  val fio: String = "Unknown",

  val subsidia: Long = 0L,
  val vxvoda: Long = 0L,
  val teplomer: Long = 0L,
  val distributor: Long = 0L,
  val kvartplata: Long = 0L,
  val otoplenie: Long = 0L,
  val ateplo: Long = 0L,
  val podogrev: Long = 0L,
  val voda: Long = 0L,
  val stoki: Long = 0L,
  val avoda: Long = 0L,
  val astoki: Long = 0L,
  val tbo: Long = 0L,

  @SerialName("aggr_kv")
  val aggrKv: Long = 0L,

  @SerialName("aggr_voda")
  val aggrVoda: Long = 0L,

  @SerialName("aggr_teplo")
  val aggrTeplo: Long = 0L,

  @SerialName("aggr_tbo")
  val aggrTbo: Long = 0L,

  val boiler: Long = 0L,
  val enaudit: Long = 0L,
  val heated: Long = 0L,
  val ztp: Long = 0L,
  val ovu: Long = 0L,
  val paused: Long = 0L,
  val osmd: Long = 0L,

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

  val ipay: Long = 0L,
  val pb: Long = 0L,
  val mtb: Long = 0L
)
