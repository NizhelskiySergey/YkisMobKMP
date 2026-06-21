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
import platform.Foundation.NSUserDefaults
import com.russhwolf.settings.NSUserDefaultsSettings

/**
 * [MainViewController] — Головна точка входу графічного полотна для iOS.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalForeignApi::class)
fun MainViewController(): UIViewController {
    
    // Встановлюємо мову перед запуском UI
    updateIosLocale()

    return ComposeUIViewController {
        androidx.compose.foundation.layout.BoxWithConstraints {
            val dpSize = DpSize(maxWidth, maxHeight)
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
 * Оновлення мови для iOS на основі збережених налаштувань.
 */
private fun updateIosLocale() {
    val userDefaults = NSUserDefaults.standardUserDefaults
    val settings = NSUserDefaultsSettings(userDefaults)
    val lang = settings.getString("app_language", "uk")
    
    // Force set the language for the app
    userDefaults.setObject(listOf(lang), "AppleLanguages")
    userDefaults.synchronize()
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalForeignApi::class)
fun createDefaultWindowSizeClass(): WindowSizeClass {
    val dpSize = UIScreen.mainScreen.bounds.useContents {
        DpSize(size.width.dp, size.height.dp)
    }
    return WindowSizeClass.calculateFromSize(dpSize)
}
