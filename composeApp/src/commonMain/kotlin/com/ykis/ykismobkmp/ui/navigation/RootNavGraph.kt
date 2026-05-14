package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

// Импорты экранов приложения (каждый реализует интерфейс Screen)
import com.ykis.ykismobkmp.ui.screens.auth.TermsScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen
import com.ykis.ykismobkmp.ui.screens.apartment.MainApartmentScreen

// Импорты конфигураций и сервисов
import com.ykis.ykismobkmp.ui.NavigationType
import com.ykis.ykismobkmp.ui.ContentType
import com.ykis.ykismobkmp.ui.YkisPamAppState
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.ui.UserRole

// НАШИ ИСПРАВЛЕННЫЕ СТАНДАРТЫ (Ыскуут / ScreenModel):
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel

private const val className = "RootNavGraph"

/**
 * [RootNavGraph] — Корневой навигационный граф.
 * Инжектирует исключительно Ыскуут (ScreenModel) для управления логикой ЖКХ-панели на Mac и Android.
 */
@Composable
fun RootNavGraph(
  appState: YkisPamAppState,
  navigationType: NavigationType,
  contentType: ContentType,
  initialChatId: String? = null
) {
  val firebaseService = koinInject<FirebaseService>()

  // ЗАФИКСИРОВАНО: Извлекаем из Koin строго наши ScreenModel (Ыскуут)
  val chatScreenModel = koinInject<ChatScreenModel>()
  val apartmentScreenModel = koinInject<ApartmentScreenModel>()

  // Подписка на реактивные состояния наших Ыскуут (ScreenModel)
  val baseUIState by apartmentScreenModel.uiState.collectAsState()
  val pendingChatId by chatScreenModel.pendingPushChatId.collectAsState()

  val isAgreed = remember { firebaseService.isUserAgreed() }
  val currentUser = remember { firebaseService.currentUser }

  LaunchedEffect(isAgreed, currentUser) {
    println("[$className]: Auth State -> Agreed: $isAgreed, UserUID: ${currentUser?.uid?.takeLast(5)}")
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    snackbarHost = {
      SnackbarHost(hostState = appState.snackbarHostState) { data ->
        Snackbar(data)
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Позиционный вызов Navigator без "screen =", типы Screen приводятся на лету
      Navigator(
        when {
          !isAgreed -> TermsScreen() as Screen
          currentUser == null -> SignUpScreen() as Screen
          else -> MainApartmentScreen(contentType, navigationType) as Screen
        }
      ) { navigator ->

        SlideTransition(navigator)

        // Обработка внутренних триггеров переходов из ChatScreenModel (Ыскуут)
        LaunchedEffect(pendingChatId) {
          pendingChatId?.let { id ->
            println("[$className.LaunchedEffect]: PUSH_NAV -> $id")
            navigator.push(ChatDetailScreen(chatId = id))
            chatScreenModel.setPendingPushChatId(null)
          }
        }

        // Обработка DeepLink на основе данных ApartmentScreenModel (Ыскуут)
        LaunchedEffect(baseUIState.userRole, initialChatId) {
          if (baseUIState.userRole != UserRole.Unknown && !initialChatId.isNullOrEmpty()) {
            println("[$className.LaunchedEffect]: DEEP_LINK_NAV -> $initialChatId")

            val parts = initialChatId.split("_")
            if (parts.size >= 3) {
              val addrId = parts[parts.size - 2].toLongOrNull() ?: 0L
              val targetUid = parts.last()

              if (addrId != 0L) {
                // Записываем сквозной Long ID в стейт Ыскуут
                apartmentScreenModel.setAddressId(addrId)
                chatScreenModel.selectUserByUid(targetUid)

                delay(400)
                navigator.push(ChatDetailScreen(chatId = initialChatId))
                println("[$className.LaunchedEffect]: DeepLink применен через ScreenModel")
              }
            }
          }
        }
      }
    }
  }
}
