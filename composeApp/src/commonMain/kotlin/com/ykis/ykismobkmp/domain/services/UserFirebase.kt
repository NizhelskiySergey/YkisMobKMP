package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [UserFirebase] — Кроссплатформенная сериализуемая модель пользователя.
 * ФІКС: fcmTokens тепер null за замовчуванням для запобігання RangeError у JS.
 */
@Serializable
data class UserFirebase(
  @SerialName("uid")
  val uid: String = "",

  @SerialName("email")
  val email: String = "",

  @SerialName("isEmailVerification")
  val isEmailVerification: Boolean = false,

  @SerialName("provider")
  val provider: String? = null,

  @SerialName("displayName") 
  val name: String? = null,

  @SerialName("phone")
  val phone: String? = null,

  @SerialName("photoUrl")
  val photoUrl: String? = null,

  @SerialName("userRole")
  val userRole: String = "StandardUser",

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("osbbId")
  val osbbId: Long = 0L,

  @Serializable(with = SmartLongSerializer::class)
  @SerialName("addressId")
  val addressId: Long = 0L,

  @SerialName("fio")
  val fio: String = "",

  @SerialName("osbb")
  val osbb: String? = null,

  @SerialName("fcmTokens")
  val fcmTokens: List<String>? = null // Null замість emptyList() для стабільності Web
)

fun UserFirebase.toEntity(): UserEntity {
  return UserEntity(
    uid = this.uid,
    displayName = this.name ?: this.email,
    photoUrl = this.photoUrl,
    userRole = UserRole.fromString(this.userRole),
    email = this.email,
    address = this.name ?: "",
    fio = this.fio,
    osbbId = this.osbbId,
    addressId = this.addressId,
    osbb = this.osbb ?: "",
    tokens = this.fcmTokens ?: emptyList()
  )
}
