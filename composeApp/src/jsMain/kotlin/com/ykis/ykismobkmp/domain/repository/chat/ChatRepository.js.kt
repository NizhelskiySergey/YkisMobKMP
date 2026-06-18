package com.ykis.ykismobkmp.domain.repository.chat

import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * [platformReadFileAsBytes] — Web-реалізація перетворення Base64 у масив байтів.
 */
actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
    return try {
        val base64Content = if (path.startsWith("data:")) path.substringAfter(",") else path
        val binaryString = window.atob(base64Content)
        val bytes = ByteArray(binaryString.length)
        for (i in binaryString.indices) {
            bytes[i] = binaryString[i].code.toByte()
        }
        bytes
    } catch (e: Exception) {
        println("[ChatRepository.Web]: Помилка декодування файлу: ${e.message}")
        byteArrayOf()
    }
}

/**
 * [platformCompressImage] — Стиснення зображення через Canvas у браузері.
 */
actual suspend fun platformCompressImage(path: String): ByteArray {
    if (!path.startsWith("data:image")) return platformReadFileAsBytes(path)
    
    return suspendCoroutine { continuation ->
        val img = window.document.createElement("img") as HTMLImageElement
        img.onload = {
            val canvas = window.document.createElement("canvas") as HTMLCanvasElement
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            
            var width = img.width.toDouble()
            var height = img.height.toDouble()
            val maxSide = 1280.0
            
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
                bytes[i] = binaryString[i].code.toByte()
            }
            continuation.resume(bytes)
        }
        img.onerror = { _, _, _, _, _ ->
            // В случае ошибки возвращаем пустой массив, чтобы не блокировать поток
            continuation.resume(byteArrayOf())
        }
        img.src = path
    }
}
