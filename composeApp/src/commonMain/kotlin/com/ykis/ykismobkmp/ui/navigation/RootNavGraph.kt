package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ykis.ykismobkmp.*

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
  val updateConfig by appStartModel.updateConfig.collectAsState()
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
      Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        // БАННЕР ОБНОВЛЕНИЯ
        AnimatedVisibility(
            visible = updateConfig != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            updateConfig?.let { cfg ->
                AppUpdateBanner(
                    config = cfg,
                    onDismiss = { appStartModel.dismissUpdateBanner() }
                )
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
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
}

@Composable
fun AppUpdateBanner(
    config: com.ykis.ykismobkmp.domain.entity.AppUpdateConfig,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val platform = getPlatform().name.lowercase()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFF4CAF50), // Ярко-зеленый цвет (Material Green 500)
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    println("[YkisLogKMP.UpdateBanner]: Клик! Платформа: $platform")
                    val url = when {
                        platform.contains("android") -> config.androidUrl
                        platform.contains("ios") || platform.contains("iphone") || platform.contains("ipad") || platform.contains("apple") -> config.iosUrl
                        platform.contains("web") -> config.webUrl
                        else -> config.webUrl
                    }
                    
                    if (url.isNotBlank() && url != "reload") {
                        try { 
                            uriHandler.openUri(url) 
                        } catch (_: Exception) {
                            if (config.webUrl.isNotBlank() && config.webUrl != "reload") {
                                try { uriHandler.openUri(config.webUrl) } catch(_: Exception) {}
                            }
                        }
                    } else if (url == "reload" && platform.contains("web")) {
                        restartApp()
                    }
                }
                .padding(vertical = 8.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Color.White, // Белая иконка на зеленом фоне
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Доступна нова версія",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White, // Белый текст на зеленом фоне
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
