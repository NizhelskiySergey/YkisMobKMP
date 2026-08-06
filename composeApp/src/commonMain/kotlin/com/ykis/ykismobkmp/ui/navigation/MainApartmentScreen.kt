package com.ykis.ykismobkmp.ui.navigation

import com.ykis.ykismobkmp.core.Constants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.InfoApartmentScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ServiceSelectorScreen
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.MainServiceScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementListScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val className = "MainApartmentScreen"

/**
 * [MainApartmentScreen] — Кроссплатформенный каркас навигации (Shell).
 * УНИФИЦИРОВАНО: Логика переключения между Info и Add Apartment едина для всех ОС.
 */
class MainApartmentScreen(
  private val contentType: ContentType,
  private val navigationType: NavigationType
) : Screen {

  @Composable
  override fun Content() {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val globalNavigator = LocalNavigator.currentOrThrow

    var isRailExpanded by rememberSaveable {
      mutableStateOf(navigationType != NavigationType.BOTTOM_NAVIGATION)
    }

    val onMenuClick: () -> Unit = { isRailExpanded = !isRailExpanded }

    val railWidth by animateDpAsState(
      targetValue = if (isRailExpanded) 280.dp else 80.dp,
      animationSpec = tween(400),
      label = "RailWidth"
    )

    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val chatScreenModel = koinInject<ChatScreenModel>()
    val announcementModel = koinInject<com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel>()
    val meterScreenModel = koinInject<MeterScreenModel>()
    val ledgerScreenModel = koinInject<LedgerScreenModel>()

    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    val meterUIState by meterScreenModel.uiState.collectAsState()
    val ledgerUIState by ledgerScreenModel.uiState.collectAsState()

    // 1. Мониторинг объявлений (общий для всех)
    LaunchedEffect(baseUIState.osbbId) {
      if (baseUIState.userRole != UserRole.Unknown) {
        announcementModel.observeAnnouncements(baseUIState.osbbId)
      }
    }

    // 2. Определение активного модуля на старте
    var activeSubModule by rememberSaveable {
      mutableStateOf(
        when {
          baseUIState.userRole == UserRole.StandardUser -> {
            if (baseUIState.addressId == 0L || baseUIState.apartments.isEmpty()) "AddApartmentScreen" else "InfoApartmentScreen"
          }
          baseUIState.userRole != UserRole.Unknown -> "chat_user_list" 
          else -> "AddApartmentScreen"
        }
      )
    }

    var isInitialBoot by rememberSaveable { mutableStateOf(true) }

    // 3. УНИФИЦИРОВАННАЯ НАВИГАЦИЯ (LaunchedEffect)
    LaunchedEffect(baseUIState.addressId, baseUIState.userRole, baseUIState.osbbId, baseUIState.mainLoading, baseUIState.apartments) {
      val role = baseUIState.userRole
      val addressId = baseUIState.addressId
      val hasApartments = baseUIState.apartments.isNotEmpty()

      if (baseUIState.mainLoading) return@LaunchedEffect

      if (isInitialBoot) {
        activeSubModule = when {
          role == UserRole.StandardUser -> if (addressId != 0L || hasApartments) "InfoApartmentScreen" else "AddApartmentScreen"
          role != UserRole.Unknown -> "chat_user_list"
          else -> "AddApartmentScreen"
        }
        isInitialBoot = false
        println("[YkisLogKMP.$className]: Boot complete -> $activeSubModule")
        return@LaunchedEffect
      }

      // Динамическое переключение (например, после привязки счета)
      if (activeSubModule == "AddApartmentScreen") {
          if (role == UserRole.StandardUser && (addressId != 0L || hasApartments)) {
            activeSubModule = "InfoApartmentScreen"
          } else if (role != UserRole.StandardUser && role != UserRole.Unknown && baseUIState.osbbId != 0L) {
            activeSubModule = "chat_user_list"
          }
      } else if (activeSubModule == "InfoApartmentScreen") {
           if (role == UserRole.StandardUser && addressId == 0L && !hasApartments) {
             activeSubModule = "AddApartmentScreen"
           }
      }
    }

    // 4. Настройка идентификаторов для чата
    LaunchedEffect(baseUIState.userRole, baseUIState.osbbId, baseUIState.apartments) {
      val role = baseUIState.userRole
      val osbbId = baseUIState.osbbId
      
      if (role != UserRole.Unknown) {
        if (role == UserRole.StandardUser) {
          if (baseUIState.apartments.isNotEmpty()) chatScreenModel.trackUserIdentifiersWithRole(role, 0L, baseUIState.apartments)
          return@LaunchedEffect
        }
        
        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> Constants.WATER_SERVICE_ID
          UserRole.YtkeUser      -> Constants.WARM_SERVICE_ID
          UserRole.TboUser       -> Constants.GARBAGE_SERVICE_ID
          else                   -> osbbId
        }

        val adminPrefix = when (role) {
          UserRole.VodokanalUser -> "WATER_SERVICE"
          UserRole.YtkeUser      -> "WARM_SERVICE"
          UserRole.TboUser       -> "GARBAGE_SERVICE"
          UserRole.OsbbUser      -> "OSBB"
          UserRole.StandardUser, UserRole.Unknown -> null
        }
        
        adminPrefix?.let { chatScreenModel.onServiceSelectedForResident(it) }
        chatScreenModel.trackUserIdentifiersWithRole(role, effectiveOsbbId)
      }
    }

    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      activeSubModule = "InfoApartmentScreen"
      apartmentScreenModel.setAddressId(id)
      coroutineScope.launch { if (drawerState.isOpen) drawerState.close() }
    }

    @Composable
    fun RenderSubContent() {
      Crossfade(targetState = activeSubModule, label = "SubModuleVoyagerFade") { route ->
        when (route) {
          "InfoApartmentScreen" -> InfoApartmentScreen(onDrawerClicked = { coroutineScope.launch { drawerState.open() } }).Content()
          "AddApartmentScreen" -> AddApartmentScreen(onDrawerClicked = { coroutineScope.launch { drawerState.open() } }, closeContentDetail = { activeSubModule = "InfoApartmentScreen" }).Content()
          "service_selector" -> MainMeterScreen(onDrawerClick = { coroutineScope.launch { drawerState.open() } }).Content()
          "finance_selector" -> MainServiceScreen(baseUIState = baseUIState, onDrawerClick = { coroutineScope.launch { drawerState.open() } }).Content()
          "chat_selector" -> {
            if (baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.Unknown) activeSubModule = "chat_user_list"
            ServiceSelectorScreen(baseUIState = baseUIState, onDrawerClicked = { coroutineScope.launch { drawerState.open() } }, onServiceClick = { activeSubModule = "chat_user_list" }).Content()
          }
          "chat_user_list" -> UserListScreen(
              onDrawerClicked = { if (baseUIState.userRole == UserRole.StandardUser) activeSubModule = "chat_selector" else coroutineScope.launch { drawerState.open() } },
              onUserClicked = { user ->
                val role = baseUIState.userRole
                if (role == UserRole.OsbbUser || role == UserRole.StandardUser) {
                    apartmentScreenModel.setAddressId(user.addressId)
                }
                if (role == UserRole.StandardUser) {
                    chatScreenModel.onServiceSelectedForResident(chatScreenModel.selectedServicePrefix.value)
                }
                val currentOsbbId = apartmentScreenModel.uiState.value.osbbId
                chatScreenModel.openChatWithUser(user, role, currentOsbbId)
                activeSubModule = "chat_room_active"
              }
            ).Content()
          "chat_room_active" -> ChatScreen(onBackClick = { activeSubModule = "chat_user_list" }).Content()
          "announcements" -> AnnouncementListScreen(onDrawerClicked = { coroutineScope.launch { drawerState.open() } }).Content()
          "SettingsScreenDest" -> SettingsScreen(onDrawerClick = { coroutineScope.launch { drawerState.open() } }).Content()
          else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Завантаження...") }
        }
      }
    }

    val localFocusManager = LocalFocusManager.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    val showRail = navigationType == NavigationType.NAVIGATION_RAIL_COMPACT || 
                   navigationType == NavigationType.NAVIGATION_RAIL_EXPANDED || 
                   navigationType == NavigationType.PERMANENT_NAVIGATION_DRAWER

    val isChatRoomActive = activeSubModule == "chat_room_active" && contentType == ContentType.SINGLE_PANE
    val isChatUserList = activeSubModule == "chat_user_list" && baseUIState.userRole == UserRole.StandardUser && contentType == ContentType.SINGLE_PANE
    val isMeterDetail = activeSubModule == "service_selector" && meterUIState.showDetail && contentType == ContentType.SINGLE_PANE
    val isLedgerDetail = activeSubModule == "finance_selector" && ledgerUIState.showDetail && contentType == ContentType.SINGLE_PANE
    val isAnyDetailActive = isChatRoomActive || isChatUserList || isMeterDetail || isLedgerDetail

    Row(modifier = Modifier.fillMaxSize()) {
      if (showRail) {
        ApartmentNavigationRail(
          baseUIState = baseUIState,
          navigator = globalNavigator,
          activeSubModule = activeSubModule,
          onSubModuleChange = { activeSubModule = it },
          isRailExpanded = isRailExpanded,
          onMenuClick = {
            apartmentScreenModel.onSearchQueryChanged("")
            onMenuClick()
          },
          navigateToApartment = finalizeApartmentSelection,
          railWidth = railWidth,
          isApartmentsEmpty = baseUIState.addressId == 0L
        )
        VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
      }

      ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !showRail && !isAnyDetailActive,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            navigator = globalNavigator,
            activeSubModule = activeSubModule,
            onSubModuleChange = { activeSubModule = it },
            onMenuClick = {
              localKeyboardController?.hide()
              localFocusManager.clearFocus()
              coroutineScope.launch { drawerState.close() }
            },
            navigateToApartment = finalizeApartmentSelection,
            isApartmentsEmpty = baseUIState.addressId == 0L
          )
        }
      ) {
        Scaffold(
          contentWindowInsets = WindowInsets.statusBars,
          bottomBar = {
            val showBottomBar = (baseUIState.addressId != 0L || baseUIState.userRole != UserRole.StandardUser) && 
                                !isAnyDetailActive
            
            if (showBottomBar) {
               BottomNavigationBar(
                 baseUIState = baseUIState, 
                 activeSubModule = activeSubModule, 
                 onSubModuleChange = { activeSubModule = it }
               )
            }
          }
        ) { paddingValues ->
          Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding(), bottom = if (isAnyDetailActive) 0.dp else paddingValues.calculateBottomPadding())) {
            RenderSubContent() 
          }
        }
      }
    }
  }
}
