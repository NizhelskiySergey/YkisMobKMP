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
import dev.gitlive.firebase.auth.PhoneAuthProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import platform.UserNotifications.UNUserNotificationCenter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlin.coroutines.resume

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
      bridge.signInWithGoogle(
          onSuccess = { token -> onTokenReceived(token) },
          onError = { error -> onError(error) }
      )
  } else {
      onError("Нативна авторизація не налаштована")
  }
}

actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String, String?, String?) -> Unit,
    onError: (String) -> Unit
) {
    val bridge = IosAuthConnector.bridge
    if (bridge != null) {
        bridge.signInWithApple(
            onSuccess = { token, nonce, authCode -> onTokenReceived(token, nonce, authCode) },
            onError = { error -> onError(error) }
        )
    } else {
        onError("Apple Auth не налаштована")
    }
}

actual suspend fun performPlatformSignInWithApple(
    auth: FirebaseAuth,
    idToken: String,
    rawNonce: String?,
    authCode: String?
): Resource<Boolean> = try {
    val appleCredential = OAuthProvider.credential(
        providerId = "apple.com",
        idToken = idToken,
        accessToken = authCode,
        rawNonce = rawNonce
    )
    auth.signInWithCredential(appleCredential)
    Resource.Success(true)
} catch (e: Exception) {
    Resource.Error(message = e.message ?: "Apple Auth Failed")
}

actual suspend fun performPlatformSendSms(
    auth: FirebaseAuth,
    phoneNumber: String,
    platformActivity: Any?
): Resource<String> = suspendCancellableCoroutine { continuation ->
    val bridge = IosAuthConnector.bridge
    if (bridge != null) {
        bridge.sendSmsCode(
            phoneNumber = phoneNumber,
            onSuccess = { vId -> 
                continuation.resume(Resource.Success(vId)) 
            },
            onError = { err -> 
                continuation.resume(Resource.Error(err)) 
            }
        )
    } else {
        continuation.resume(Resource.Error("Native Bridge not found"))
    }
}

actual suspend fun performPlatformSignInWithSms(
    auth: FirebaseAuth,
    verificationId: String,
    smsCode: String
): Resource<String> = try {
    val provider = PhoneAuthProvider(auth)
    val credential = provider.credential(verificationId, smsCode)
    val authResult = auth.signInWithCredential(credential)
    val uid = authResult.user?.uid ?: ""
    Resource.Success(uid)
} catch (e: Exception) {
    Resource.Error(message = e.message ?: "SMS Login Failed")
}

actual suspend fun getPlatformFcmToken(): String? {
  repeat(3) { attempt ->
    try {
      val token = Firebase.messaging.getToken()
      if (!token.isNullOrBlank()) return token
    } catch (e: Exception) {
      if (attempt < 2) delay(2000)
    }
  }
  return null
}

actual fun performPlatformClearNotifications(chatId: String?) {
  try {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.removeAllDeliveredNotifications()
  } catch (e: Exception) { }
}

actual fun getNativeBridge(): NativeAuthBridge? = IosAuthConnector.bridge
