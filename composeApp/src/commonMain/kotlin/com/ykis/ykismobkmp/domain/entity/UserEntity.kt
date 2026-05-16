package com.ykis.ykismobkmp.domain.entity

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.services.UserRole

fun mapToUserEntity(uid: String, map: Map<String, Any?>): UserEntity {
  val methodName = "mapToUserEntity"

  return try {
    UserEntity(
      uid = uid,
      userRole = UserRole.entries.find { it.name == map["userRole"] as? String }
        ?: UserRole.StandardUser,
      photoUrl = map["photoUrl"] as? String,
      // Безопасное получение даты
      createdAt = map["createdAt"]?.let { (it as? Long) ?: (it as? Double)?.toLong() },
      displayName = (map["name"] as? String)
        ?: (map["displayName"] as? String)
        ?: (map["email"] as? String),
      email = map["email"] as? String,
      // Безопасное получение ID (Double -> Long -> Int)
      osbbId = map["osbbId"]?.toSafeInt(),
      addressId = map["addressId"]?.toSafeInt() ?: 0,
      address = (map["address"] as? String) ?: (map["name"] as? String) ?: "",
      tokens = (map["fcmTokens"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    )
  } catch (e: Exception) {
    Log.e("YkisLog", "[$methodName]: Error mapping user $uid -> ${e.message}")
    UserEntity(uid = uid)
  }
}

/**
 * Хелпер для защиты от платформенных различий в типах чисел (KMP).
 */
private fun Any.toSafeInt(): Int? {
  return when (this) {
    is Number -> this.toInt()
    is String -> this.toIntOrNull()
    else -> null
  }
}

data class UserEntity(
  val uid: String = "",
  val userRole: UserRole = UserRole.StandardUser,
  val photoUrl: String? = "",
  val createdAt: Long? = null, // Заменили Timestamp на Long для KMP
  val displayName: String? = "",
  val email: String? = "",
  val address: String = "",
  val nanim: String = "",
  val osbbId: Int? = null,
  val addressId: Int = 0,
  val tokens: List<String> = emptyList()
)
