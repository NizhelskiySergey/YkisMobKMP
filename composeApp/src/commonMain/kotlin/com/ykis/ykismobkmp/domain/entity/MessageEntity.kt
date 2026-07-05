package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMetadata(
  @SerialName("residentActive") val residentActive: Boolean = true
)

/**
 * [MessageEntity] — Кроссплатформенная модель сообщения.
 * ИСПРАВЛЕНО: Добавлены явные @SerialName для всех полей для 100% совместимости Web/Android.
 */
@Serializable
data class MessageEntity(
  @SerialName("id") val id: String = "",
  @SerialName("senderUid") val senderUid: String = "",
  @SerialName("senderDisplayedName") val senderDisplayedName: String = "",
  @SerialName("senderLogoUrl") val senderLogoUrl: String? = null,
  @SerialName("senderAddress") val senderAddress: String = "",
  @SerialName("text") val text: String = "",
  @SerialName("type") val type: String = "TEXT",
  @SerialName("imageUrl") val imageUrl: String? = null,
  @SerialName("fileUrl") val fileUrl: String? = null,
  @SerialName("fileName") val fileName: String? = null,
  @SerialName("timestamp") val timestamp: Long = 0L,
  @SerialName("read") val read: Boolean = false,
  @SerialName("edited") val edited: Boolean = false,
  @SerialName("deletedFor") val deletedFor: List<String> = emptyList(),
  @SerialName("forwarded") val isForwarded: Boolean = false,
  @SerialName("fromAdmin") val fromAdmin: Boolean = false,
  @SerialName("imageWidth") val imageWidth: Int = 0,
  @SerialName("imageHeight") val imageHeight: Int = 0
)
