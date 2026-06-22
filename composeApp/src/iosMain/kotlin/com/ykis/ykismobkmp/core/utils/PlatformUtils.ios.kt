package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
actual fun platformActivityContext(): Any? = null

actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  val bridge = IosAuthConnector.bridge
  if (bridge != null) {
      println("[YkisLogKMP.PlatformUtils]: [iOS_GOOGLE] Запуск нативного моста Swift")
      bridge.signInWithGoogle(
          onSuccess = { token -> onTokenReceived(token) },
          onError = { error -> onError(error) }
      )
  } else {
      println("[YkisLogKMP.PlatformUtils]: [iOS_ERROR] NativeAuthBridge не ініціалізовано")
      onError("Нативна авторизація не налаштована")
  }
}

/**
 * [triggerNativeAppleSignIn] — Кроссплатформенный запуск нативного диалога Apple ID.
 */
actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    val bridge = IosAuthConnector.bridge
    if (bridge != null) {
        println("[YkisLogKMP.PlatformUtils]: [iOS_APPLE] Запуск нативного моста Apple")
        bridge.signInWithApple(
            onSuccess = { token -> onTokenReceived(token) },
            onError = { error -> onError(error) }
        )
    } else {
        onError("Apple Auth не налаштована")
    }
}

/**
 * [performPlatformSignInWithApple] — Спеціальна реалізація для iOS через OAuthProvider.
 */
actual suspend fun performPlatformSignInWithApple(
    auth: FirebaseAuth,
    idToken: String,
    rawNonce: String?
): Resource<Boolean> = try {
    val appleProvider = OAuthProvider("apple.com")
    val credential = appleProvider.credential(idToken = idToken, accessToken = "", rawNonce = rawNonce)
    auth.signInWithCredential(credential)
    Resource.Success(true)
} catch (e: Exception) {
    Resource.Error(message = e.message ?: "Apple Auth Failed")
}
