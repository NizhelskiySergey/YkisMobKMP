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
 * [encodeBase64] — Універсальне KMP-перетворення масиву байтів у рядок Base64.
 */
expect fun encodeBase64(bytes: ByteArray): String

/**
 * [triggerNativeAppleSignIn] — Кроссплатформенный запуск нативного диалога Apple ID.
 */
expect fun triggerNativeAppleSignIn(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
)

/**
 * [performPlatformSignInWithApple] — Спеціальна реалізація для iOS через OAuthProvider.
 */
expect suspend fun performPlatformSignInWithApple(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    idToken: String,
    rawNonce: String?
): Resource<Boolean>

