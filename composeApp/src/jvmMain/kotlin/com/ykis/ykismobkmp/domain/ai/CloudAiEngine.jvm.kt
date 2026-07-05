package com.ykis.ykismobkmp.domain.ai

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * JVM-реалізація через прямий REST API Vertex AI.
 */
actual class CloudAiEngine actual constructor(private val apiKey: String) : KoinComponent {
    
    private val httpClient: HttpClient by inject()
    private val modelId = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1/models/$modelId:generateContent"

    actual suspend fun generate(prompt: String): String? {
        return try {
            val response = httpClient.post(baseUrl) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    putJsonArray("contents") {
                        addJsonObject {
                            putJsonArray("parts") { addJsonObject { put("text", prompt) } }
                        }
                    }
                })
            }
            parseText(response.bodyAsText())
        } catch (e: Exception) { null }
    }

    actual suspend fun analyze(prompt: String, image: ByteArray): String? {
        return try {
            val response = httpClient.post(baseUrl) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    putJsonArray("contents") {
                        addJsonObject {
                            putJsonArray("parts") {
                                addJsonObject { put("text", prompt) }
                                addJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", "image/jpeg")
                                        put("data", java.util.Base64.getEncoder().encodeToString(image))
                                    }
                                }
                            }
                        }
                    }
                })
            }
            parseText(response.bodyAsText())
        } catch (e: Exception) { null }
    }

    private fun parseText(json: String): String? {
        return try {
            Json.parseToJsonElement(json).jsonObject["candidates"]
                ?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
        } catch (e: Exception) { null }
    }
}
