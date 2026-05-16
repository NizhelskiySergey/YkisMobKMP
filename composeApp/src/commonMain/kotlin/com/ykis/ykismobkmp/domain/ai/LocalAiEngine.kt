package com.ykis.ykismobkmp.domain.ai

/**
 * [LocalAiEngine] — Ожидаемый кроссплатформенный класс локального ИИ-движка ЮКИС.
 * Его физическая реализация пишется отдельно для каждой операционной системы.
 */
expect class LocalAiEngine() {
  suspend fun generate(prompt: String): String?
}
