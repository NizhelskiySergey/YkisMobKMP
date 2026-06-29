package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.FastpayEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * [GetFastpayTokensResponse] — Кроссплатформенная модель ответа с токенами.
 * УНИФИЦИРОВАНО: Добавлена защита от некорректного типа поля fastpayTokens (когда сервер шлет строку вместо массива).
 */
@Serializable
data class GetFastpayTokensResponse(
    @SerialName("success")
    val success: Int = 0,
    
    @SerialName("message")
    val message: String = "",
    
    @SerialName("fastpayTokens")
    private val _tokens: JsonElement? = null
) {
    val tokens: List<FastpayEntity>
        get() = try {
            val jsonParser = Json { 
                ignoreUnknownKeys = true 
                isLenient = true
                coerceInputValues = true
            }
            if (_tokens != null && _tokens is JsonArray) {
                println("[YkisLogKMP.FastPay_RAW]: ${_tokens.toString()}")
                jsonParser.decodeFromJsonElement<List<FastpayEntity>>(_tokens)
            } else {
                println("[YkisLogKMP.FastPay_RAW]: Поле fastpayTokens не є масивом або порожнє: $_tokens")
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
}
