package com.ykis.ykismobkmp.domain.ai

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content

private const val tag = "GeminiCloudProvider"

/**
 * [GeminiCloudProvider] — Реализация ИИ-диспетчера ЮКИС.
 * Совмещает кроссплатформенное облако и платформозависимый локальный движок expect/actual.
 */
class GeminiCloudProvider(
  private val model: GenerativeModel,
  private val localEngine: LocalAiEngine // Инжектируем expect-класс локального ИИ
) : GeminiAiManager {

  /**
   * ПОШАГОВОЕ ВЫПОЛНЕНИЕ ДИСПЕТЧЕРИЗАЦИИ ЗАПРОСА ( processPrompt ):
   */
  override suspend fun processPrompt(prompt: String): Result<String> {
    println("[$tag.processPrompt]: [ШАГ 1] Получен запрос от UI: '$prompt'")

    // ШАГ 2: Опрашиваем локальный движок LocalAiEngine (expect/actual)
    println("[$tag.processPrompt]: [ШАГ 2] Попытка запустить локальную генерацию на чипе...")
    val localResponse = localEngine.generate(prompt)

    // ШАГ 3: Если локальный движок вернул результат (Nano сработал на Android) — отдаем его
    if (localResponse != null) {
      println("[$tag.processPrompt]: [УСПЕХ] Запрос обработан локально через Gemini Nano (AICore)!")
      return Result.success(localResponse)
    }

    // ШАГ 4: ФОЛБЭК (Откат). Если мы на Mac Desktop или старом Android — localResponse равен null.
    // Перенаправляем запрос в облако Google.
    println("[$tag.processPrompt]: [ОТКАТ] Локальный чип недоступен. Перенаправление в облако Gemini...")
    return askAssistant(prompt)
  }

  override suspend fun askAssistant(prompt: String): Result<String> {
    return try {
      val response = model.generateContent(prompt)
      val textResult = response.text
      if (textResult != null) {
        Result.success(textResult)
      } else {
        Result.failure(Exception("Порожня відповідь нейромережі"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray?): String? {
    return try {
      val response = if (imageData != null) {
        model.generateContent(
          content {
            image(imageData)
            text(prompt)
          }
        )
      } else {
        model.generateContent(prompt)
      }
      val text = response.text
      if (text == null) {
          println("[$tag]: [WARN] Gemini повернув порожній текст. Можливо, контент заблоковано фільтрами безпеки.")
      }
      text
    } catch (e: Exception) {
      println("[$tag]: [ERROR] Помилка мультимодального аналізу: ${e.message}")
      e.printStackTrace()
      null
    }
  }
}
