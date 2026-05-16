package com.ykis.ykismobkmp.ui.navigation


import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable

private const val className = "AdaptiveWindowSizeBridge"

/**
 * [rememberAdaptiveLayoutType] — Единый кроссплатформенный expect-контракт вычисления геометрии.
 * Каждая платформа (actual) реализует его нативно, учитывая позы экрана и размеры окон.
 */
@Composable
expect fun rememberAdaptiveLayoutType(
  windowSize: WindowSizeClass,
  displayFeatures: List<Any> // Универсальный список для проброса платформенных особенностей (Fold API)
): Pair<NavigationType, ContentType>
