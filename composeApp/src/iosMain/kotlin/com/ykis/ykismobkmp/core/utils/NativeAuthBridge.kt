package com.ykis.ykismobkmp.core.utils

import kotlin.native.concurrent.ThreadLocal

/**
 * [NativeAuthBridge] — Кроссплатформенный мост для вызова Swift-кода из Kotlin.
 * Используется для Google Sign-In и Apple Sign-In на iOS.
 */
interface NativeAuthBridge {
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun signInWithApple(onSuccess: (String) -> Unit, onError: (String) -> Unit)
}

@ThreadLocal
object IosAuthConnector {
    var bridge: NativeAuthBridge? = null
}
