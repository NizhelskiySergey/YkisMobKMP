package com.ykis.ykismobkmp.data.remote


import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GeminiAiManagerImpl(
  private val client: HttpClient,
  private val apiKey: String
) : GeminiAiManager {
  private val className = "GeminiAiManagerImpl"
  private val geminiUrl = "https://googleapis.com"

  override suspend fun askAssistant(prompt: String): Result<String> {
    return try {
      val response = client.post("$geminiUrl?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(buildPayload(prompt, null))
      }
      Result.success(parseResponse(response.bodyAsText()))
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.askAssistant]: ${e.message}")
      Result.failure(e)
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  override suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray): String? {
    return try {
      val base64Image = Base64.encode(imageData)
      val response = client.post("$geminiUrl?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(buildPayload(prompt, base64Image))
      }
      parseResponse(response.bodyAsText())
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.analyzeMeterImage]: ${e.message}")
      null
    }
  }

  private fun buildPayload(prompt: String, base64Image: String?) = buildJsonObject {
    putJsonArray("contents") {
      addJsonObject {
        putJsonArray("parts") {
          addJsonObject { put("text", prompt) }
          base64Image?.let {
            addJsonObject {
              putJsonObject("inline_data") {
                put("mime_type", "image/jpeg")
                put("data", it)
              }
            }
          }
        }
      }
    }
  }

  private fun parseResponse(json: String): String {
    return try {
      val element = Json.parseToJsonElement(json).jsonObject
      element["candidates"]?.jsonArray?.get(0)
        ?.jsonObject?.get("content")
        ?.jsonObject?.get("parts")
        ?.jsonArray?.get(0)
        ?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
    } catch (e: Exception) { "" }
  }
}
