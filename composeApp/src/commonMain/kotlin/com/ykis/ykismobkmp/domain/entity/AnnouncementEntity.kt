package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [AnnouncementEntity] — Модель объявления/новости от коммунальных служб или ОСББ.
 * ИСПРАВЛЕНО: authorRole теперь String для предотвращения падений десериализации в Web.
 */
@Serializable
data class AnnouncementEntity(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("authorUid") val authorUid: String = "",
    @SerialName("authorName") val authorName: String = "",
    @SerialName("authorRole") val authorRole: String = "Unknown",
    @SerialName("osbbId") val osbbId: Long = 0L,
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("isPriority") val isPriority: Boolean = false,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("fileUrl") val fileUrl: String? = null,
    @SerialName("fileName") val fileName: String? = null
)
