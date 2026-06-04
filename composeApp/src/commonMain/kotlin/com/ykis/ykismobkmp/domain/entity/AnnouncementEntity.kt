package com.ykis.ykismobkmp.domain.entity

import com.ykis.ykismobkmp.domain.services.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [AnnouncementEntity] — Модель объявления/новости от коммунальных служб или ОСББ.
 */
@Serializable
data class AnnouncementEntity(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("author_uid") val authorUid: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("author_role") val authorRole: UserRole = UserRole.Unknown,
    @SerialName("osbb_id") val osbbId: Long = 0L, // 0 - для всех, либо конкретный ID ОСББ
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("is_priority") val isPriority: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null
)
