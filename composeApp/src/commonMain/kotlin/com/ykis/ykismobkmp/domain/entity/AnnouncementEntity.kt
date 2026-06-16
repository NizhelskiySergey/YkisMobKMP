package com.ykis.ykismobkmp.domain.entity

import com.ykis.ykismobkmp.domain.services.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [AnnouncementEntity] — Модель объявления/новости от коммунальных служб или ОСББ.
 * ИСПРАВЛЕНО: Поля синхронизированы со структурой Firestore (camelCase).
 */
@Serializable
data class AnnouncementEntity(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("authorUid") val authorUid: String = "",
    @SerialName("authorName") val authorName: String = "",
    @SerialName("authorRole") val authorRole: UserRole = UserRole.Unknown,
    @SerialName("osbbId") val osbbId: Long = 0L,
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("isPriority") val isPriority: Boolean = false,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("fileUrl") val fileUrl: String? = null,
    @SerialName("fileName") val fileName: String? = null
)
