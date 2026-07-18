package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

@Composable
expect fun platformActivityContext(): Any?

expect fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
)

expect fun encodeBase64(bytes: ByteArray): String

/**
 * [triggerNativeAppleSignIn] — Кроссплатформенный запуск нативного диалога Apple ID.
 */
expect fun triggerNativeAppleSignIn(
    onTokenReceived: (token: String, nonce: String?, authCode: String?) -> Unit,
    onError: (String) -> Unit
)

/**
 * [performPlatformSignInWithApple] — Специальная реализация для iOS. 
 */
expect suspend fun performPlatformSignInWithApple(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    idToken: String,
    rawNonce: String?,
    authCode: String?
): Resource<Boolean>

expect suspend fun performPlatformSendSms(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    phoneNumber: String,
    platformActivity: Any?
): Resource<String>

expect suspend fun performPlatformSignInWithSms(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    verificationId: String,
    smsCode: String
): Resource<String>

interface NativeAuthBridge {
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun signInWithApple(onSuccess: (String, String?, String?) -> Unit, onError: (String) -> Unit)
    
    // SMS AUTH (iOS)
    fun sendSmsCode(phoneNumber: String, onSuccess: (String) -> Unit, onError: (String) -> Unit)

    // FIREBASE AI LOGIC (iOS)
    fun generateAiContent(prompt: String, onResult: (String?, String?) -> Unit)
    fun analyzeAiImage(prompt: String, imageBase64: String, onResult: (String?, String?) -> Unit)
}

expect fun getNativeBridge(): NativeAuthBridge?

expect suspend fun getPlatformFcmToken(): String?
expect fun performPlatformClearNotifications(chatId: String?)

object IosAuthConnector {
    var bridge: NativeAuthBridge? = null
}
