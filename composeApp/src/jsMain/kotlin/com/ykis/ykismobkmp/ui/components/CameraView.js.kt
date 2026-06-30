package com.ykis.ykismobkmp.ui.components

// ИМПОРТЫ НАТИВНОГО WEB API БРАУЗЕРА ИЗ KOTLIN/JS:

// ИМПОРТЫ НАТИВНОГО WEB API БРАУЗЕРА ИЗ KOTLIN/JS:
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStream

private const val className = "CameraView"

/**
 * [CameraView] — Нативная Web-реализация съемки счетчиков для браузеров (Kotlin/JS).
 * Запускает защищенный поток веб-камеры через HTML5 MediaDevices API.
 */
@Composable
actual fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
) {
  var isCapturing by rememberSaveable { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Храним ссылки на HTML5 элементы DOM-дерева браузера
  var activeStream by remember { mutableStateOf<MediaStream?>(null) }
  val videoElement = remember { document.createElement("video") as HTMLVideoElement }

  // ШАГ 1: Инициализация потока камеры при старте Composable-экрана в браузере
  LaunchedEffect(Unit) {
    val mediaDevices = window.navigator.mediaDevices

    // Формируем КМР-конфигурацию медиа-запроса
    val constraints = kotlin.js.json(
      "video" to kotlin.js.json(
        "facingMode" to "environment", // Задняя камера по умолчанию для съемки водомеров
        "width" to 1280,
        "height" to 720
      ),
      "audio" to false
    )

    // ИСПРАВЛЕНО: Удалены лишние закрывающие скобки, вызов getUserMedia переведен на .asDynamic()
    // Это полностью убирает ошибку "expected MediaStreamConstraints, actual Json"
    mediaDevices.asDynamic().getUserMedia(constraints)
      .then { stream: MediaStream ->
        activeStream = stream
        videoElement.srcObject = stream
        videoElement.setAttribute("playsinline", "true")
        videoElement.play()
        println("[$className.Web]: Потік веб-камери успішно запущено")
      }
      .catch { error ->
        println("[$className.Web] Помилка доступу до камери: $error")
        errorMessage = "Доступ до камери відхилено або пристрій зайнятий"
      }
  }

  // ШАГ 2: Гарантированное закрытие объектива и деактивация шторки камери при уходе с экрана
  DisposableEffect(Unit) {
    onDispose {
      activeStream?.getTracks()?.forEach { track ->
        track.stop() // Выключаем зеленый светодиод веб-камеры на Mac/ПК
      }
      videoElement.pause()
    }
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    if (errorMessage != null) {
      // Отрисовка ошибки отсутствия прав на камеру внутри вкладки браузера
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
      ) {
        Text(
          text = errorMessage ?: "",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
          Text("Повернутися")
        }
      }
    } else {
      // В КМР для JS видеопоток крутится в фоне в DOM-модели, выводим лаконичный Material 3 интерфейс
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Камера готова до зйомки",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Наведіть пристрій на табло лічильника та натисніть кнопку",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Кнопка закрытия окна камеры
      IconButton(
        modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
        onClick = onBack,
        enabled = !isCapturing
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Назад"
        )
      }

      // Кнопка моментального снимка (Затвор)
      Button(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
        enabled = !isCapturing && activeStream != null,
        onClick = {
          isCapturing = true
          try {
            // ШАГ 3: Создаем невидимый HTML5 Canvas холст для копирования кадра из видеопотока
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = videoElement.videoWidth
            canvas.height = videoElement.videoHeight

            val context = canvas.getContext("2d")
            // Прорисовываем текущий кадр из тега video на холст
            context?.asDynamic()?.drawImage(videoElement, 0, 0, canvas.width, canvas.height)

            // Конвертируем холст в сжатую универсальную Base64-строку JPEG
            val base64DataUrl = canvas.toDataURL("image/jpeg", 0.85)

            println("[$className.Web]: Знімок успішно сформовано в форматі Base64")

            // Передаем готовую ИИ-строку в общий Use Case
            onImageCaptured(base64DataUrl)
          } catch (e: Exception) {
            println("[$className.Web] Сбой захвата кадра: ${e.message}")
          } catch (e: dynamic) {
            println("[$className.Web] Сбой захвата кадра JS-error: $e")
          } finally {
            isCapturing = false
          }
        }
      ) {
        if (isCapturing) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
          Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Зробити фото")
        }
      }
    }
  }
}
