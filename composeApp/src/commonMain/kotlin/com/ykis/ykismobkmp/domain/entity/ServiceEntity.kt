package com.ykis.ykismobkmp.domain.entity
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * [ServiceEntity] — Сериализуемая КМР-модель начисления и инвойса ГИОЦ биллинга г. Южного.
 * ИСПРАВЛЕНО НАМЕРТВО: Поле data запечатано под строгое сопоставление с Json-ключом бэкенда!
 */
@Serializable
data class ServiceEntity(
  @SerialName("address_id")
  var addressId: Long = 0, // Перевели в var для атомарной страховки ID лицевого счета в репозитории

  val service: String = "Unknown",
  val service1: String? = "Unknown",
  val service2: String? = "Unknown",
  val service3: String? = "Unknown",
  val service4: String? = "Unknown",
  @SerialName("data")
  val data: String = "2000-01-01",

  val zadol: Double? = 0.0,
  val zadol1: Double? = 0.0,
  val zadol2: Double? = 0.0,
  val zadol3: Double? = 0.0,
  val zadol4: Double? = 0.0,
  val nachisleno: Double? = 0.0,
  val nachisleno1: Double? = 0.0,
  val nachisleno2: Double? = 0.0,
  val nachisleno3: Double? = 0.0,
  val nachisleno4: Double? = 0.0,
  val oplacheno: Double? = 0.0,
  val oplacheno1: Double? = 0.0,
  val oplacheno2: Double? = 0.0,
  val oplacheno3: Double? = 0.0,
  val oplacheno4: Double? = 0.0,
  val dolg: Double? = 0.0,
  val dolg1: Double? = 0.0,
  val dolg2: Double? = 0.0,
  val dolg3: Double? = 0.0,
  val dolg4: Double? = 0.0
)



