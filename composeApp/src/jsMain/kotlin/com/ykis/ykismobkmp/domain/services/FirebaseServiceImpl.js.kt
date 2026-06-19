package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.di.VAPID_KEY
import kotlinx.browser.window
import kotlinx.coroutines.await

// ИСПРАВЛЕНО: Реалізація SMS через JS міст для Web
actual suspend fun performPlatformSendSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Відправка SMS на $phoneNumber...")
  return try {
    val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+380$phoneNumber"
    val promise = window.asDynamic().sendSmsWeb(formattedPhone)
    (promise as kotlin.js.Promise<String>).await()
    Resource.Success("WEB_SMS_SESSION")
  } catch (e: Exception) {
    println("[YkisLogKMP.FirebaseServiceImpl_ERROR]: ${e.message}")
    Resource.Error(e.message ?: "Помилка відправки SMS")
  }
}

actual suspend fun performPlatformSignInWithSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Підтвердження SMS коду...")
  return try {
    val promise = window.asDynamic().verifySmsWeb(smsCode)
    (promise as kotlin.js.Promise<Boolean>).await()
    Resource.Success(true)
  } catch (e: Exception) {
    println("[YkisLogKMP.FirebaseServiceImpl_ERROR]: ${e.message}")
    Resource.Error(e.message ?: "Невірний код")
  }
}

actual suspend fun getPlatformFcmToken(): String? {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Запит FCM токена...")
  return try {
    val promise = window.asDynamic().getWebFcmToken(VAPID_KEY)
    if (promise != null) {
      val token = (promise as kotlin.js.Promise<String?>).await()
      println("[YkisLogKMP.FirebaseServiceImpl]: [SUCCESS] Web Push Token отримано")
      token
    } else null
  } catch (e: Exception) {
    println("[YkisLogKMP.FirebaseServiceImpl_ERROR]: Не вдалося отримати Web Push Token: ${e.message}")
    null
  }
}

actual fun performPlatformClearNotifications(chatId: String?) {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_NOTIF_CLEAR] (Заглушка)")
}
