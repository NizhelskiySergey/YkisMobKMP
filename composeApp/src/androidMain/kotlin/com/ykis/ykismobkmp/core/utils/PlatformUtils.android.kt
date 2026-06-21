package com.ykis.ykismobkmp.core.utils


import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

  // 1. Извлекаем default_web_client_id, автоматически сгенерированный Firebase из твоего google-services.json
  val webClientId = try {
    val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
    if (resId != 0) activity.getString(resId) else ""
  } catch (e: Exception) { "" }

  if (webClientId.isBlank()) {
    onError("Не удалось найти default_web_client_id в ресурсах google-services.json")
    return
  }

  // 2. Создаем современную опцию запроса Google ID Token
  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false) // Показывать ВСЕ Google-аккаунты на устройстве, а не только ранее входившие
    .setServerClientId(webClientId)
    .setAutoSelectEnabled(false) // Дать пользователю явно кликнуть по своей почте
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  val credentialManager = CredentialManager.create(activity)

  // 3. Запускаем асинхронное всплывающее системное окно в фоновом Android-потоке
  CoroutineScope(Dispatchers.Main).launch {
    try {
      // Принудительно очищаем старый кэш авторизации, чтобы шторка выбора аккаунта всплывала всегда
      credentialManager.clearCredentialState(ClearCredentialStateRequest())

      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_LAUNCH] Вызов системного Bottom Sheet выбора аккаунтов")
      val result = credentialManager.getCredential(context = activity, request = request)

      // Извлекаем полученный защищенный токен из системного ответа
      val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data)
      val realIdToken = googleIdTokenCredential.idToken

      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_SUCCESS] JWT ID Токен успешно сгенерирован службами Google Play")
      onTokenReceived(realIdToken)

    } catch (e: Exception) {
      val errorMsg = e.message ?: "Сбой авторизации Credential Manager"
      println("[YkisLogKMP.PlatformUtils]: [CREDENTIAL_MANAGER_ERROR] $errorMsg")
      onError(errorMsg)
    }
  }
}

actual fun triggerNativeAppleSignIn(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    onError("Apple ID не підтримується на Android")
}

