package com.ykis.ykismobkmp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

private const val className = "CameraView"

/**
 * [CameraView] — Нативная Android-реализация компонента съемки счетчиков биллинга ЮКИС.
 * Полностью сохраняет твою логику runtime-проверки CAMERA разрешений и блокировки затвора.
 */
@Composable
actual fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
  var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

  // ИСПРАВЛЕНО: Состояние блокировки кнопок переведено на КМР rememberSaveable для защиты от рекомпозиций экрана
  var isCapturing by rememberSaveable { mutableStateOf(false) }

  // Контракт системного лаунчера запроса на доступ к аппаратной камере смартфона
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (!isGranted) {
      println("[$className]: Користувач відхилив доступ до камери. Повернення.")
      onBack()
    }
  }

  // Триггер мгновенной проверки манифеста безопасности при входе на экран фотофиксации
  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
          scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        cameraProviderFuture.addListener({
          try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(previewView.surfaceProvider)
            imageCapture = ImageCapture.Builder().build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA,
              preview,
              imageCapture
            )
          } catch (e: Exception) {
            println("[$className.init] Critical Binding Failed: ${e.message}")
          }
        }, ContextCompat.getMainExecutor(ctx))

        previewView
      },
      modifier = Modifier.fillMaxSize()
    )

    // Кнопка возврата в родительский UI-модуль
    IconButton(
      modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
      onClick = onBack,
      enabled = !isCapturing,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
      )
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Назад",
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

    // Кнопка затвора с каскадным занулением и блокировкой
    Button(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
      enabled = !isCapturing,
      onClick = {
        isCapturing = true
        val photoFile = File(context.cacheDir, "meter_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
          outputOptions,
          ContextCompat.getMainExecutor(context),
          object : ImageCapture.OnImageSavedCallback {
            override fun onError(e: ImageCaptureException) {
              isCapturing = false
              // ИСПРАВЛЕНО: Нативные Android-логи переведены на println() общего кода Котлина
              println("[$className.capture] Error code: ${e.imageCaptureError}, msg: ${e.message}")
            }

            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
              println("[$className.capture] Success capture. Internal file path: ${photoFile.absolutePath}")
              // Передаем точную КМР-строку пути в коллбэк общего Use Case
              onImageCaptured(photoFile.absolutePath)
              isCapturing = false
            }
          }
        )
      }
    ) {
      if (isCapturing) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary
        )
      } else {
        Text(
          text = "Зробити фото",
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  }
}

