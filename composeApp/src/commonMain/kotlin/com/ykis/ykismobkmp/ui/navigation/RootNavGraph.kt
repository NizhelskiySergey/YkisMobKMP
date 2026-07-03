package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.TermsAndConditionScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val className = "RootNavGraph"

val LocalContentType = compositionLocalOf<ContentType> { ContentType.SINGLE_PANE }
val LocalNavigationType = compositionLocalOf<NavigationType> { NavigationType.BOTTOM_NAVIGATION }

@OptIn(InternalVoyagerApi::class)
@Composable
fun RootNavGraph(
  appState: YkisPamAppState,
  contentType: ContentType,
  navigationType: NavigationType
) {
  val appStartModel = koinInject<AppScreenModel>()
  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()
  apartmentScreenModel.firebaseService

  val currentStartState by appStartModel.startState.collectAsState()
  val baseUIState by apartmentScreenModel.uiState.collectAsState()
  val pendingChatId by chatScreenModel.pendingPushChatId.collectAsState()

  CompositionLocalProvider(
    LocalContentType provides contentType,
    LocalNavigationType provides navigationType
  ) {
    LaunchedEffect(pendingChatId) {
      if (pendingChatId != null) {
        snapshotFlow { currentStartState }.first { it != AppStartState.Loading }
        snapshotFlow { baseUIState }.first { it.userRole != UserRole.Unknown && !it.mainLoading }
        
        val addrId = pendingChatId!!.split("_").lastOrNull()?.toLongOrNull() ?: 0L
        if (addrId != 0L) {
          apartmentScreenModel.setAddressId(addrId)
          snapshotFlow { baseUIState }.first { it.addressId == addrId }
          chatScreenModel.selectUserByAddressId(addrId)
        }
      }
    }

    Scaffold(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) { data -> Snackbar(data) } }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        if (currentStartState == AppStartState.Loading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
          }
        } else {
          val stableStartScreen = remember {
            when (currentStartState) {
              AppStartState.TermsAndConditions -> TermsAndConditionScreen(appStartModel.cachedTermsText)
              AppStartState.SignIn -> SignInScreen
              AppStartState.VerifyEmail -> VerifyEmailScreenDest
              else -> MainApartmentScreen(contentType = contentType, navigationType = navigationType)
            }
          }

          Navigator(screen = stableStartScreen) { navigator ->
            SlideTransition(navigator)

            LaunchedEffect(currentStartState) {
              val currentRoute = navigator.lastItem
              if (currentRoute is ChatScreenDest || pendingChatId != null) return@LaunchedEffect

              when (currentStartState) {
                AppStartState.TermsAndConditions -> {
                  if (currentRoute !is TermsAndConditionScreen) navigator.replaceAll(TermsAndConditionScreen(appStartModel.cachedTermsText))
                }
                AppStartState.SignIn -> {
                  if (currentRoute != SignInScreen && currentRoute != SignUpScreenDest) navigator.replaceAll(SignInScreen)
                }
                AppStartState.VerifyEmail -> {
                  if (currentRoute != VerifyEmailScreenDest) navigator.replaceAll(VerifyEmailScreenDest)
                }
                AppStartState.AddApartment, AppStartState.InfoApartment, AppStartState.UserList -> {
                  if (currentRoute !is MainApartmentScreen) {
                    navigator.replaceAll(MainApartmentScreen(contentType = contentType, navigationType = navigationType))
                  }
                }
                else -> {}
              }
            }

            LaunchedEffect(pendingChatId) {
              if (pendingChatId != null) {
                snapshotFlow { baseUIState }.first { it.userRole != UserRole.Unknown && !it.mainLoading }
                val addrId = pendingChatId!!.split("_").lastOrNull()?.toLongOrNull() ?: 0L
                if (addrId != 0L) {
                   snapshotFlow { baseUIState }.first { it.addressId == addrId }
                   delay(300)
                   // Перевіряємо чи ми вже не в чаті
                   if (navigator.lastItem !is ChatScreenDest) {
                       navigator.push(ChatScreenDest(chatId = pendingChatId))
                   }
                   chatScreenModel.setPendingPushChatId(null)
                }
              }
            }
          }
        }
      }
    }
  }
}
