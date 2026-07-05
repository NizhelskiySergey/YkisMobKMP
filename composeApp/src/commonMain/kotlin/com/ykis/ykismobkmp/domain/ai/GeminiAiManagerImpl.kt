package com.ykis.ykismobkmp.domain.ai

private const val tag = "GeminiCloudProvider"

/**
 * [GeminiCloudProvider] — Реалізація ІІ-диспетчера ЮКІС через Firebase AI Logic.
 * Гібридна архітектура: Офіційний SDK на Android/iOS, REST на Web/Desktop.
 */
class GeminiCloudProvider(
  private val localEngine: LocalAiEngine,
  private val cloudEngine: CloudAiEngine
) : GeminiAiManager {

  override suspend fun processPrompt(prompt: String): Result<String> {
    // 1. Гібридний режим: спочатку пробуємо локальний Gemini Nano (AICore)
    val localResponse = localEngine.generate(prompt)
    if (localResponse != null) {
        println("[$tag]: Використано локальний Gemini Nano")
        return Result.success(localResponse)
    }
    
    // 2. Фолбек на хмару через CloudAiEngine (Firebase SDK або REST)
    return askAssistant(prompt)
  }

  override suspend fun askAssistant(prompt: String): Result<String> {
    return try {
      println("[$tag]: >>> [CLOUD_REQUEST_START]")
      val result = cloudEngine.generate(prompt)
      if (result != null) Result.success(result) 
      else Result.failure(Exception("Порожня відповідь"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray?): String? {
    if (imageData == null) return null
    return try {
      println("[$tag]: >>> [CLOUD_VISION_START] Розмір: ${imageData.size} байт")
      cloudEngine.analyze(prompt, imageData)
    } catch (e: Exception) {
      println("[$tag]: [CRITICAL_ERROR] ${e.message}")
      null
    }
  }
}
