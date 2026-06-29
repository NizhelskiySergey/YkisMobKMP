package com.ykis.ykismobkmp.domain.entity

import com.ykis.ykismobkmp.core.utils.SmartLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [FastpayEntity] — Модель токена швидкої оплати Privat24 для організацій.
 */
@Serializable
data class FastpayEntity(
    @Serializable(with = SmartLongSerializer::class)
    @SerialName("id")
    val id: Long = 0L,

    @SerialName("name")
    val name: String = "",

    @Serializable(with = SmartLongSerializer::class)
    @SerialName("biplan_id")
    val biplanId: Long = 0L,

    @Serializable(with = SmartLongSerializer::class)
    @SerialName("okpo")
    val okpo: Long = 0L,

    @Serializable(with = SmartLongSerializer::class)
    @SerialName("osbbId")
    val osbbId: Long = 0L,

    @SerialName("full_url")
    val fullUrl: String = "",

    @SerialName("token")
    val token: String = ""
)
