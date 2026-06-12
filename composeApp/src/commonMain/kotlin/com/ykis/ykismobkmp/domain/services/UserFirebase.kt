package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.domain.entity.UserEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "UserFirebase"

/**
 * [UserFirebase] — Кроссплатформенная сериализуемая модель пользователя для работы с Firestore KMP.
 * ЗАФИКСИРОВАНО: Идентификаторы osbbId и addressId имеют жесткий сквозной тип Long под каноны SQLDelight.
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

  @SerialName("displayName") // Имя поля в облачной БД Firestore
  val name: String? = null,

  @SerialName("phone")
  val phone: String? = null,

  @SerialName("photoUrl")
  val photoUrl: String? = null,

  @SerialName("userRole")
  val userRole: String = "StandardUser",

  @SerialName("osbbId")
  val osbbId: Long = 0L,

  @SerialName("addressId")
  val addressId: Long = 0L,

  @SerialName("fio")
  val fio: String = "",

  @SerialName("osbb")
  val osbb: String? = null,

  @SerialName("fcmTokens")
  val fcmTokens: List<String>? = emptyList()
)

/**
 * [UserFirebase.toEntity] — Кроссплатформенный маппер из модели Firebase в Entity-модель для UI слоя.
 * ИСПРАВЛЕНО: Убран ложный кастинг .toInt(). Идентификаторы пробрасываются как чистые Long.
 * Префикс логирования переведен на стандарт YkisLogKMP.
 */
fun UserFirebase.toEntity(): UserEntity {
  println("[YkisLogKMP.$className.toEntity]: Выполняется КМР-маппинг профиля Firestore для UID: $uid")

  return UserEntity(
    uid = this.uid,
    // Используем name ("Адрес | Фамилия"), а если он null — email
    displayName = this.name ?: this.email,
    photoUrl = this.photoUrl,
    // Безопасно парсим строку роли в Enum класс через нашу зафиксированную функцию fromString
    userRole = UserRole.fromString(this.userRole),
    email = this.email,
    address = this.name ?: "",
    fio = this.fio, // Пробрасываем новое поле ФИО
    // ИСПРАВЛЕНО: Никаких .toInt(). Пробрасываем чистые Long идентификаторы напрямую в UI Entity структуру
    osbbId = this.osbbId,
    addressId = this.addressId,
    osbb = this.osbb ?: "", // Пробрасываем новое поле название ОСББ
    tokens = this.fcmTokens ?: emptyList()
  )
}
