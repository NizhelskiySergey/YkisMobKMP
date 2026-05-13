package com.ykis.ykismobkmp.domain.ai

actual class LocalAiEngine {
  // Here we connect Core ML / Apple MLX
  actual suspend fun generate(prompt: String): String? {
    // Call the native Apple model
    return "Response from Apple Core ML"
  }
}
