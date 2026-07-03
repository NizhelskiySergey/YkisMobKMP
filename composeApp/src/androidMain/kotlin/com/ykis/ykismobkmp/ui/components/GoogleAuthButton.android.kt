package com.ykis.ykismobkmp.ui.components


import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_google_logo

private const val tag = "GoogleAuthButton"

@Composable
actual fun GoogleAuthButton(
  buttonTextRes: Int,
  isLoading: Boolean,
  onTokenReceived: (String) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isChoosingAccount by remember { mutableStateOf(false) }

  OutlinedButton(
    onClick = {
      coroutineScope.launch {
        println("[$tag]: [CLICK] Включаем локальный лоадер выбора аккаунта")
        isChoosingAccount = true

        launchCredManButtonUI(
          context = context,
          onFinished = {
            isChoosingAccount = false
            println("[$tag]: [FINISHED] Локальный лоадер выключен")
          },
          onRequestResult = { credential ->
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
              try {
                // РЕШЕНИЕ: В современных версиях Google Identity SDK метод называется createFrom
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                onTokenReceived(idToken) // Отдаем чистый токен строкой в ScreenModel
              } catch (e: Exception) {
                println("[$tag]: Помилка разбору токена Google: ${e.message}")
              }
            }
          }
        )
      }
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = !isLoading && !isChoosingAccount,
    shape = RoundedCornerShape(12.dp)
  ) {
    if (isLoading || isChoosingAccount) {
      CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary
      )
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Image(
          painter = painterResource(Res.drawable.ic_google_logo),
          contentDescription = "Google logo",
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "Увійти через Google", // Временно строка для кроссплатформенности
          style = MaterialTheme.typography.titleMedium
        )
      }
    }
  }
}

private suspend fun launchCredManButtonUI(
  context: Context,
  onFinished: () -> Unit,
  onRequestResult: (Credential) -> Unit
) {
  try {
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(false)
      .setServerClientId("googleusercontent.com")
      .setAutoSelectEnabled(false)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    val result = CredentialManager.create(context).getCredential(request = request, context = context)
    onRequestResult(result.credential)
  } catch (e: GetCredentialException) {
    when (e) {
      is GetCredentialCancellationException -> println("[$tag]: Пользователь отменил выбор аккаунта")
      is NoCredentialException -> println("[$tag]: Аккаунты не найдены")
      else -> println("[$tag.Error]: ${e.message}")
    }
  } catch (e: Exception) {
    println("[$tag.Critical]: ${e.message}")
  } finally {
    onFinished()
  }
}
