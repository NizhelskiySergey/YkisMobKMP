package com.ykis.ykismobkmp.domain.repository.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.ykis.ykismobkmp.core.utils.Log
import androidx.core.graphics.scale
import java.io.File
import java.io.ByteArrayOutputStream

/**
 * [platformCompressImage] — Android-реалізація стиснення та нормалізації орієнтації.
 * ВИПРАВЛЕНО: Додано обробку EXIF Orientation, щоб фото не завантажувались повернутими.
 */
actual suspend fun platformCompressImage(path: String): ByteArray {
    val file = File(path)
    if (!file.exists()) return byteArrayOf()

    return try {
        // 1. Отримуємо орієнтацію з EXIF
        val exifInterface = ExifInterface(path)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        // 2. Декодуємо Bitmap (з невеликим sampleSize для економії пам'яті)
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1 // Для максимальної якості при 1600px краще 1
        }
        val original = BitmapFactory.decodeFile(file.absolutePath, options) ?: return byteArrayOf()

        // 3. Створюємо матрицю повороту
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        // 4. Повертаємо, якщо потрібно
        val rotated = if (!matrix.isIdentity) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } else {
            original
        }

        // 5. Масштабуємо до 1600px по довшій стороні (стандарт як на Web)
        val maxSide = 1600
        val width: Int
        val height: Int
        
        if (rotated.width > rotated.height) {
            width = maxSide
            height = (rotated.height.toFloat() * (maxSide.toFloat() / rotated.width)).toInt()
        } else {
            height = maxSide
            width = (rotated.width.toFloat() * (maxSide.toFloat() / rotated.height)).toInt()
        }

        val scaled = rotated.scale(width, height, true)

        // 6. Стискаємо в JPEG
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        val result = out.toByteArray()

        // 7. Очищення пам'яті
        if (original != rotated) original.recycle()
        rotated.recycle()
        if (rotated != scaled) scaled.recycle()

        println("[ChatRepository.android]: Фото нормалізовано. Розмір: ${result.size / 1024} КБ. Розміри: ${width}x${height}")
        result
    } catch (e: Exception) {
        println("[ChatRepository.android_ERROR]: Помилка стиснення: ${e.message}")
        byteArrayOf()
    }
}

actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
    return try {
        File(path).readBytes()
    } catch (e: Exception) {
        byteArrayOf()
    }
}
