package com.ykis.ykismobkmp.ui.screens.chat


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.util.Log

@Composable
actual fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Камера недоступна на Desktop")
      Button(onClick = {
        // Здесь будет вызов системного диалога выбора файла (AWT FileDialog)
        Log.d("YkisLog", "[CameraView.Desktop]: Simulating photo capture via file picker")
      }) {
        Text("Вибрати існуюче фото")
      }
      TextButton(onClick = onBack) { Text("Назад") }
    }
  }
}
