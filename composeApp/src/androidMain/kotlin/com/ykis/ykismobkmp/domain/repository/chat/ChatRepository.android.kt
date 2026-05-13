package com.ykis.ykismobkmp.domain.repository.chat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ykis.ykismobkmp.core.utils.Log
import java.io.File
import java.io.ByteArrayOutputStream

// Мы расширяем существующий класс actual методами
actual suspend fun platformCompressImage(path: String): ByteArray {
  val file = File(path)
  if (!file.exists()) return byteArrayOf()

  return try {
    // 1. Сначала читаем только границы, чтобы не грузить все фото в память (Опционально, но полезно)
    val options = BitmapFactory.Options().apply {
      inSampleSize = 2 // Твой выбор - это хороший баланс
    }

    // 2. Декодируем в Bitmap
    val original = BitmapFactory.decodeFile(file.absolutePath, options) ?: return byteArrayOf()

    // 3. Вычисляем пропорции для 1200px по ширине
    val width = 1200
    val height = (original.height.toFloat() * (width.toFloat() / original.width)).toInt()

    // 4. Создаем масштабированную копию
    val scaled = Bitmap.createScaledBitmap(original, width, height, true)

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
    val result = out.toByteArray()

    // 5. КРИТИЧНО: Всегда вызываем recycle для обоих объектов
    original.recycle()
    if (original != scaled) {
      scaled.recycle()
    }

    result
  } catch (e: Exception) {
    Log.e("YkisLog", "[PlatformFiles.android]: Error compressing $path -> ${e.message}")
    byteArrayOf()
  }
}

actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
  return File(path).readBytes()
}

