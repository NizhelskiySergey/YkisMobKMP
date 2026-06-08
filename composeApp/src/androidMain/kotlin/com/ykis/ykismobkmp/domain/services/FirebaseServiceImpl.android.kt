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
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.android
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

actual suspend fun performPlatformSendSms(
  auth: FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> = suspendCancellableCoroutine { continuation ->
  println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS] Запуск нативного провайдера Google")

  val activity = platformActivity as? Activity
  if (activity == null) {
    continuation.resume(Resource.Error("Android Activity отсутствует в контексте"))
    return@suspendCancellableCoroutine
  }

  // Извлекаем оригинальный нативный инстанс Google через свойство .android библиотеки GitLive
  val nativeAndroidAuth = auth.android

  val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS] Автоматична миттєва верифікація")
      nativeAndroidAuth.signInWithCredential(credential)
        .addOnSuccessListener {
            println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS_AUTO_OK] Автоматичний вхід успішний")
        }
    }

    override fun onVerificationFailed(e: FirebaseException) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS_ERROR] Відмова Google Cloud: ${e.message}")
      if (continuation.isActive) continuation.resume(Resource.Error(e.message ?: "Сбой отправки SMS"))
    }

    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS_SUCCESS] Код надіслано! ID сесії: $verificationId")
      if (continuation.isActive) continuation.resume(Resource.Success(verificationId))
    }
  }

  val cleanPhone = phoneNumber.filter { it.isDigit() }
  val fullFormattedPhoneNumber = when {
      phoneNumber.startsWith("+") -> phoneNumber
      cleanPhone.startsWith("380") -> "+$cleanPhone"
      cleanPhone.startsWith("0") -> "+380${cleanPhone.drop(1)}"
      else -> "+380$cleanPhone"
  }
  
  println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_SMS_PREPARE] Номер: $fullFormattedPhoneNumber")

  try {
      // ИСПРАВЛЕНО: Принудительный запрос токена App Check перед отправкой SMS
      com.google.firebase.appcheck.FirebaseAppCheck.getInstance().getAppCheckToken(false).addOnCompleteListener { task ->
          if (task.isSuccessful) {
              println("[YkisLogKMP.FirebaseServiceImpl]: App Check токен отримано: ${task.result?.token?.take(10)}...")
          } else {
              println("[YkisLogKMP.FirebaseServiceImpl]: [APP_CHECK_FAIL] Помилка: ${task.exception?.message}")
          }
          
          // В ЛЮБОМ СЛУЧАЕ пробуем отправить SMS
          val options = PhoneAuthOptions.newBuilder(nativeAndroidAuth)
            .setPhoneNumber(fullFormattedPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

          PhoneAuthProvider.verifyPhoneNumber(options)
      }
  } catch (e: Exception) {
      println("[YkisLogKMP.FirebaseServiceImpl]: [CRITICAL_FAIL] ${e.message}")
  }
}

actual suspend fun performPlatformSignInWithSms(
  auth: FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<Boolean> = suspendCancellableCoroutine { continuation ->
  try {
    println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH] Сборка нативного credential Google")

    val nativeCredential = PhoneAuthProvider.getCredential(verificationId, smsCode)

    auth.android.signInWithCredential(nativeCredential)
      .addOnSuccessListener {
        println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH_SUCCESS] Вход по телефону успешно завершен!")
        continuation.resume(Resource.Success(true))
      }
      .addOnFailureListener { e ->
        println("[YkisLogKMP.FirebaseServiceImpl]: [ANDROID_AUTH_ERROR] ${e.message}")
        continuation.resume(Resource.Error(e.message ?: "Невірний код подветрждения"))
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
    val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    if (chatId != null) {
      notificationManager.cancel(chatId.hashCode())
      println("[YkisLogKMP.FirebaseServiceImpl]: [NOTIF_CLEAR] Уведомления чата $chatId очищены.")
    } else {
      notificationManager.cancelAll()
      println("[YkisLogKMP.FirebaseServiceImpl]: [NOTIF_CLEAR] Все уведомления приложения очищены.")
    }
  } catch (e: Exception) {
    println("[YkisLogKMP.FirebaseServiceImpl_ERROR]: Ошибка очистки уведомлений: ${e.message}")
  }
}
