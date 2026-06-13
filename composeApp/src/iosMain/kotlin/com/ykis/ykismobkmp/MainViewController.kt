package com.ykis.ykismobkmp

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import platform.UIKit.UIViewController
import platform.UIKit.UIScreen
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import com.ykis.ykismobkmp.ui.navigation.YkisPamApp

/**
 * [MainViewController] — Главная точка входа графического холста для iOS.
 * ИСПРАВЛЕНО: Теперь WindowSizeClass вычисляется динамически на основе реальных размеров экрана.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalForeignApi::class)
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        // ИСПРАВЛЕНО: Используем BoxWithConstraints для реактивного отслеживания поворота экрана
        androidx.compose.foundation.layout.BoxWithConstraints {
            val dpSize = DpSize(maxWidth, maxHeight)
            
            // Этот код будет перезапускаться при каждом повороте устройства
            val windowSizeClass = WindowSizeClass.calculateFromSize(dpSize)
            
            println("[YkisLogKMP.IOS_ROOT]: Recalculated size: width=${maxWidth}, height=${maxHeight} -> ${windowSizeClass.widthSizeClass}")
            
            YkisPamApp(
                windowSize = windowSizeClass,
                displayFeatures = emptyList()
            )
        }
    }
}

/**
 * Метод для Swift-слоя (ContentView.swift).
 * ИСПРАВЛЕНО: Теперь возвращает реальные размеры вместо заглушки смартфона.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalForeignApi::class)
fun createDefaultWindowSizeClass(): WindowSizeClass {
    val dpSize = UIScreen.mainScreen.bounds.useContents {
        DpSize(size.width.dp, size.height.dp)
    }
    return WindowSizeClass.calculateFromSize(dpSize)
}
