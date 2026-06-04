package com.ykis.ykismobkmp.domain.services

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.ykis.ykismobkmp.core.utils.Resource
import com.google.firebase.messaging.FirebaseMessaging
import dev.gitlive.firebase.auth.android
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

actual suspend fun performPlatformSendSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> = suspendCancellableCoroutine { continuation ->
  println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS] Запуск нативного провайдера Google")

  val activity = platformActivity as? Activity
  if (activity == null) {
    continuation.resume(Resource.Error("Android Activity отсутствует в контексте"))
    return@suspendCancellableCoroutine
  }

  val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS] Автоматическая мгновенная верификация")
    }

    override fun onVerificationFailed(e: FirebaseException) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS_ERROR] Отказ Google Cloud: ${e.message}")
      continuation.resume(Resource.Error(e.message ?: "Сбой отправки SMS"))
    }

    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS_SUCCESS] Код отправлен! ID сессии: $verificationId")
      continuation.resume(Resource.Success(verificationId))
    }
  }

  // Извлекаем оригинальный нативный инстанс Google через свойство .android библиотеки GitLive
  val nativeAndroidAuth = auth.android

  // ИСПРАВЛЕНО: Принудительно добавляем международный префикс к чистым цифрам из KMP стейта
  val fullFormattedPhoneNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+380$phoneNumber"

  val options = PhoneAuthOptions.newBuilder(nativeAndroidAuth)
    .setPhoneNumber(fullFormattedPhoneNumber) // Передаем полный валидный номер телефона
    .setTimeout(60L, TimeUnit.SECONDS)
    .setActivity(activity)
    .setCallbacks(callbacks)
    .build()


  PhoneAuthProvider.verifyPhoneNumber(options)
}

actual suspend fun performPlatformSignInWithSms(
  auth: dev.gitlive.firebase.auth.FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean> = suspendCancellableCoroutine { continuation ->
  try {
    println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH] Сборка нативного credential Google")

    // 1. Получаем оригинальный нативный токен от Google Android SDK
    val nativeCredential = PhoneAuthProvider.getCredential(verificationId, smsCode)

    // 2. Нативно авторизуем жильца непосредственно в нативном ядре FirebaseAuth Android
    auth.android.signInWithCredential(nativeCredential)
      .addOnSuccessListener {
        println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH_SUCCESS] Вход по телефону успешно завершен!")
        continuation.resume(Resource.Success(true))
      }
      .addOnFailureListener { e ->
        println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH_ERROR] ${e.message}")
        continuation.resume(Resource.Error(e.message ?: "Невірний код підтвердження"))
      }
  } catch (e: Exception) {
    println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH_CRITICAL] ${e.message}")
    continuation.resume(Resource.Error(e.message ?: "Сбой рантайма"))
  }
}

actual suspend fun getPlatformFcmToken(): String? = try {
  println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_FCM] Запрос нативного токена...")
  FirebaseMessaging.getInstance().token.await()
} catch (e: Exception) {
  println("[YkisLogKMP.FirebaseServiceImpl_ERROR]: Не удалось получить FCM токен: ${e.message}")
  null
}

actual fun performPlatformClearNotifications(chatId: String?) {
  try {
    // Получаем контекст через Firebase SDK (на Android он всегда доступен)
    val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    if (chatId != null) {
      // Очищаем уведомление конкретного чата
      notificationManager.cancel(chatId.hashCode())
      println("[YkisLogKMP.FirebaseServiceImpl]: [NOTIF_CLEAR] Уведомления чата $chatId очищены.")
    } else {
      // Очищаем ВСЕ уведомления приложения
      notificationManager.cancelAll()
      println("[YkisLogKMP.FirebaseServiceImpl]: [NOTIF_CLEAR] Все уведомления приложения очищены.")
    }
  } catch (e: Exception) {
    println("[YkisLogKMP.FirebaseServiceImpl_ERROR]: Ошибка очистки уведомлений: ${e.message}")
  }
}
