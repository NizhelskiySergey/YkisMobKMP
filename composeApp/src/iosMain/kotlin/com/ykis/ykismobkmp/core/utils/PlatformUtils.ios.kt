package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
@Composable
actual fun platformActivityContext(): Any? = null
actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  println("[YkisLogKMP.PlatformUtils]: [iOS_GOOGLE_SIGN_IN] Нативний вхід Google ізольований для Apple платформ")
  onError("Вхід через Google на iOS тимчасово недоступний у тестовому білді.")
}
