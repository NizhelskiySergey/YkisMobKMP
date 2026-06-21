package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

@Composable
expect fun platformActivityContext(): Any?
/**
 * [triggerNativeGoogleSignIn] — Кроссплатформенный мост запуска оригинального диалога Google.
 * На Android запускает системную шторку Google Play Services, на iOS/Desktop возвращает заглушку.
 */
expect fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
)

/**
 * [triggerNativeAppleSignIn] — Кроссплатформенный запуск нативного диалога Apple ID.
 */
expect fun triggerNativeAppleSignIn(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
)

