package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.FastpayEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [GetFastpayTokensResponse] — Кроссплатформенная модель ответа со списком токенов оплаты FastPay.
 */
@Serializable
data class GetFastpayTokensResponse(
    @SerialName("success")
    val success: Int = 0,
    
    @SerialName("message")
    val message: String = "",
    
    @SerialName("tokens")
    val tokens: List<FastpayEntity> = emptyList()
)
