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
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object IosAuthConnector {
    var bridge: NativeAuthBridge? = null
}

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
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    val bridge = IosAuthConnector.bridge
    if (bridge != null) {
        bridge.signInWithApple(
            onSuccess = { token -> onTokenReceived(token) },
            onError = { error -> onError(error) }
        )
    } else {
        onError("Apple Auth не налаштована")
    }
}

actual suspend fun performPlatformSignInWithApple(
    auth: FirebaseAuth,
    idToken: String,
    rawNonce: String?
): Resource<Boolean> = try {
    val appleCredential = OAuthProvider.credential(
        providerId = "apple.com",
        idToken = idToken,
        accessToken = "", 
        rawNonce = rawNonce
    )
    auth.signInWithCredential(appleCredential)
    Resource.Success(true)
} catch (e: Exception) {
    Resource.Error(message = e.message ?: "Apple Auth Failed")
}

actual fun getNativeBridge(): NativeAuthBridge? = IosAuthConnector.bridge
