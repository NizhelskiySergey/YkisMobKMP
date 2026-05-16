package com.ykis.ykismobkmp.domain.ai

/**
 * [GeminiAiManager] — Центральный кроссплатформенный контракт ИИ-ассистента ЮКИС.
 */
interface GeminiAiManager {

  /**
   * [processPrompt] — ГИБРИДНЫЙ МЕТОД: Автоматически пытается обработать запрос локально на устройстве (Gemini Nano),
   * а если это невозможно (Mac Desktop/iOS/старый Android) — бесшовно перенаправляет его в облако Google.
   */
  suspend fun processPrompt(prompt: String): Result<String>

  /**
   * [askAssistant] — Прямой принудительный текстовый запрос в облако Gemini.
   */
  suspend fun askAssistant(prompt: String): Result<String>

  /**
   * [analyzeMeterImage] — Мультимодальный анализ фотографии счетчика (Облако).
   */
  suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray?): String?
}
