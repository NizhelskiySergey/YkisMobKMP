package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
@Composable
actual fun platformActivityContext(): Any? = null
actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  println("[YkisLogKMP.PlatformUtils]: [JVM_DESKTOP] Сценарій Google Auth недоступний на Mac/PC")
  onError("Вхід через Google доступний тільки на мобільних пристроях.")
}

actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    onError("Apple ID не підтримується на Desktop")
}

actual suspend fun performPlatformSignInWithApple(
    auth: dev.gitlive.firebase.auth.FirebaseAuth,
    idToken: String,
    rawNonce: String?
): Resource<Boolean> = Resource.Error("Apple ID не підтримується на Desktop")

