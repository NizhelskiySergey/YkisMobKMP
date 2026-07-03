package com.ykis.ykismobkmp.domain.ai

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import com.ykis.ykismobkmp.di.GEMINI_API_KEY
import com.ykis.ykismobkmp.core.utils.Resource
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel

private const val tag = "GeminiCloudProvider"

/**
 * [GeminiCloudProvider] — Реалізація ІІ-диспетчера ЮКІС.
 * ПІДТРИМКА КЛЮЧІВ AQ ТА МОДЕЛІ 3.5 FLASH ЧЕРЕЗ ПРЯМИЙ REST (КМР стандарт).
 */
class GeminiCloudProvider(
  private val model: GenerativeModel, // Залишаємо для сумісності з Koin
  private val localEngine: LocalAiEngine,
  private val httpClient: HttpClient // Використовуємо Ktor для прямого доступу
) : GeminiAiManager {

  private val modelId = "gemini-3.5-flash"
  private val baseUrl = "https://generativelanguage.googleapis.com/v1/models/$modelId:generateContent"

  override suspend fun processPrompt(prompt: String): Result<String> {
    val localResponse = localEngine.generate(prompt)
    if (localResponse != null) return Result.success(localResponse)
    return askAssistant(prompt)
  }

  override suspend fun askAssistant(prompt: String): Result<String> {
    return try {
      println("[$tag]: >>> [REST_REQUEST_START] Модель: $modelId")
      
      val response = httpClient.post(baseUrl) {
        header("x-goog-api-key", GEMINI_API_KEY)
        contentType(ContentType.Application.Json)
        setBody(buildJsonObject {
          putJsonArray("contents") {
            addJsonObject {
              putJsonArray("parts") {
                addJsonObject { put("text", prompt) }
              }
            }
          }
        })
      }

      if (response.status.isSuccess()) {
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val text = body["candidates"]?.jsonArray?.get(0)?.jsonObject
          ?.get("content")?.jsonObject
          ?.get("parts")?.jsonArray?.get(0)?.jsonObject
          ?.get("text")?.jsonPrimitive?.content
        
        if (text != null) Result.success(text) 
        else Result.failure(Exception("Порожня відповідь"))
      } else {
        println("[$tag]: [REST_ERROR] Код: ${response.status}. Текст: ${response.bodyAsText()}")
        Result.failure(Exception("Помилка API: ${response.status}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray?): String? {
    if (imageData == null) return null
    return try {
      println("[$tag]: >>> [REST_VISION_START] Розмір: ${imageData.size} байт")
      
      val response = httpClient.post(baseUrl) {
        header("x-goog-api-key", GEMINI_API_KEY)
        contentType(ContentType.Application.Json)
        setBody(buildJsonObject {
          putJsonArray("contents") {
            addJsonObject {
              putJsonArray("parts") {
                addJsonObject { put("text", prompt) }
                addJsonObject {
                  putJsonObject("inline_data") {
                    put("mime_type", "image/jpeg")
                    put("data", com.ykis.ykismobkmp.core.utils.encodeBase64(imageData))
                  }
                }
              }
            }
          }
        })
      }

      if (response.status.isSuccess()) {
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["candidates"]?.jsonArray?.get(0)?.jsonObject
          ?.get("content")?.jsonObject
          ?.get("parts")?.jsonArray?.get(0)?.jsonObject
          ?.get("text")?.jsonPrimitive?.content
      } else {
        println("[$tag]: [REST_VISION_ERROR] ${response.bodyAsText()}")
        null
      }
    } catch (e: Exception) {
      println("[$tag]: [CRITICAL_ERROR] ${e.message}")
      null
    }
  }
}
