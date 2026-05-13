package com.ykis.ykismobkmp.domain.ai

// [commonMain or platforms]
class GeminiCloudProvider(
  private val model: dev.shreyaspatil.ai.client.generativeai.GenerativeModel
) : GeminiAiManager {
  override suspend fun askCloud(prompt: String, imageData: ByteArray?): String? {
    return try {
      // If there is a photo, create content with the image
      val response = model.generateContent(prompt) // The logic is here with ByteArray
      response.text
    } catch (e: Exception) {
      null
    }
  }

  override suspend fun askLocal(prompt: String): String? {
    // There is no Nano on Mac, so the "local" request also goes to the cloud on Desktop
    return askCloud(prompt)
  }
}

expect class LocalAiEngine() {
  suspend fun generate(prompt: String): String?
}
