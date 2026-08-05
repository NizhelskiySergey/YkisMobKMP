package com.ykis.ykismobkmp.core.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * НАПРАВЛЯЕМ КОНТЕКСТ НА ANDROID: Извлекаем текущую Activity из Compose рантайма.
 */
@Composable
actual fun platformActivityContext(): Any? {
  var context = LocalContext.current
  while (context is android.content.ContextWrapper) {
    if (context is Activity) return context
    context = context.baseContext
  }
  return null
}

actual fun triggerNativeGoogleSignIn(
  activityContext: Any?,
  onTokenReceived: (String) -> Unit,
  onError: (String) -> Unit
) {
  val activity = activityContext as? Activity
  if (activity == null) {
    onError("Android Activity отсутствует в контексте")
    return
  }

  println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER] Инициализация современного Google Credential Manager")

  // 1. Используем Web Client ID напрямую из google-services.json (Тип 3)
  val webClientId = "1062920014188-8s41hcrkkik155m7mo2spj26jupp27e5.apps.googleusercontent.com"

  // 2. Создаем современную опцию запроса Google ID Token
  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(webClientId)
    .setAutoSelectEnabled(false)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  val credentialManager = CredentialManager.create(activity)

  // 3. Запускаем асинхронное всплывающее системное окно в фоновом Android-потоке
  CoroutineScope(Dispatchers.Main).launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())

      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_LAUNCH] Вызов системного Bottom Sheet выбора аккаунтов")
      val result = credentialManager.getCredential(context = activity, request = request)

      val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data)
      val realIdToken = googleIdTokenCredential.idToken

      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_SUCCESS] JWT ID Токен успешно сгенерирован службами Google Play")
      onTokenReceived(realIdToken)

    } catch (e: GetCredentialCancellationException) {
      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_CANCEL] Пользователь отменил вход")
      onError("Canceled")
    } catch (e: NoCredentialException) {
      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_NO_CRED] Аккаунты не найдены")
      onError("No accounts found")
    } catch (e: Exception) {
      val errorMsg = e.message ?: "Сбой авторизации Credential Manager"
      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_ERROR] $errorMsg")
      onError(errorMsg)
    }
  }
}

actual fun encodeBase64(bytes: ByteArray): String {
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String, String?, String?) -> Unit,
    onError: (String) -> Unit
) {
    onError("Apple ID не підтримується на Android")
}

actual suspend fun performPlatformSignInWithApple(
    auth: FirebaseAuth,
    idToken: String,
    rawNonce: String?,
    authCode: String?
): Resource<Boolean> = Resource.Error("Apple ID не підтримується на Android")

actual suspend fun performPlatformSendSms(
  auth: FirebaseAuth,
  phoneNumber: String,
  platformActivity: Any?
): Resource<String> = suspendCancellableCoroutine { continuation ->
  val activity = platformActivity as? Activity
  if (activity == null) {
    continuation.resume(Resource.Error("Android Activity отсутствует"))
    return@suspendCancellableCoroutine
  }

  val nativeAndroidAuth = auth.android

  val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
      nativeAndroidAuth.signInWithCredential(credential).addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(Resource.Success(result.user?.uid ?: "INSTANT_OK"))
      }
    }

    override fun onVerificationFailed(e: FirebaseException) {
      if (continuation.isActive) continuation.resume(Resource.Error(e.message ?: "SMS Error"))
    }

    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
      if (continuation.isActive) continuation.resume(Resource.Success(verificationId))
    }
  }

  val options = PhoneAuthOptions.newBuilder(nativeAndroidAuth)
    .setPhoneNumber(phoneNumber)
    .setTimeout(60L, TimeUnit.SECONDS)
    .setActivity(activity)
    .setCallbacks(callbacks)
    .build()

  PhoneAuthProvider.verifyPhoneNumber(options)
}

actual suspend fun performPlatformSignInWithSms(
  auth: FirebaseAuth,
  verificationId: String,
  smsCode: String
): Resource<String> = suspendCancellableCoroutine { continuation ->
  try {
    val nativeCredential = PhoneAuthProvider.getCredential(verificationId, smsCode)
    auth.android.signInWithCredential(nativeCredential)
      .addOnSuccessListener { result ->
        val uid = result.user?.uid ?: ""
        continuation.resume(Resource.Success(uid))
      }
      .addOnFailureListener { e ->
        continuation.resume(Resource.Error(e.message ?: "Помилка входу"))
      }
  } catch (e: Exception) {
    continuation.resume(Resource.Error("Сбой рантайма"))
  }
}

actual suspend fun getPlatformFcmToken(): String? = try {
  FirebaseMessaging.getInstance().token.await()
} catch (e: Exception) {
  null
}

actual fun performPlatformClearNotifications(chatId: String?) {
  try {
    val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (chatId != null) {
      notificationManager.cancel(chatId.hashCode())
    } else {
      notificationManager.cancelAll()
    }
  } catch (e: Exception) { }
}

actual fun getNativeBridge(): NativeAuthBridge? = null
