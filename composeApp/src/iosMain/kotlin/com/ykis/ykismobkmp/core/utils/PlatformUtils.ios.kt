package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

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
