package com.ykis.ykismobkmp.domain.ai


actual class LocalAiEngine actual constructor() {
  actual suspend fun generate(prompt: String): String? {
    // На Mac Desktop и iOS Nano нет -> всегда возвращаем null для мгновенного отката в облако
    return null
  }
}
