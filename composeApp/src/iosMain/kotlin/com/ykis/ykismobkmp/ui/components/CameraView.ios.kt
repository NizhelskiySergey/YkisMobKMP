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
import kotlinx.cinterop.useContents
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.UIKit.UIColor
import platform.Foundation.NSUUID
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSData
import platform.Foundation.writeToFile
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.darwin.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val tag = "CameraView"

// Кастомна в'юшка для автоматичного керування розміром шару камери
@OptIn(ExperimentalForeignApi::class)
class CameraContainerView : UIView(CGRectZero.readValue()) {
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer?.setFrame(this.bounds)
        CATransaction.commit()
    }
}

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
        println("[$tag.ios]: Фото збережено: $rawPath")
        onCaptured(rawPath)
      }
    } else {
      println("[$tag.ios]: Помилка зйомки: ${error.localizedDescription}")
    }
    onFinished()
  }
}

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
  var isPermissionGranted by remember { mutableStateOf(false) }

  val previewLayer = remember {
      AVCaptureVideoPreviewLayer.layerWithSession(captureSession).apply {
          videoGravity = AVLayerVideoGravityResizeAspectFill
      }
  }

  LaunchedEffect(Unit) {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    if (status == AVAuthorizationStatusAuthorized) {
        isPermissionGranted = true
    } else if (status == AVAuthorizationStatusNotDetermined) {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            isPermissionGranted = granted
        }
    }

    withContext(Dispatchers.Default) {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device != null) {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput
            if (input != null && captureSession.canAddInput(input)) captureSession.addInput(input)
            if (captureSession.canAddOutput(photoOutput)) captureSession.addOutput(photoOutput)
        } else {
            isCameraAvailable = false
        }
        captureSession.commitConfiguration()
        if (!captureSession.isRunning()) captureSession.startRunning()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
        val queue = dispatch_get_global_queue(0L, 0u)
        dispatch_async(queue) {
            if (captureSession.isRunning()) captureSession.stopRunning()
        }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    if (isCameraAvailable && isPermissionGranted) {
      UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
          CameraContainerView().apply {
            this.previewLayer = previewLayer
            this.layer.addSublayer(previewLayer)
          }
        },
        update = { view ->
            if (!captureSession.isRunning()) {
                val queue = dispatch_get_global_queue(0L, 0u)
                dispatch_async(queue) { captureSession.startRunning() }
            }
        },
        interactive = true
      )
    } else {
      Column(
        modifier = Modifier.align(Alignment.Center).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val message = if (!isCameraAvailable) "Камера недоступна" else "Очікування дозволу..."
        Text(message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
          Text("Повернутися назад")
        }
      }
    }

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

    if (isCameraAvailable && isPermissionGranted) {
      Button(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
        enabled = !isCapturing,
        onClick = {
          val connection = photoOutput.connectionWithMediaType(AVMediaTypeVideo)
          if (connection?.isActive() == true) {
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
          }
        }
      ) {
        Text(if (isCapturing) "Зйомка..." else "Зробити фото")
      }
    }
  }
}
