package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

// Временная КМР-заглушка сущности задолженности для корректной сборки чатов
data class TotalServiceDebt(val name: String, val detail: Any, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, val debt: Double)

/**
 * [RootNavGraph] — Декларативная КМР стейт-машина холодного старта, соглашений и пуш-распределения.
 * ИСПРАВЛЕНО: Полное вытеснение Android NavHost, выравнивание ID под сквозной Long стандарт YkisMobKMP.
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
      val startScreen = remember(currentStartState) {
        when (currentStartState) {
          AppStartState.TermsAndConditions -> TermsAndConditionScreen
          AppStartState.SignIn -> SignInScreen()
          AppStartState.AddApartment -> AddApartmentScreen
          AppStartState.InfoApartment, AppStartState.UserList -> {
            MainApartmentScreen(contentType, navigationType)
          }
          AppStartState.Loading -> null
        }
      }

      if (startScreen == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(strokeWidth = 3.dp)
        }
      } else {
        Navigator(screen = startScreen) { navigator ->
          SlideTransition(navigator)

          // Сквозной подхват и накат внутренних пуш-уведомлений ЖЭК / ОСМД г. Южного
          LaunchedEffect(pendingChatId) {
            pendingChatId?.let { id ->
              println("[$className.LaunchedEffect]: ПУШ СИГНАЛ -> Відкриття чату: $id")

              // РЕШЕНИЕ: Вызываем легитимное КМР-имя класса из реестра ScreensRegistry.kt!
              navigator.push(ChatScreenDest(chatId = id))

              chatScreenModel.setPendingPushChatId(null)
            }
          }

          // Сквозная КМР обработка DeepLink при холодном старте приложения
          // Внутри RootNavGraph.kt в блоке LaunchedEffect(pendingChatId)

          LaunchedEffect(pendingChatId) {
            pendingChatId?.let { id ->
              println("[$className.LaunchedEffect]: ПУШ СИГНАЛ -> Відкриття чату: $id")

              // РЕШЕНИЕ: Вызываем легитимное КМР-имя класса из реестра ScreensRegistry.kt!
              navigator.push(ChatScreenDest(chatId = id))

              chatScreenModel.setPendingPushChatId(null)
            }
          }

// И аналогично чуть ниже в блоке LaunchedEffect(baseUIState.userRole, initialChatId):
          LaunchedEffect(baseUIState.userRole, initialChatId) {
            if (baseUIState.userRole != UserRole.Unknown && !initialChatId.isNullOrEmpty()) {
              println("[$className.LaunchedEffect]: DEEP_LINK_NAV -> Підхват пуша при старті: $initialChatId")
              val parts = initialChatId.split("_")
              if (parts.size >= 3) {
                val addrId = parts[parts.size - 2].toLongOrNull() ?: 0L
                val targetUid = parts.last()

                if (addrId != 0L) {
                  apartmentScreenModel.setAddressId(addrId)
                  chatScreenModel.selectUserByUid(targetUid)
                  delay(400)

                  // РЕШЕНИЕ: Заменяем и для холодного старта пушей!
                  navigator.push(ChatScreenDest(chatId = initialChatId))

                  println("[$className.LaunchedEffect]: Перехід по пушу виконано")
                }
              }
            }
          }

        }
      }
    }
  }
}



