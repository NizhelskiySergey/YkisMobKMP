package com.ykis.ykismobkmp.domain.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import android.graphics.BitmapFactory

/**
 * Android-реалізація через офіційний Firebase AI Logic SDK.
 * ПІДТРИМКА: App Check Limited-use Tokens.
 */
actual class CloudAiEngine actual constructor(apiKey: String) {
    
    private val ai = Firebase.ai(
        backend = GenerativeBackend.googleAI(), 
        useLimitedUseAppCheckTokens = true
    )
    
    private val model = ai.generativeModel("gemini-3.5-flash")

    actual suspend fun generate(prompt: String): String? {
        return try {
            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            println("[CloudAiEngine.generate]: SDK Error - ${e.message}")
            null
        }
    }

    actual suspend fun analyze(prompt: String, image: ByteArray): String? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            val result = response.text
            if (result == null) {
                println("[CloudAiEngine.analyze]: AI returned NULL. Block reason: ${response.promptFeedback?.blockReason}")
            }
            result
        } catch (e: Exception) {
            println("[CloudAiEngine.analyze]: SDK Critical Error - ${e.message}")
            null
        }
    }
}
