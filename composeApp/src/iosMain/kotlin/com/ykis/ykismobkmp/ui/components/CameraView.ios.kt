package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.Foundation.NSUUID
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSData
import platform.Foundation.writeToFile
import platform.darwin.NSObject

private const val tag = "CameraView"

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
      val imageData = didFinishProcessingPhoto.fileDataRepresentation()
      if (imageData != null) {
        val rawPath = NSTemporaryDirectory() + NSUUID.UUID().UUIDString() + ".jpg"
        imageData.writeToFile(rawPath, true)
        println("[$tag.ios]: Фото збережено за шляхом: $rawPath")
        onCaptured(rawPath)
      }
    } else {
      println("[$tag.ios]: Помилка зйомки: ${error.localizedDescription}")
    }
    onFinished()
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

  LaunchedEffect(Unit) {
    captureSession.sessionPreset = AVCaptureSessionPresetPhoto
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return@LaunchedEffect
    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput ?: return@LaunchedEffect

    if (captureSession.canAddInput(input)) captureSession.addInput(input)
    if (captureSession.canAddOutput(photoOutput)) captureSession.addOutput(photoOutput)

    captureSession.startRunning()
  }

  DisposableEffect(captureSession) {
    onDispose {
      captureSession.stopRunning()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
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

    Button(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
      enabled = !isCapturing,
      onClick = {
        // ПРОВЕРКА: Есть ли активное соединение с камерой?
        // Это предотвращает краш "No active and enabled video connection"
        val connection = photoOutput.connectionWithMediaType(AVMediaTypeVideo)
        
        if (connection != null && connection.isActive()) {
          isCapturing = true

          val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
            mapOf(AVVideoCodecKey to AVVideoCodecJPEG)
          )

          photoOutput.capturePhotoWithSettings(
            settings = photoSettings,
            delegate = PhotoCaptureDelegate(
              onCaptured = { path -> onImageCaptured(path) },
              onFinished = { isCapturing = false }
            )
          )
        } else {
          println("[$tag.ios]: Камера ще не готова або недоступна (можливо, це симулятор)")
          // Если это симулятор, можно добавить эмуляцию фото для тестов
          // Но для реального устройства просто игнорируем нажатие, пока не появится картинка
        }
      }
    ) {
      Text(if (isCapturing) "Зйомка..." else "Зробити фото")
    }
  }
}
