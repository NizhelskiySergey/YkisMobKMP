package com.ykis.ykismobkmp.domain.services // Укажи свой актуальный пакет сущностей

import com.ykis.ykismobkmp.domain.entity.UserEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
/**
 * [UserFirebase] — Кроссплатформенная модель профиля пользователя в Firestore.
 * Очищена от платформенных привязок и синхронизирована с типами Long для баз данных.
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

  @SerialName("displayName") // Имя поля в Firestore
  val name: String? = null,

  @SerialName("phone")
  val phone: String? = null,

  @SerialName("photoUrl")
  val photoUrl: String? = null,

  @SerialName("userRole")
  val userRole: String = "StandardUser",

  // ИСПРАВЛЕНО: ID организации (ОСББ) переведен на Long под типы SQLDelight
  @SerialName("osbbId")
  val osbbId: Long = 0L,

  // ИСПРАВЛЕНО: ID конкретной квартиры переведен на Long под типы SQLDelight
  @SerialName("addressId")
  val addressId: Long = 0L,

  @SerialName("fcmTokens")
  val fcmTokens: List<String>? = emptyList()
)

/**
 * [UserFirebase.toEntity] — Кроссплатформенный маппер из модели Firebase в Entity-модель для UI слоя.
 * Убирает необходимость ручного кастинга числовых идентификаторов в ScreenModels.
 */
fun UserFirebase.toEntity(): UserEntity {
  return UserEntity(
    uid = this.uid,
    // Используем name ("Адрес | Фамилия"), а если он null — email
    displayName = this.name ?: this.email,
    photoUrl = this.photoUrl,
    // Безопасно парсим строку роли в Enum класс
    userRole = UserRole.entries.find { it.name == this.userRole } ?: UserRole.StandardUser,
    email = this.email,
    address = this.name ?: "",
    // Пробрасываем чистые Long идентификаторы напрямую в UI Entity структуру
    osbbId = this.osbbId,
    addressId = this.addressId,
    tokens = this.fcmTokens ?: emptyList()
  )
}
