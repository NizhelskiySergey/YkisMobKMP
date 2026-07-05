package com.ykis.ykismobkmp.domain.repository.chat

import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.get

/**
 * [platformReadFileAsBytes] — Web-реалізація отримання ОРИГІНАЛЬНИХ байтів файлу.
 */
actual suspend fun platformReadFileAsBytes(path: String): ByteArray = suspendCoroutine { continuation ->
    val file = (window.asDynamic()).lastSelectedFile as? File
    if (file == null) {
        continuation.resume(byteArrayOf())
        return@suspendCoroutine
    }

    val reader = FileReader()
    reader.onload = { event ->
        val buffer = event.target.asDynamic().result as ArrayBuffer
        val int8Array = Int8Array(buffer)
        val bytes = ByteArray(int8Array.length) { i -> int8Array[i] }
        continuation.resume(bytes)
    }
    reader.onerror = { continuation.resume(byteArrayOf()) }
    reader.readAsArrayBuffer(file)
}

/**
 * [platformCompressImage] — Нормалізація орієнтації та розмірів для Web.
 * ВИКОРИСТОВУЄМО naturalWidth/Height для виправлення бага орієнтації Skia.
 */
actual suspend fun platformCompressImage(path: String): ByteArray {
    return suspendCoroutine { continuation ->
        val img = window.document.createElement("img") as HTMLImageElement
        img.crossOrigin = "anonymous"
        img.onload = {
            val canvas = window.document.createElement("canvas") as HTMLCanvasElement
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            
            // ЧИТАЄМО ЕТАЛОННІ РОЗМІРИ (з урахуванням EXIF)
            val realW = img.naturalWidth.toDouble()
            val realH = img.naturalHeight.toDouble()
            
            // Обмежуємо для ШІ, зберігаючи еталонні пропорції
            val maxSide = 1200.0
            var targetW = realW
            var targetH = realH
            
            if (realW > realH && realW > maxSide) {
                targetH *= maxSide / realW
                targetW = maxSide
            } else if (realH > maxSide) {
                targetW *= maxSide / realH
                targetH = maxSide
            }
            
            canvas.width = targetW.toInt()
            canvas.height = targetH.toInt()
            
            // ПЕРЕМАЛЬОВУЄМО: Браузер автоматично розверне пікселі правильно при drawImage
            ctx.drawImage(img, 0.0, 0.0, targetW, targetH)
            
            val compressedBase64 = canvas.toDataURL("image/jpeg", 0.8)
            val binaryString = window.atob(compressedBase64.substringAfter(","))
            val bytes = ByteArray(binaryString.length) { i -> (binaryString[i].code and 0xFF).toByte() }
            continuation.resume(bytes)
        }
        img.onerror = { _, _, _, _, _ -> continuation.resume(byteArrayOf()) }
        img.src = path
    }
}
