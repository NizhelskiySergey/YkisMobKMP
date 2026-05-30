package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.InfoApartmentScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenStateful
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import com.ykis.ykismobkmp.ui.screens.ledger.MainServiceScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import com.ykis.ykismobkmp.ui.screens.meter.MeterListScreen
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    // Достаем НАСТОЯЩИЙ, ЕДИНСТВЕННЫЙ навигатор верхнего уровня из RootNavGraph
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

    val baseUIState by apartmentScreenModel.apartmentUiState.collectAsState()
    val userList by chatScreenModel.userList.collectAsState()

    var activeSubModule by rememberSaveable {
      mutableStateOf(
        when {
          baseUIState.addressId == 0L || baseUIState.apartments.isEmpty() -> "AddApartmentScreen"
          baseUIState.userRole == UserRole.StandardUser -> "InfoApartmentScreen"
          else -> "UserListScreen"
        }
      )
    }

    // Реактивный переключатель подмодуля, когда СУБД завершила холодный старт
    LaunchedEffect(baseUIState.mainLoading, baseUIState.addressId) {
      if (!baseUIState.mainLoading && baseUIState.addressId != 0L) {
        activeSubModule = when {
          baseUIState.userRole == UserRole.StandardUser -> "InfoApartmentScreen"
          else -> "UserListScreen"
        }
      }
    }

    // Слушатель роли для администраторов коммунальных служб Южного
    LaunchedEffect(baseUIState.userRole, baseUIState.osbbId) {
      val role = baseUIState.userRole
      if (role != UserRole.StandardUser && role != UserRole.Unknown) {
        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> 9999L
          UserRole.YtkeUser -> 9998L
          UserRole.TboUser -> 9997L
          else -> baseUIState.osbbId
        }
        chatScreenModel.trackUserIdentifiersWithRole(role, effectiveOsbbId.toInt())
      }
    }
    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      println("[YkisLogKMP.$className.finalizeApartmentSelection]: Зміна о/р квартири на Long ID: ${id}L")
      activeSubModule = "InfoApartmentScreen"
      apartmentScreenModel.setAddressId(id)
      coroutineScope.launch {
        if (drawerState.isOpen) {
          println("[YkisLogKMP.$className.finalizeApartmentSelection]: Закриття бокової шторки Drawer...")
          drawerState.close()
        }
        println("[YkisLogKMP.$className.finalizeApartmentSelection]: Кадр успішно синхронізовано з о/р: ${id}L")
      }
    }
    @Composable
    fun RenderSubContent() {
      Crossfade(targetState = activeSubModule, label = "SubModuleVoyagerFade") { route ->
        when (route) {
          "InfoApartmentScreen" -> {
            InfoApartmentScreen(
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } }
            ).Content()
          }

          "UserListScreen" -> {
            UserListScreen(
              userList = userList,
              navigationType = navigationType,
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              onUserClicked = { selectedItem ->
                apartmentScreenModel.setAddressId(selectedItem.addressId)
                activeSubModule = "InfoApartmentScreen"
              }
            ).Content()
          }
          "AddApartmentScreen" -> {
            AddApartmentScreen(
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              closeContentDetail = {
                println("[YkisLogKMP.MainApartmentScreen.AddApartmentScreen]: Успішне закриття форми прив'язки. Повернення на анкету БТІ.")
                activeSubModule = "InfoApartmentScreen"
              }
            ).Content()
          }
          "service_selector" -> {
            MainMeterScreen(
              onDrawerClick = { coroutineScope.launch { drawerState.open() } }
            ).Content()
          }

          "finance_selector" -> {
            MainServiceScreen(
              baseUIState = baseUIState,
              navigationType = navigationType,
              onDrawerClick = { coroutineScope.launch { drawerState.open() } }
            ).Content()
          }


//          "ChatScreenStateful" -> {
//            ChatScreenStateful(
//              screenModel = chatScreenModel,
//              baseUIState = baseUIState,
//              navigationType = navigationType,
//              onBackClick = { activeSubModule = "UserListScreen" }
//            ).Content()
//          }

          "SettingsScreenDest" -> {
            rememberSaveable(globalNavigator.lastItem) {
              SettingsScreen(
                onDrawerClick = { coroutineScope.launch { drawerState.open() } }
              )
            }.Content()
          }


          else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("Модуль ЖКХ")
            }
          }
        }
      }
    }



    // --- АДАПТИВНАЯ МАТРИЦА СБОРКИ ИНТЕРФЕЙСА (Смартфон против Планшета) ---
    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            navigator = globalNavigator,
            onMenuClick = { coroutineScope.launch { drawerState.close() } },
            navigateToApartment = finalizeApartmentSelection,
            isApartmentsEmpty = baseUIState.addressId == 0L
          )
        }
      ) {
        Scaffold(
          bottomBar = {
            val showBottomBar = baseUIState.addressId != 0L || baseUIState.userRole != UserRole.StandardUser
            if (showBottomBar) {
              // ИСПРАВЛЕНО НАМЕРТВО: Передаем нативный глобальный навигатор Voyager в нижний бар
              BottomNavigationBar(
                navigator = globalNavigator,
                baseUIState = baseUIState
              )
            }
          }
        ) { paddingValues ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
          ) {
            RenderSubContent()
          }
        }
      }
    } else {
      // 🖥️ ПРЕСЕТ ПЛАНШЕТА / DESKTOP: Стаціонарний бічний рельс Rail
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
          ApartmentNavigationRail(
            baseUIState = baseUIState,
            navigator = globalNavigator,
            activeSubModule = activeSubModule,
            onSubModuleChange = { newModule -> activeSubModule = newModule },

            isRailExpanded = isRailExpanded,
            onMenuClick = onMenuClick,
            navigateToApartment = finalizeApartmentSelection,
            railWidth = railWidth,
            isApartmentsEmpty = baseUIState.addressId == 0L
          )


          VerticalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
          )

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            RenderSubContent()
          }
        }
      }
    }
  }
}



