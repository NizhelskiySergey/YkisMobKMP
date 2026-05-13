package com.ykis.ykismobkmp.domain.ai

// [commonMain] domain/ai/GeminiAiManagerImpl.kt
interface GeminiAiManager {
  suspend fun askAssistant(prompt: String): Result<String>
  suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray): String?
}
