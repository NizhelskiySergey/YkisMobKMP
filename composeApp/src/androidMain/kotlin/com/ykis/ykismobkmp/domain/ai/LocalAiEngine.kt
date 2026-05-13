package com.ykis.ykismobkmp.domain.ai

actual class LocalAiEngine {
  // Here we connect Google AI Edge SDK
  actual suspend fun generate(prompt: String): String? {
    // Call Gemini Nano via AICore
    return "Response from Gemini Nano"
  }
}
