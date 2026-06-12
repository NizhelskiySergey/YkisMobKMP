package com.ykis.ykismobkmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.navigation.YkisPamApp
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform

@Composable
fun YkisPamAppRoot(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any>
) {
  // 1. Стейт готовности ядра (Koin + СУБД)
  var isCoreReady by remember { mutableStateOf(false) }

  // 2. Безопасный "прогрев" системы (без лишних надписей)
  LaunchedEffect(Unit) {
    while (KoinPlatform.getKoinOrNull() == null) {
      delay(50)
    }
    // Короткая пауза для стабилизации нативных ресурсов
    delay(300)
    isCoreReady = true
  }

  if (!isCoreReady) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
  } else {
    // ЗАПУСК ОСНОВНОГО ПРИЛОЖЕНИЯ
    YkisPamApp(
      windowSize = windowSize,
      displayFeatures = displayFeatures
    )
  }
}
