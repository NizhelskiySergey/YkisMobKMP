package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import com.ykis.ykismobkmp.core.utils.Resource

@Composable
actual fun platformActivityContext(): Any? = null
actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  onError("Вхід через Google доступний тільки на мобільних пристроях.")
}

actual fun encodeBase64(bytes: ByteArray): String {
    return java.util.Base64.getEncoder().encodeToString(bytes)
}

actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String, String?, String?) -> Unit,
    onError: (String) -> Unit
) {
    onError("Apple ID не підтримується на Desktop")
}

actual suspend fun performPlatformSignInWithApple(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    idToken: String,
    rawNonce: String?,
    authCode: String?
): Resource<Boolean> = Resource.Error("Apple ID не підтримується на Desktop")

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
