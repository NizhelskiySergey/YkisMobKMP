package com.ykis.ykismobkmp.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [FastpayEntity] — Модель токена швидкої оплати Privat24 для організацій.
 */
@Serializable
data class FastpayEntity(
    @SerialName("id")
    val id: Int = 0,

    @SerialName("name")
    val name: String = "",

    @SerialName("biplan_id")
    val biplanId: Long = 0L,

    @SerialName("okpo")
    val okpo: Long = 0L,

    @SerialName("osbb_id")
    val osbbId: Long = 0L,

    @SerialName("full_url")
    val fullUrl: String = "",

    @SerialName("token")
    val token: String = ""
)
