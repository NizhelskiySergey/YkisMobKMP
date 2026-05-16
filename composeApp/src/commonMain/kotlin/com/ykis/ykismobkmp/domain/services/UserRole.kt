package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.domain.services.UserRole.Companion.fromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [UserRole] — Кроссплатформенное перечисление ролей пользователей системы ЮКИС.
 * Управляет логикой разделения прав доступа между жильцами Южного и администрацией ЖКХ.
 */
@Serializable
enum class UserRole {
  @SerialName("UNKNOWN")
  Unknown,

  @SerialName("STANDARD_USER")
  StandardUser,

  @SerialName("WATER_SERVICE")
  VodokanalUser,

  @SerialName("WARM_SERVICE")
  YtkeUser,

  @SerialName("GARBAGE_SERVICE")
  TboUser,

  @SerialName("OSBB")
  OsbbUser;

  companion object {
    /**
     * [fromString] — Безопасный кроссплатформенный парсинг строкового значения роли из Firestore.
     * Гарантирует возврат StandardUser в случае непредвиденных сбоев структуры данных.
     */
    fun fromString(roleStr: String?): UserRole {
      if (roleStr.isNullOrBlank()) return StandardUser

      // ИСПРАВЛЕНО: Прямое КМР-сравнение строк со всеми возможными форматами ответов бэкенда и Firestore
      return entries.find {
        it.name.equals(roleStr, ignoreCase = true) ||
          it.getSerialName().equals(roleStr, ignoreCase = true)
      } ?: StandardUser
    }

    // Хелпер для извлечения строкового значения SerialName в KMP рантайме
    private fun UserRole.getSerialName(): String {
      return when (this) {
        Unknown -> "UNKNOWN"
        StandardUser -> "STANDARD_USER"
        VodokanalUser -> "WATER_SERVICE"
        YtkeUser -> "WARM_SERVICE"
        TboUser -> "GARBAGE_SERVICE"
        OsbbUser -> "OSBB"
      }
    }
  }
}
