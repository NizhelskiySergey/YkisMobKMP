package com.ykis.ykismobkmp.domain.ai

/**
 * Кроссплатформенна обгортка для Firebase AI Logic (стандарт 2026).
 */
expect class CloudAiEngine(apiKey: String) {
    /**
     * Генерує текст на основі промпту.
     */
    suspend fun generate(prompt: String): String?

    /**
     * Аналізує зображення (лічильник) з промптом.
     */
    suspend fun analyze(prompt: String, image: ByteArray): String?
}
