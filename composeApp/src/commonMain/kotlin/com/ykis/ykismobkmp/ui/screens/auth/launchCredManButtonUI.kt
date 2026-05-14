package com.ykis.ykismobkmp.ui.screens.auth

import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.SnackbarManager


private suspend fun launchCredManButtonUI(
  context: Context,
  onFinished: () -> Unit, // Сигнал для выключения лоадера
  onRequestResult: (Credential) -> Unit
) {
  val methodName = "Auth.launchCredMan"
  try {
    Log.d("YkisLog", "$methodName: [START] Открытие системного окна Google")

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(false) // Позволяет выбрать любой аккаунт на устройстве
      .setServerClientId("1062920014188-8s41hcrkkik155m7mo2spj26jupp27e5.apps.googleusercontent.com")
      .setAutoSelectEnabled(false)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    val result = CredentialManager.create(context).getCredential(
      request = request,
      context = context
    )

    Log.d("YkisLog", "$methodName: [SUCCESS] Аккаунт выбран")
    onRequestResult(result.credential)

  } catch (e: GetCredentialException) {
    when (e) {
      is GetCredentialCancellationException -> {
        Log.d("YkisLog", "$methodName: [CANCEL] Пользователь закрыл окно")
      }
      else -> {
        Log.e("YkisLog", "$methodName: [ERROR] ${e.message}")
        SnackbarManager.showMessage("Помилка авторизації: ${e.localizedMessage}")
      }
    }
  } finally {
    // КРИТИЧЕСКИ ВАЖНО: всегда уведомляем кнопку об окончании,
    // чтобы выключить локальный лоадер (isChoosingAccount = false)
    onFinished()
  }
}
