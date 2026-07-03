package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import com.ykis.ykismobkmp.di.WEB_GOOGLE_CLIENT_ID
import kotlinx.browser.window

@Composable
actual fun platformActivityContext(): Any? = null

/**
 * [triggerNativeGoogleSignIn] — Реалізація для браузера через Google Identity Services (GIS).
 */
actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  println("[YkisLogKMP.PlatformUtils]: [JS_WEB] Запуск Google Auth (GIS)...")
  
  (window.asDynamic()).onGoogleTokenReceived = { credential: String ->
    println("[YkisLogKMP.PlatformUtils]: [SUCCESS] Токен отримано через JS міст")
    onTokenReceived(credential)
  }

  try {
    val bridge = (window.asDynamic()).triggerGoogleAuth
    if (bridge != null) {
        bridge(WEB_GOOGLE_CLIENT_ID)
    } else {
        println("[YkisLogKMP.PlatformUtils_ERROR]: JS функція triggerGoogleAuth не знайдена")
        onError("Помилка: Google Auth Bridge не ініціалізований")
    }
  } catch (e: Exception) {
    println("[YkisLogKMP.PlatformUtils_ERROR]: Не вдалося викликати JS міст: ${e.message}")
    onError("Помилка ініціалізації Google Auth")
  }
}

actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    onError("Apple ID не підтримується в браузері")
}

actual suspend fun performPlatformSignInWithApple(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    idToken: String,
    rawNonce: String?
): Resource<Boolean> = Resource.Error("Apple ID не підтримується в браузері")

/**
 * [encodeBase64] — Нативна JS реалізація для передачі зображень в ІІ.
 */
actual fun encodeBase64(bytes: ByteArray): String {
    val dynamicBytes = bytes.asDynamic()
    var binary = ""
    val len = dynamicBytes.length as Int
    for (i in 0 until len) {
        binary += window.asDynamic().String.fromCharCode(dynamicBytes[i]).unsafeCast<String>()
    }
    return window.btoa(binary)
}
