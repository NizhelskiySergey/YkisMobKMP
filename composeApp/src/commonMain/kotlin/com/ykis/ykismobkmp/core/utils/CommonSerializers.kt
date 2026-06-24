package com.ykis.ykismobkmp.core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * [SmartLongSerializer] — Універсальний дешифратор довгих чисел.
 * Вміє працювати як з JSON, так і з внутрішніми форматами Firestore.
 */
object SmartLongSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("SmartLong", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: Long) {
        val platform = com.ykis.ykismobkmp.getPlatform().name
        if (platform.contains("Web", true) || platform.contains("JS", true)) {
            encoder.encodeDouble(value.toDouble())
        } else {
            encoder.encodeLong(value)
        }
    }

    override fun deserialize(decoder: Decoder): Long {
        // Спроба 1: Читання через Json (якщо це мережевий запит)
        val jsonElement = (decoder as? JsonDecoder)?.decodeJsonElement()
        if (jsonElement is JsonPrimitive) {
            return jsonElement.content.toLongOrNull() ?: jsonElement.content.toDoubleOrNull()?.toLong() ?: 0L
        }
        
        // Спроба 2: Пряме читання (якщо це Firestore або інший формат)
        return try {
            decoder.decodeLong()
        } catch (e: Exception) {
            try {
                decoder.decodeDouble().toLong()
            } catch (e2: Exception) {
                0L
            }
        }
    }
}

/**
 * [SmartDoubleSerializer] — Універсальний дешифратор дробних чисел.
 */
object SmartDoubleSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("SmartDouble", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double {
        val jsonElement = (decoder as? JsonDecoder)?.decodeJsonElement()
        if (jsonElement is JsonPrimitive) {
            return jsonElement.content.toDoubleOrNull() ?: 0.0
        }
        return try {
            decoder.decodeDouble()
        } catch (e: Exception) {
            try {
                decoder.decodeString().toDoubleOrNull() ?: 0.0
            } catch (e2: Exception) {
                0.0
            }
        }
    }
}

/**
 * [SmartIntSerializer] — Дешифратор цілих чисел.
 */
object SmartIntSerializer : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("SmartInt", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
    override fun deserialize(decoder: Decoder): Int {
        val jsonElement = (decoder as? JsonDecoder)?.decodeJsonElement()
        if (jsonElement is JsonPrimitive) {
            return jsonElement.content.toIntOrNull() ?: 0
        }
        return try {
            decoder.decodeInt()
        } catch (e: Exception) {
            0
        }
    }
}
