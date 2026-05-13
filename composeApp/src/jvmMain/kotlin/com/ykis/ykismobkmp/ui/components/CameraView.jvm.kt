package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.SwingPanel
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamPanel
import java.io.File
import javax.imageio.ImageIO

@Composable
actual fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
) {
  // Получаем дефолтную камеру FaceTime на Mac
  val webcam = remember { Webcam.getDefault() }
  var isCapturing by remember { mutableStateOf(false) }

  if (webcam == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Камера не знайдена на цьому Mac")
    }
    return
  }

  // Автоматически открываем и закрываем камеру при уничтожении Composable
  DisposableEffect(webcam) {
    if (!webcam.isOpen) {
      webcam.open()
    }
    onDispose {
      webcam.close()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Отрисовка нативного Swing-компонента с живым видеопотоком камеры внутри Compose
    SwingPanel(
      modifier = Modifier.fillMaxSize(),
      factory = {
        WebcamPanel(webcam).apply {
          // В Java-библиотеке Sarxos метод называется именно так:
          setAntialiasingEnabled(true)

          // Для флагов вывода FPS и размера используем стандартные Java-сеттеры
          isFPSDisplayed = false
          isImageSizeDisplayed = false
        }
      }
    )

    // Кнопка затвора
    Button(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
      enabled = !isCapturing,
      onClick = {
        isCapturing = true
        try {
          val appDir = File(System.getProperty("user.home"), ".ykis_app/cache")
          if (!appDir.exists()) appDir.mkdirs()

          val photoFile = File(appDir, "${System.currentTimeMillis()}.png")

          // Захватываем текущий BufferedImage из потока и сохраняем на диск
          val image = webcam.image
          ImageIO.write(image, "PNG", photoFile)

          println("[CameraView.jvm]: Фото успішно збережено: ${photoFile.absolutePath}")
          onImageCaptured(photoFile.absolutePath)
        } catch (e: Exception) {
          println("[CameraView.jvm] Помилка збереження кадру: ${e.message}")
        } finally {
          isCapturing = false
        }
      }
    ) {
      Text(if (isCapturing) "Збереження..." else "Зробити фото")
    }
  }
}
