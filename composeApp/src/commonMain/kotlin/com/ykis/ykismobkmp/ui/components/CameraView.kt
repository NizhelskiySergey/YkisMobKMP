package com.ykis.ykismobkmp.ui.components

import androidx.compose.runtime.Composable

/**
 * [CameraView] — Единый кроссплатформенный expect-контракт.
 */
@Composable
expect fun CameraView(
  onImageCaptured: (String) -> Unit,
  onBack: () -> Unit
)
