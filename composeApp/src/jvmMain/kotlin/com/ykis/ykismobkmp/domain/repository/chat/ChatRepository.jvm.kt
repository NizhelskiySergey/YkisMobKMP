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
  val image = ImageIO.read(file) ?: return@withContext byteArrayOf()

  // Ресайз до 1200px
  val width = 1200
  val height = (image.height.toFloat() * (width.toFloat() / image.width)).toInt()

  val scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
  val bufferedResized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

  val g = bufferedResized.createGraphics()
  g.drawImage(scaledImage, 0, 0, null)
  g.dispose()

  val out = ByteArrayOutputStream()
  ImageIO.write(bufferedResized, "jpg", out)
  out.toByteArray()
}

actual suspend fun platformReadFileAsBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
  File(path).readBytes()
}
