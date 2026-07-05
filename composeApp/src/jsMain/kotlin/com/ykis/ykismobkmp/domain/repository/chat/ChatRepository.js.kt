package com.ykis.ykismobkmp.domain.repository.chat

import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

/**
 * [platformReadFileAsBytes] — Web-реалізація отримання ОРИГІНАЛЬНИХ байтів файлу.
 * ВИПРАВЛЕНО: Використовуємо fetch для отримання чистого ArrayBuffer з Blob/Data URL.
 */
actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
    return try {
        val response = window.fetch(path).await()
        val buffer = response.arrayBuffer().await()
        val uint8Array = Uint8Array(buffer)
        ByteArray(uint8Array.length) { i -> uint8Array[i] }
    } catch (e: Exception) {
        println("[ChatRepository.Web]: Помилка читання файлу ($path): ${e.message}")
        byteArrayOf()
    }
}

/**
 * [platformCompressImage] — Стиснення зображення через Canvas у браузері.
 */
actual suspend fun platformCompressImage(path: String): ByteArray {
    if (!path.startsWith("data:image") && !path.startsWith("blob:")) return platformReadFileAsBytes(path)

    return suspendCoroutine { continuation ->
        val img = window.document.createElement("img") as HTMLImageElement
        img.crossOrigin = "anonymous"
        img.onload = {
            val canvas = window.document.createElement("canvas") as HTMLCanvasElement
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            
            var width = img.naturalWidth.toDouble()
            var height = img.naturalHeight.toDouble()
            val maxSide = 1200.0
            
            if (width > height && width > maxSide) {
                height *= maxSide / width
                width = maxSide
            } else if (height > maxSide) {
                width *= maxSide / height
                height = maxSide
            }
            
            canvas.width = width.toInt()
            canvas.height = height.toInt()
            ctx.drawImage(img, 0.0, 0.0, width, height)
            
            val compressedBase64 = canvas.toDataURL("image/jpeg", 0.7)
            val binaryString = window.atob(compressedBase64.substringAfter(","))
            val bytes = ByteArray(binaryString.length)
            for (i in binaryString.indices) {
                bytes[i] = (binaryString[i].code and 0xFF).toByte()
            }
            continuation.resume(bytes)
        }
        img.onerror = { _, _, _, _, _ ->
            continuation.resume(byteArrayOf())
        }
        img.src = path
    }
}
