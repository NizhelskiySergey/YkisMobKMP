package com.ykis.ykismobkmp.domain.ai

import com.ykis.ykismobkmp.core.utils.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.koin.core.component.KoinComponent

/**
 * JS-реалізація через прямий REST (найбільш стабільно для Gemini 3.5 Flash).
 */
actual class CloudAiEngine actual constructor(private val apiKey: String) : KoinComponent {
    
    actual suspend fun generate(prompt: String): String? {
        return try {
            val promise = (window.asDynamic()).generateAiContentWeb(prompt, apiKey)
            promise.unsafeCast<kotlin.js.Promise<String>>().await()
        } catch (e: Exception) {
            println("[CloudAiEngine.js]: generate error - ${e.message}")
            null
        }
    }

    actual suspend fun analyze(prompt: String, image: ByteArray): String? {
        return try {
            val base64Data = encodeBase64(image)
            val promise = (window.asDynamic()).analyzeAiImageWeb(prompt, base64Data, apiKey)
            promise.unsafeCast<kotlin.js.Promise<String>>().await()
        } catch (e: Exception) {
            println("[CloudAiEngine.js]: analyze error - ${e.message}")
            null
        }
    }
}
