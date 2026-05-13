package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIView
import platform.darwin.NSObject

/**
 * [PhotoCaptureDelegate] — Нативный Objective-C делегат для захвата фото.
 * Наследование от NSObject автоматически закрывает все системные методы Apple (hash, isEqual).
 */
@OptIn(ExperimentalForeignApi::class)
class PhotoCaptureDelegate(
  private val onCaptured: (String) -> Unit,
  private val onFinished: () -> Unit
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

  override fun captureOutput(
    captureOutput: AVCapturePhotoOutput,
    didFinishProcessingPhoto: AVCapturePhoto,
    error: platform.Foundation.NSError?
  ) {
    if (error == null) {
      // Извлекаем байты JPEG напрямую из современного объекта AVCapturePhoto
      val imageData = didFinishProcessingPhoto.fileDataRepresentation()
      if (imageData != null) {
        val rawPath = NSTemporaryDirectory() + NSUUID.UUID().UUIDString() + ".jpg"
        imageData.writeToFile(rawPath, true)
        println("[CameraView.ios]: Фото збережено за шляхом: $rawPath")
        onCaptured(rawPath)
      }
    } else {
      println("[CameraView.ios]: Помилка зйомки: ${error.localizedDescription}")
    }
    onFinished() // Сбрасываем флаг загрузки в Compose UI
  }
}

/**
 * [CameraView] — iOS-реализация кроссплатформенного компонента камеры.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
) {
  val captureSession = remember { AVCaptureSession() }
  val photoOutput = remember { AVCapturePhotoOutput() }
  var isCapturing by remember { mutableStateOf(false) }

  // Инициализация сессии камеры при старте экрана
  LaunchedEffect(Unit) {
    captureSession.sessionPreset = AVCaptureSessionPresetPhoto
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return@LaunchedEffect
    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput ?: return@LaunchedEffect

    if (captureSession.canAddInput(input)) captureSession.addInput(input)
    if (captureSession.canAddOutput(photoOutput)) captureSession.addOutput(photoOutput)

    captureSession.startRunning()
  }

  // Автоматическая остановка камеры при уходе с экрана
  DisposableEffect(captureSession) {
    onDispose {
      captureSession.stopRunning()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Отрисовка живого видеопотока iOS внутри Jetpack Compose холста
    UIKitView(
      modifier = Modifier.fillMaxSize(),
      factory = {
        UIView(frame = CGRectZero.readValue()).apply {
          val previewLayer = AVCaptureVideoPreviewLayer.layerWithSession(captureSession).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
            frame = layer.bounds
          }
          layer.addSublayer(previewLayer)
        }
      }
    )

    // Кнопка затвора камеры
    Button(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
      enabled = !isCapturing,
      onClick = {
        isCapturing = true

        // Настройки формата JPEG для сохранения на диск
        val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
          mapOf(AVVideoCodecKey to AVVideoCodecJPEG)
        )

        // Запускаем захват, передавая наш NSObject-делегат
        photoOutput.capturePhotoWithSettings(
          settings = photoSettings,
          delegate = PhotoCaptureDelegate(
            onCaptured = { path -> onImageCaptured(path) },
            onFinished = { isCapturing = false }
          )
        )
      }
    ) {
      Text(if (isCapturing) "Зйомка..." else "Зробити фото")
    }
  }
}

