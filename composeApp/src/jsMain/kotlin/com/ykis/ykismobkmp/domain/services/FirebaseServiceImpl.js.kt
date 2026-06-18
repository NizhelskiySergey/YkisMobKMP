package com.ykis.ykismobkmp.domain.services

import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.di.VAPID_KEY
import kotlinx.browser.window
import kotlinx.coroutines.await

// ИСПРАВЛЕНО: Сигнатура синхронизирована по параметрам
actual suspend fun performPlatformSendSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Сценарий SMS изолирован для Web-браузера")
  return Resource.Error("Вхід за номером телефону тимчасово обмежений у Веб-версії.")
}
actual suspend fun performPlatformSignInWithSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean> {
  println("[YkisLogKMP.FirebaseServiceImpl]: [JS_WEB] Сценарій входу за SMS ізольований для браузера")
  return Resource.Error("Функція авторизації за номером телефону недоступна у Веб-версії.")
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
