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
    onTokenReceived: (String, String?, String?) -> Unit,
    onError: (String) -> Unit
) {
    onError("Apple ID не підтримується в браузері")
}

actual suspend fun performPlatformSignInWithApple(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    idToken: String,
    rawNonce: String?,
    authCode: String?
): Resource<Boolean> = Resource.Error("Apple ID не підтримується в браузері")

actual suspend fun performPlatformSendSms(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    phoneNumber: String,
    platformActivity: Any?
): Resource<String> = Resource.Error("SMS не підтримується")

actual suspend fun performPlatformSignInWithSms(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    verificationId: String,
    smsCode: String
): Resource<String> = Resource.Error("SMS не підтримується")

actual suspend fun getPlatformFcmToken(): String? = null
actual fun performPlatformClearNotifications(chatId: String?) { }

actual fun getNativeBridge(): NativeAuthBridge? = null

/**
 * [encodeBase64] — Універсальне та швидке перетворення для Web.
 */
actual fun encodeBase64(bytes: ByteArray): String {
    val dynamicBytes = bytes.asDynamic()
    return js("""
        var uint8 = new Uint8Array(dynamicBytes);
        var binary = '';
        var len = uint8.byteLength;
        var chunkSize = 0x4000; 
        for (var i = 0; i < len; i += chunkSize) {
            var chunk = uint8.subarray(i, Math.min(i + chunkSize, len));
            binary += String.fromCharCode.apply(null, chunk);
        }
        return window.btoa(binary);
    """) as String
}
