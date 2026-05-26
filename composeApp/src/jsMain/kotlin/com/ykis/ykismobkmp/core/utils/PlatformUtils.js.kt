package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
@Composable
actual fun platformActivityContext(): Any? = null
actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  println("[YkisLogKMP.PlatformUtils]: [JS_WEB] Сценарій входу через Google ізольований для браузера")
  onError("Вхід через Google недоступний у Веб-версії програми.")
}
