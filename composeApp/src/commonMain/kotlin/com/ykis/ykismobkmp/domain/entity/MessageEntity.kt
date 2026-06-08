package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ChatMetadata(
  @SerialName("residentActive") val residentActive: Boolean = true
)

@Serializable
data class MessageEntity(
  val id: String = "",
  val senderUid: String = "",
  val senderDisplayedName: String = "",
  val senderLogoUrl: String? = null,
  val senderAddress: String = "",
  val text: String = "",
  val type: String = "TEXT",
  val imageUrl: String? = null,
  val fileUrl: String? = null,
  val fileName: String? = null,
  val timestamp: Long = 0L,
  val read: Boolean = false,
  val edited: Boolean = false,
  val deletedFor: List<String> = emptyList(),
  @SerialName("forwarded") val isForwarded: Boolean = false,
  @SerialName("fromAdmin") val fromAdmin: Boolean = false
)
