package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.OAuthProvider

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual fun encodeBase64(bytes: ByteArray): String {
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            .base64EncodedStringWithOptions(0UL)
    }
}

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
    println("[YkisLogKMP.PlatformUtils]: [iOS_APPLE_SIGN_IN] Створення креденшала для Apple")
    
    // Використовуємо іменовані аргументи для точності в KMP бібліотеці
    val appleCredential = OAuthProvider.credential(
        providerId = "apple.com",
        idToken = idToken,
        accessToken = "", 
        rawNonce = rawNonce
    )
    
    auth.signInWithCredential(appleCredential)
    println("[YkisLogKMP.PlatformUtils]: [iOS_APPLE_SIGN_IN] Вхід успішний")
    Resource.Success(true)
} catch (e: Exception) {
    println("[YkisLogKMP.PlatformUtils_ERROR]: Apple Auth Failed: ${e.message}")
    Resource.Error(message = e.message ?: "Apple Auth Failed")
}
