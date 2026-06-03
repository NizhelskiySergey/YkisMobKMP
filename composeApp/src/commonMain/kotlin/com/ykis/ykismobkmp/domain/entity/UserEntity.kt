package com.ykis.ykismobkmp.domain.entity

import com.ykis.ykismobkmp.domain.services.UserRole

private const val tag = "UserEntity"

/**
 * [UserEntity] — Монолитная КМР-модель пользователя/абонента ЮКИС г. Южный.
 * ИСПРАВЛЕНО: Согласно сквозному стандарту YkisMobKMP, osbbId и addressId переведены с Int на Long.
 */
data class UserEntity(
  val uid: String = "",
  val userRole: UserRole = UserRole.StandardUser,
  val photoUrl: String? = "",
  val createdAt: Long? = null, // Заменено с Timestamp на Long для кроссплатформенности KMP
  val displayName: String? = "",
  val email: String? = "",
  val address: String = "",
  val nanim: String = "",
  val osbbId: Long? = null,   // ИСПРАВЛЕНО: Приведено к типу Long под каноны SQLDelight
  val addressId: Long = 0L,   // ИСПРАВЛЕНО: Приведено к типу Long под каноны SQLDelight
  val tokens: List<String> = emptyList()
)

/**
 * [mapToUserEntity] — Безопасный КМР-маппер сырых Map-данных из Firestore / Ktor API.
 * ИСПРАВЛЕНО: Защищен от различий парсинга чисел чисел на Mac Desktop (Double) и Android (Long).
 */
fun mapToUserEntity(uid: String, map: Map<String, Any?>): UserEntity {
  val methodName = "mapToUserEntity"

  return try {
    UserEntity(
      uid = uid,
      userRole = UserRole.entries.find { it.name == map["userRole"] as? String }
        ?: UserRole.StandardUser,
      photoUrl = map["photoUrl"] as? String,
      // Безопасное получение временной метки создания аккаунта
      createdAt = map["createdAt"]?.let { (it as? Long) ?: (it as? Double)?.toLong() },
      displayName = (map["name"] as? String)
        ?: (map["displayName"] as? String)
        ?: (map["email"] as? String),
      email = map["email"] as? String,
      // ИСПРАВЛЕНО: Извлечение ИД переведено на безопасный Long-парсер
      osbbId = map["osbbId"]?.toSafeLong(),
      addressId = map["addressId"]?.toSafeLong() ?: 0L,
      address = (map["address"] as? String) ?: (map["name"] as? String) ?: "",
      // ИСПРАВЛЕНО: Проверяем оба варианта ключей для обратной совместимости
      tokens = (map["fcmTokens"] as? List<*>)?.filterIsInstance<String>()
        ?: (map["tokens"] as? List<*>)?.filterIsInstance<String>()
        ?: emptyList()
    )
  } catch (e: Exception) {
    // ИСПРАВЛЕНО: Нативный Android Log.e заменен на кроссплатформенный метод println()
    println("[$tag.$methodName]: Error mapping user $uid -> ${e.message}")
    UserEntity(uid = uid)
  }
}

/**
 * Хелпер для защиты от платформенных различий в типах чисел (KMP).
 * ИСПРАВЛЕНО: Возвращает Long вместо Int во избежание Race Condition в СУБД.
 */
private fun Any.toSafeLong(): Long? {
  return when (this) {
    is Number -> this.toLong()
    is String -> this.toLongOrNull()
    else -> null
  }
}
