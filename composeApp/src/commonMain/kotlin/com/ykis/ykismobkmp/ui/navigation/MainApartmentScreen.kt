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
import com.ykis.ykismobkmp.ui.screens.ledger.MainServiceScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementListScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val className = "MainApartmentScreen"

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

    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    LaunchedEffect(baseUIState.osbbId) {
      if (baseUIState.userRole != UserRole.Unknown) {
        println("[YkisLogKMP.$className]: Фоновий запуск моніторингу оголошень для OSBB ID: ${baseUIState.osbbId}")
        announcementModel.observeAnnouncements(baseUIState.osbbId)
      }
    }

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

    LaunchedEffect(baseUIState.addressId, baseUIState.userRole, baseUIState.osbbId, baseUIState.mainLoading) {
      val role = baseUIState.userRole
      val addressId = baseUIState.addressId
      val osbbId = baseUIState.osbbId

      if (isInitialBoot && !baseUIState.mainLoading) {
        activeSubModule = when {
          role == UserRole.StandardUser -> if (addressId != 0L) "InfoApartmentScreen" else "AddApartmentScreen"
          role != UserRole.Unknown -> "chat_user_list"
          else -> "AddApartmentScreen"
        }
        isInitialBoot = false
        println("[YkisLogKMP.$className.Navigation]: Холодний старт завершено. Екран: $activeSubModule")
        return@LaunchedEffect
      }

      if (!isInitialBoot) {
        if (activeSubModule == "AddApartmentScreen") {
          if (role == UserRole.StandardUser && addressId != 0L) {
            println("[YkisLogKMP.$className.Navigation]: Рахунок прив'язано. Перехід на InfoApartmentScreen")
            activeSubModule = "InfoApartmentScreen"
          } else if (role != UserRole.StandardUser && role != UserRole.Unknown && osbbId != 0L) {
            println("[YkisLogKMP.$className.Navigation]: Адмін авторизований. Перехід на список чатів")
            activeSubModule = "chat_user_list"
          }
        } else if (activeSubModule == "InfoApartmentScreen") {
           if (role == UserRole.StandardUser && addressId == 0L) {
             println("[YkisLogKMP.$className.Navigation]: Рахунків немає. Редирект на AddApartmentScreen")
             activeSubModule = "AddApartmentScreen"
           }
        }
      }
    }

    LaunchedEffect(baseUIState.userRole, baseUIState.osbbId, baseUIState.apartments) {
      val role = baseUIState.userRole
      val osbbId = baseUIState.osbbId ?: 0L
      
      if (role != UserRole.Unknown) {
        if (role == UserRole.StandardUser) {
          if (baseUIState.apartments.isNotEmpty()) chatScreenModel.trackUserIdentifiersWithRole(role, 0L, baseUIState.apartments)
          return@LaunchedEffect
        }
        if (role == UserRole.OsbbUser && osbbId == 0L) return@LaunchedEffect

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
          else -> null
        }
        
        adminPrefix?.let { 
          println("[YkisLogKMP.$className]: [ADMIN_AUTO_PREFIX] Установка службы: $it")
          chatScreenModel.onServiceSelectedForResident(it) 
        }

        chatScreenModel.trackUserIdentifiersWithRole(role, effectiveOsbbId)
      }
    }

    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      println("[YkisLogKMP.$className.finalizeApartmentSelection]: Зміна о/р на $id")
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
                
                // ШАГ 1: ПЕРЕКЛЮЧАЕМ КОНТЕКСТ ТОЛЬКО ДЛЯ ЖИЛЬЦА И ОСББ
                // Для коммунальных служб (Водоканал и др.) мы НЕ вызываем setAddressId,
                // чтобы не ломать их текущую навигацию в Drawer.
                if (role == UserRole.OsbbUser || role == UserRole.StandardUser) {
                    apartmentScreenModel.setAddressId(user.addressId)
                }
                
                // ШАГ 2: ОБНОВЛЯЕМ ПРЕФИКС СЛУЖБЫ ДЛЯ ЖИЛЬЦА
                if (role == UserRole.StandardUser) {
                    chatScreenModel.onServiceSelectedForResident(chatScreenModel.selectedServicePrefix.value)
                }
                
                // ШАГ 3: ОТКРЫВАЕМ ЧАТ
                // Для коммунальных служб берем osbbId из стейта (там 9999, 9998 или 9997)
                val currentOsbbId = apartmentScreenModel.uiState.value.osbbId
                chatScreenModel.openChatWithUser(user, role, currentOsbbId)

                activeSubModule = "chat_room_active"
              }
            ).Content()
          "chat_room_active" -> ChatScreen(onBackClick = { activeSubModule = "chat_user_list" }).Content()
          "announcements" -> AnnouncementListScreen(onDrawerClicked = { coroutineScope.launch { drawerState.open() } }).Content()
          "SettingsScreenDest" -> SettingsScreen(onDrawerClick = { coroutineScope.launch { drawerState.open() } }).Content()
          else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Модуль ЖКХ") }
        }
      }
    }

    val localFocusManager = LocalFocusManager.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    val showRail = navigationType == NavigationType.NAVIGATION_RAIL_COMPACT || 
                   navigationType == NavigationType.NAVIGATION_RAIL_EXPANDED || 
                   navigationType == NavigationType.PERMANENT_NAVIGATION_DRAWER

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
        gesturesEnabled = !showRail, // Отключаем жесты дравера, если есть рейл
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
            val isChatRoomActive = activeSubModule == "chat_room_active"
            val showBottomBar = (baseUIState.addressId != 0L || baseUIState.userRole != UserRole.StandardUser) && !isChatRoomActive
            if (showBottomBar) {
               BottomNavigationBar(
                 navigator = globalNavigator, 
                 baseUIState = baseUIState, 
                 activeSubModule = activeSubModule, 
                 onSubModuleChange = { activeSubModule = it }
               )
            }
          }
        ) { paddingValues ->
          val isChatRoomActive = activeSubModule == "chat_room_active"
          Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding(), bottom = if (isChatRoomActive) 0.dp else paddingValues.calculateBottomPadding())) { 
            RenderSubContent() 
          }
        }
      }
    }
  }
}
