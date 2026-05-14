package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.domain.services.UserRole.Companion.fromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [UserRole] — Кроссплатформенное перечисление ролей пользователей системы ЮКИС.
 * Управляет логикой разделения прав доступа между жильцами Южного и администрацией ЖКХ.
 */
@Serializable
enum class UserRole(val codeName: ContentDetail) {
  @SerialName("UNKNOWN")
  Unknown(ContentDetail.UNKNOWN),

  @SerialName("STANDARD_USER")
  StandardUser(ContentDetail.STANDARD_USER),

  @SerialName("WATER_SERVICE")
  VodokanalUser(ContentDetail.WATER_SERVICE),

  @SerialName("WARM_SERVICE")
  YtkeUser(ContentDetail.WARM_SERVICE),

  @SerialName("GARBAGE_SERVICE")
  TboUser(ContentDetail.GARBAGE_SERVICE),

  // ИСПРАВЛЕНО: "OSBB" из Firestore/JSON превратится в OsbbUser (Администратор ОСМД/Дома на Mac)
  @SerialName("OSBB")
  OsbbUser(ContentDetail.OSBB);

  companion object {
    /**
     * [fromString] — Безопасный кроссплатформенный парсинг строкового значения роли из Firestore.
     * Гарантирует возврат StandardUser в случае непредвиденных сбоев структуры данных.
     */
    fun fromString(roleStr: String?): UserRole {
      if (roleStr.isNullOrBlank()) return StandardUser

      // ИСПРАВЛЕНО: используем КМР-совместимое сравнение строк без привязки к Java/Android SDK
      return entries.find { it.name.equals(roleStr, ignoreCase = true) }
        ?: entries.find { it.codeName.name.equals(roleStr, ignoreCase = true) }
        ?: StandardUser
    }
  }
}

/**
 * [ContentDetail] — Системные идентификаторы служб ЖКХ.
 */
@Serializable
enum class ContentDetail {
  UNKNOWN,
  STANDARD_USER,
  WATER_SERVICE,
  WARM_SERVICE,
  GARBAGE_SERVICE,
  OSBB
}

