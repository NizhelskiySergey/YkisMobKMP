package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
  var isCameraAvailable by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    captureSession.sessionPreset = AVCaptureSessionPresetPhoto
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    if (device == null) {
      isCameraAvailable = false
      return@LaunchedEffect
    }

    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput
    if (input == null) {
      isCameraAvailable = false
      return@LaunchedEffect
    }

    if (captureSession.canAddInput(input)) captureSession.addInput(input)
    if (captureSession.canAddOutput(photoOutput)) captureSession.addOutput(photoOutput)

    captureSession.startRunning()
  }

  DisposableEffect(captureSession) {
    onDispose {
      captureSession.stopRunning()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    if (isCameraAvailable) {
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
    } else {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("Камера недоступна", color = Color.White)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
          Text("Повернутися назад")
        }
      }
    }

    // Кнопка назад вгорі зліва (завжди доступна)
    IconButton(
      modifier = Modifier
        .statusBarsPadding()
        .padding(16.dp)
        .align(Alignment.TopStart)
        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small),
      onClick = onBack
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Назад",
        tint = Color.White
      )
    }

    if (isCameraAvailable) {
      Button(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
        enabled = !isCapturing,
        onClick = {
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
            println("[$tag.ios]: Камера ще не готова")
          }
        }
      ) {
        Text(if (isCapturing) "Зйомка..." else "Зробити фото")
      }
    }
  }
}
