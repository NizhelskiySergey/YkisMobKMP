package com.ykis.ykismobkmp

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import platform.UIKit.UIViewController
import org.koin.dsl.module
import com.ykis.ykismobkmp.di.initKoin // Твой оригинальный метод из commonMain

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun createDefaultWindowSizeClass(): WindowSizeClass {
  return WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
}

/**
 * [MainViewController] — Главная точка входа графического холста для iOS.
 */
fun MainViewController(windowSize: WindowSizeClass): UIViewController {

  return ComposeUIViewController {
    YkisPamAppRoot(
      windowSize = windowSize,
      displayFeatures = emptyList()
    )
  }
}
