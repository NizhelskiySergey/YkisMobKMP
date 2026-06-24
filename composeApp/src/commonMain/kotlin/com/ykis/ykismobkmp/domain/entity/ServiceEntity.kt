package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import com.ykis.ykismobkmp.core.utils.SmartDoubleSerializer

/**
 * [ServiceEntity] — Доменна модель нарахування ЮКІС.
 * УНІФІКОВАНО: Використання Smart-серіалізаторів для стабільності на всіх платформах.
 */
@Serializable
data class ServiceEntity(
  @Serializable(with = SmartLongSerializer::class)
  @SerialName("address_id")
  var addressId: Long = 0,

  val service: String = "Unknown",
  val service1: String? = "Unknown",
  val service2: String? = "Unknown",
  val service3: String? = "Unknown",
  val service4: String? = "Unknown",
  
  @SerialName("data")
  val data: String = "2000-01-01",

  @Serializable(with = SmartDoubleSerializer::class)
  val zadol: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val zadol1: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val zadol2: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val zadol3: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val zadol4: Double = 0.0,
  
  @Serializable(with = SmartDoubleSerializer::class)
  val nachisleno: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val nachisleno1: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val nachisleno2: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val nachisleno3: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val nachisleno4: Double = 0.0,
  
  @Serializable(with = SmartDoubleSerializer::class)
  val oplacheno: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val oplacheno1: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val oplacheno2: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val oplacheno3: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val oplacheno4: Double = 0.0,
  
  @Serializable(with = SmartDoubleSerializer::class)
  val dolg: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val dolg1: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val dolg2: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val dolg3: Double = 0.0,
  @Serializable(with = SmartDoubleSerializer::class)
  val dolg4: Double = 0.0
)
