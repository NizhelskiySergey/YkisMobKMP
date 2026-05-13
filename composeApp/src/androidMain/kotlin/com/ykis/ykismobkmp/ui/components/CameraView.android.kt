package com.ykis.ykismobkmp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.camera.lifecycle.ProcessCameraProvider

import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.*

private const val className = "CameraView"

@Composable
actual fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
  var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
  var isCapturing by remember { mutableStateOf(false) }

  // Проверка разрешений
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (!isGranted) onBack()
  }

  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { ctx ->
        // Создаем PreviewView явно
        val previewView = PreviewView(ctx).apply {
          scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        cameraProviderFuture.addListener({
          try {
            val cameraProvider = cameraProviderFuture.get()

            // 1. ИСПРАВЛЕНО: Чистый Builder без лишнего .also внутри себя
            val preview = Preview.Builder().build()

            // 2. ИСПРАВЛЕНО: Метод setSurfaceProvider вызывается у ГОТОВОГО объекта preview
            // и принимает surfaceProvider от созданного previewView
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
            println("[$className.init]: Binding failed, ${e.message}")
          }
        }, ContextCompat.getMainExecutor(ctx))

        previewView // Возвращаем view из фабрики
      },
      modifier = Modifier.fillMaxSize()
    )


    // Кнопка Назад
    IconButton(
      modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
      onClick = onBack,
      enabled = !isCapturing
    ) {
      Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }

    // Кнопка затвора
    Button(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
      enabled = !isCapturing,
      onClick = {
        isCapturing = true
        val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
          outputOptions,
          ContextCompat.getMainExecutor(context),
          object : ImageCapture.OnImageSavedCallback {
            override fun onError(e: ImageCaptureException) {
              isCapturing = false
              Log.e("YkisLog", "[$className.capture]: Error: ${e.message}")
            }
            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
              Log.d("YkisLog", "[$className.capture]: Success. Path: ${photoFile.absolutePath}")
              onImageCaptured(photoFile.absolutePath)
              isCapturing = false
            }
          }
        )
      }
    ) {
      if (isCapturing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
      else Text("Зробити фото")
    }
  }
}
