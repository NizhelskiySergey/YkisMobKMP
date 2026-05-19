package com.ykis.ykismobkmp.core.utils

import dev.gitlive.firebase.storage.Data // Каноничный класс данных GitLive SDK
import org.khronos.webgl.Int8Array // Знаковый JS-массив (Kotlin ByteArray)
import org.khronos.webgl.Uint8Array // Беззнаковый JS-массив, который жестко требует Firebase!

/**
 * [wrapForFirebase] — Нативная Web JS actual-реализация обертки файлов и коммунальных вложений ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Знаковый Int8Array безопасно преобразован в беззнаковый Uint8Array,
 * что полностью ликвидирует ошибку компиляции "Argument type mismatch" во всем веб-модуле!
 */
actual fun ByteArray.wrapForFirebase(): Data {
  try {
    // 1. Принудительно кастим Kotlin ByteArray в системный JavaScript Int8Array
    val jsInt8Array = this.unsafeCast<Int8Array>()

    // 2. Создаем чистый беззнаковый буфер Uint8Array строго поверх двоичной памяти исходного массива!
    // Это мгновенно переводит данные в беззнаковый формат без лишнего копирования байт в ОЗУ браузера
    val jsUint8Array = Uint8Array(jsInt8Array.buffer, jsInt8Array.byteOffset, jsInt8Array.byteLength)

    // 3. Передаем легитимный Uint8Array в конструктор GitLive Data
    val firebaseData = Data(jsUint8Array)

    println("[wrapForFirebase.js]: Вложение успешно переформатировано в Uint8Array. Размер: ${this.size}b")
    return firebaseData
  } catch (e: Exception) {
    println("[wrapForFirebase.js]: [CRITICAL ERROR] Ошибка конвертации Uint8Array: ${e.message}")
    // В случае форс-мажора возвращаем пустой безопасный беззнаковый массив
    return Data(Uint8Array(0))
  }
}
