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

/**
 * [NativeAuthBridge] — Інтерфейс для виклику нативних функцій ОС (Google, Apple, Firebase AI).
 * Цей інтерфейс реалізується в Swift на стороні iOS.
 */
interface NativeAuthBridge {
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun signInWithApple(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    
    // FIREBASE AI LOGIC (iOS)
    fun generateAiContent(prompt: String, onResult: (String?, String?) -> Unit)
    fun analyzeAiImage(prompt: String, imageBase64: String, onResult: (String?, String?) -> Unit)
}

/**
 * [getNativeBridge] — Отримання реалізації моста (тільки для iOS).
 */
expect fun getNativeBridge(): NativeAuthBridge?
