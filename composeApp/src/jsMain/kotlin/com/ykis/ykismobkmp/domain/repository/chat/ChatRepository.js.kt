package com.ykis.ykismobkmp.domain.repository.chat

/**
 * [platformReadFileAsBytes] — Web-реалізація перетворення Base64 (з камери/файлу) у масив байтів.
 */
actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
    return try {
        if (path.startsWith("data:")) {
            // Витягуємо чистий Base64 після коми: "data:image/jpeg;base64,XXXXX..."
            val base64Content = path.substringAfter(",")
            val binaryString = kotlinx.browser.window.atob(base64Content)
            val bytes = ByteArray(binaryString.length)
            for (i in binaryString.indices) {
                bytes[i] = binaryString[i].code.toByte()
            }
            bytes
        } else {
            byteArrayOf()
        }
    } catch (e: Exception) {
        println("[ChatRepository.Web]: Помилка декодування файлу: ${e.message}")
        byteArrayOf()
    }
}

/**
 * [platformCompressImage] — Для Web повертаємо як є, так як Canvas вже стиснув до 0.85.
 */
actual suspend fun platformCompressImage(path: String): ByteArray {
    return platformReadFileAsBytes(path)
}
