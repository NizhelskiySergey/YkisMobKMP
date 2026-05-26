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
