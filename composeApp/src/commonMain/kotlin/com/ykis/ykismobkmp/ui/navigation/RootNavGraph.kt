package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.ykis.ykismobkmp.getPlatform
import com.ykis.ykismobkmp.restartApp
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.TermsAndConditionScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.domain.entity.AppUpdateConfig
import com.ykis.ykismobkmp.domain.services.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RootNavGraph(
  contentType: ContentType,
  navigationType: NavigationType,
  appStartModel: AppScreenModel = koinInject()
) {
    val appState = rememberYkisPamAppState()
    val currentStartState by appStartModel.startState.collectAsState()
    val updateConfig by appStartModel.updateConfig.collectAsState()
    
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val chatScreenModel = koinInject<ChatScreenModel>()
    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    val pendingChatId by chatScreenModel.pendingPushChatId.collectAsState()

    LaunchedEffect(Unit) {
      appStartModel.evaluateStartDestination()
    }

    LaunchedEffect(currentStartState, pendingChatId) {
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
      Column(
          modifier = Modifier
              .fillMaxSize()
              .statusBarsPadding() // Тепер відступ статус-бару є ЗАВЖДИ
              .padding(bottom = paddingValues.calculateBottomPadding())
      ) {
        // 1. БАННЕР ОБНОВЛЕНИЯ
        AnimatedVisibility(
            visible = updateConfig != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            AppUpdateBanner(
                config = updateConfig!!,
                onDismiss = { appStartModel.dismissUpdateBanner() }
            )
        }

        // 2. ОСНОВНОЙ КОНТЕНТ (Navigator)
        Box(modifier = Modifier
            .weight(1f)
            .consumeWindowInsets(WindowInsets.statusBars)
        ) {
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

@Composable
fun AppUpdateBanner(
    config: AppUpdateConfig,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val platform = getPlatform().name.lowercase()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFF4CAF50),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
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
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Доступна нова версія",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
