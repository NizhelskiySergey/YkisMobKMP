package com.ykis.ykismobkmp.domain.ai

import com.ykis.ykismobkmp.core.utils.NativeAuthBridge
import com.ykis.ykismobkmp.core.utils.getNativeBridge
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.base64EncodedStringWithOptions
import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * iOS-реалізація через нативний Swift міст (Firebase AI Logic).
 */
actual class CloudAiEngine actual constructor(apiKey: String) {
    
    private val bridge: NativeAuthBridge? = getNativeBridge()

    actual suspend fun generate(prompt: String): String? = suspendCoroutine { continuation ->
        bridge?.generateAiContent(prompt) { result, error ->
            continuation.resume(result)
        } ?: continuation.resume(null)
    }

    actual suspend fun analyze(prompt: String, image: ByteArray): String? = suspendCoroutine { continuation ->
        val base64 = image.toNSData().base64EncodedStringWithOptions(0UL)
        bridge?.analyzeAiImage(prompt, base64) { result, error ->
            continuation.resume(result)
        } ?: continuation.resume(null)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData = usePinned { 
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}
