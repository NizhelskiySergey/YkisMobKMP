package com.ykis.ykismobkmp.core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * [SmartLongSerializer] — Дешифратор довгих чисел.
 * ФІКС: На Вебі серіалізує Long як Double (Number в JS), щоб уникнути RangeError у Firestore.
 */
object SmartLongSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("SmartLong", PrimitiveKind.DOUBLE) // Double для сумісності з JS
    
    override fun serialize(encoder: Encoder, value: Long) {
        val platform = com.ykis.ykismobkmp.getPlatform().name
        if (platform.contains("Web", true) || platform.contains("JS", true)) {
            encoder.encodeDouble(value.toDouble())
        } else {
            encoder.encodeLong(value)
        }
    }

    override fun deserialize(decoder: Decoder): Long {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return 0L
        return when (element) {
            is JsonPrimitive -> element.content.toLongOrNull() ?: element.content.toDoubleOrNull()?.toLong() ?: 0L
            else -> 0L
        }
    }
}

/**
 * [SmartDoubleSerializer] — Дешифратор дробних чисел.
 */
object SmartDoubleSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("SmartDouble", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return 0.0
        return when (element) {
            is JsonPrimitive -> element.content.toDoubleOrNull() ?: 0.0
            else -> 0.0
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
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return 0
        return when (element) {
            is JsonPrimitive -> element.content.toIntOrNull() ?: 0
            else -> 0
        }
    }
}
