package com.ykis.ykismobkmp.ui.screens.apartment

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.ui.ContentType
import com.ykis.ykismobkmp.ui.NavigationType
import com.ykis.ykismobkmp.ui.UserRole
import com.ykis.ykismobkmp.ui.navigation.ApartmentNavigationRail
import com.ykis.ykismobkmp.ui.navigation.BottomNavigationBar
import com.ykis.ykismobkmp.ui.screens.apartment.tabs.InfoTab
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentViewModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatTab
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * [MainApartmentScreen] — главный адаптивный экран приложения.
 * В Voyager реализуется как Screen, управляющий вкладками (Tabs).
 */
class MainApartmentScreen(
  private val contentType: ContentType,
  private val navigationType: NavigationType
) : Screen {

  @Composable
  override fun Content() {
    val className = "MainApartmentScreen"
    val navigator = LocalNavigator.currentOrThrow
    val apartmentViewModel = koinInject<ApartmentViewModel>()
    val firebaseService = koinInject<FirebaseService>()

    val baseUIState by apartmentViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // 1. КОНТРОЛЬ ГОТОВНОСТИ (Золотой фонд)
    val isDataReady = remember(baseUIState.uid, baseUIState.mainLoading) {
      baseUIState.uid != null && !baseUIState.mainLoading
    }

    // 2. ОПРЕДЕЛЕНИЕ СТАРТОВОЙ ВКЛАДКИ
    val startTab = remember(baseUIState.userRole, baseUIState.addressId, isDataReady) {
      if (!isDataReady) return@remember InfoTab // Временная заглушка

      when (baseUIState.userRole) {
        UserRole.StandardUser -> if (baseUIState.apartments.isEmpty()) InfoTab else InfoTab
        else -> ChatTab // Админы начинают с чатов
      }
    }

    if (!isDataReady) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
      return
    }

    // 3. ТАБ-НАВИГАЦИЯ (Замена внутреннего NavHost)
    TabNavigator(startTab) { tabNavigator ->
      if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
        // ВЕРСИЯ ДЛЯ ТЕЛЕФОНА (Android стиль)
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ApartmentDrawerContent(
              baseUIState = baseUIState,
              onClose = { coroutineScope.launch { drawerState.close() } }
            )
          }
        ) {
          Scaffold(
            bottomBar = {
              BottomNavigationBar(
                currentTab = tabNavigator.current,
                onTabClick = { tabNavigator.current = it }
              )
            }
          ) { padding ->
            Box(Modifier.padding(padding)) { CurrentTab() }
          }
        }
      } else {
        // ВЕРСИЯ ДЛЯ ПК/ПЛАНШЕТА (Mac Desktop стиль)
        Row(Modifier.fillMaxSize()) {
          ApartmentNavigationRail(
            currentTab = tabNavigator.current,
            onTabClick = { tabNavigator.current = it },
            baseUIState = baseUIState
          )
          VerticalDivider(thickness = 0.5.dp)
          Box(Modifier.weight(1f)) { CurrentTab() }
        }
      }
    }
  }
}
