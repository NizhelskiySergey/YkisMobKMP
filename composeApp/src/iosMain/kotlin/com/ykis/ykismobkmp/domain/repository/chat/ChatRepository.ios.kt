package com.ykis.ykismobkmp.domain.repository.chat


import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy // ПРАВИЛЬНЫЙ ИМПОРТ ТУТ
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
  val data = NSData.create(contentsOfFile = path) ?: return byteArrayOf()
  val bytes = ByteArray(data.length.toInt())

  bytes.usePinned { pinned ->
    // Используем memcpy из posix для копирования данных из NSData в ByteArray
    memcpy(pinned.addressOf(0), data.bytes, data.length)
  }

  return bytes
}

actual suspend fun platformCompressImage(path: String): ByteArray {
  // Пока возвращаем просто байты файла
  return platformReadFileAsBytes(path)
}
