package com.ykis.ykismobkmp.domain.repository.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import java.awt.Image

actual suspend fun platformCompressImage(path: String): ByteArray = withContext(Dispatchers.IO) {
  val file = File(path)
  if (!file.exists()) return@withContext byteArrayOf()
  val image = ImageIO.read(file) ?: return@withContext byteArrayOf()

  // Ресайз до 1600px по довшій стороні
  val maxSide = 1600
  var width = image.width
  var height = image.height
  
  if (width > height && width > maxSide) {
      height = (height.toFloat() * (maxSide.toFloat() / width)).toInt()
      width = maxSide
  } else if (height > maxSide) {
      width = (width.toFloat() * (maxSide.toFloat() / height)).toInt()
      height = maxSide
  }

  val scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
  val bufferedResized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

  val g = bufferedResized.createGraphics()
  g.drawImage(scaledImage, 0, 0, null)
  g.dispose()

  val out = ByteArrayOutputStream()
  ImageIO.write(bufferedResized, "jpg", out)
  val result = out.toByteArray()
  println("[ChatRepository.jvm]: Фото стиснено. Розмір: ${result.size / 1024} КБ. Розміри: ${width}x${height}")
  result
}

actual suspend fun platformReadFileAsBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
  File(path).readBytes()
}
