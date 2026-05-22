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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.auth.SignInScreen
import com.ykis.ykismobkmp.ui.screens.auth.TermsAndConditionScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val className = "RootNavGraph"

// ====================================================================
// --- ЦЕНТРАЛЬНЫЕ КМР-ПРОВАЙДЕРЫ КОНТЕКСТА ГЕОМЕТРИИ ОКНА (Top-Level) ---
// ====================================================================

val LocalContentType = compositionLocalOf<ContentType> {
  ContentType.SINGLE_PANE
}

val LocalNavigationType = compositionLocalOf<NavigationType> {
  NavigationType.BOTTOM_NAVIGATION
}

/**
 * [RootNavGraph] — Декларативная КМР стейт-машина холодного старта, соглашений и пуш-распределения.
 * ИСПРАВЛЕНО: Логирование переведено на стандарт YkisLogKMP, добавлены недостающие КМР импорты.
 */
@Composable
fun RootNavGraph(
  appState: YkisPamAppState,
  contentType: ContentType,
  navigationType: NavigationType,
  initialChatId: String? = null
) {
  val appStartModel = koinInject<AppScreenModel>()
  val currentStartState by appStartModel.startState.collectAsState()

  val chatScreenModel = koinInject<ChatScreenModel>()
  val apartmentScreenModel = koinInject<ApartmentScreenModel>()

  val baseUIState by apartmentScreenModel.uiState.collectAsState()
  val pendingChatId by chatScreenModel.pendingPushChatId.collectAsState()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) { data -> Snackbar(data) } }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

      // ШАГ 1: Мапим реактивный AppStartState в реальные КМР-объекты экранов Voyager
      val startScreen = remember(currentStartState, contentType, navigationType) {
        when (currentStartState) {
          AppStartState.TermsAndConditions -> TermsAndConditionScreen
          AppStartState.SignIn -> SignInScreen
          AppStartState.AddApartment -> AddApartmentScreen
          AppStartState.InfoApartment, AppStartState.UserList -> {
            MainApartmentScreen(
              contentType = contentType,
              navigationType = navigationType,
            )
          }
          AppStartState.Loading -> null
        }
      }

      if (startScreen == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(strokeWidth = 3.dp)
        }
      } else {
        // РЕШЕНИЕ: Упаковываем всю навигационную цепочку в глобальный CompositionLocalProvider!
        // Теперь любой вложенный экран сможет нативно прочитать contentType и navigationType через .current
        CompositionLocalProvider(
          LocalContentType provides contentType,
          LocalNavigationType provides navigationType
        ) {
          Navigator(screen = startScreen) { navigator ->
            SlideTransition(navigator)

            // Сквозной подхват и накат внутренних горячих пуш-уведомлений ЖЭК / ОСМД г. Южного
            LaunchedEffect(pendingChatId) {
              pendingChatId?.let { id ->
                println("[YkisLogKMP.$className.LaunchedEffect]: ПУШ СИГНАЛ -> Відкриття чату: $id")

                // Вызываем легитимное КМР-имя класса из реестра ScreensRegistry.kt
                navigator.push(ChatScreenDest(chatId = id))

                chatScreenModel.setPendingPushChatId(null)
              }
            }

            // Сквозная КМР обработка DeepLink при холодном старте приложения («Я в пути!»)
            LaunchedEffect(baseUIState.userRole, initialChatId) {
              if (baseUIState.userRole != UserRole.Unknown && !initialChatId.isNullOrEmpty()) {
                println("[YkisLogKMP.$className.LaunchedEffect]: DEEP_LINK_NAV -> Підхват пуша при старті: $initialChatId")
                val parts = initialChatId.split("_")
                if (parts.size >= 3) {
                  val addrId = parts[parts.size - 2].toLongOrNull() ?: 0L
                  val targetUid = parts.last()

                  if (addrId != 0L) {
                    // Вызываем оригинальное имя метода setAddressId со сквозным Long-типом под SQLDelight
                    apartmentScreenModel.setAddressId(addrId)
                    chatScreenModel.selectUserByUid(targetUid)
                    delay(400)

                    // Нативно пушаем окно сообщений в навигационный Voyager бэкстек
                    navigator.push(ChatScreenDest(chatId = initialChatId))

                    println("[YkisLogKMP.$className.LaunchedEffect]: Перехід по пушу виконано")
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




