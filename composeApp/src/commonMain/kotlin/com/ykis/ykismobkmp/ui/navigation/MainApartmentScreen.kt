package com.ykis.ykismobkmp.ui.navigation

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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val className = "RMainApartmentScreen"
/**
 * [MainApartmentScreen] — Главный адаптивный контейнер распределения интерфейса.
 * ИСПРАВЛЕНО: Внутренний NavHost вырезан, переключение вкладок БТИ/чатов переведено на нативный when.
 */
class MainApartmentScreen(
  private val contentType: ContentType,
  private val navigationType: NavigationType
) : Screen {

  @Composable
  override fun Content() {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val chatScreenModel = koinInject<ChatScreenModel>()

    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()
    val userList by chatScreenModel.userList.collectAsState()

    var currentScreenRoute by remember(baseUIState.userRole, baseUIState.addressId) {
      mutableStateOf(
        if (baseUIState.userRole == UserRole.StandardUser) {
          if (baseUIState.apartments.isEmpty()) "AddApartmentScreen" else "InfoApartmentScreen"
        } else {
          if (baseUIState.addressId != 0L) "InfoApartmentScreen" else "UserListScreen"
        }
      )
    }

    // КМР-функция переключения лицевых счетов БТИ расчетного центра
    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      println("[$className.finalizeApartmentSelection]: Смена о/р квартиры на Long ID: $id")
      apartmentScreenModel.setAddressId(id)
      coroutineScope.launch {
        if (drawerState.isOpen) drawerState.close()
        delay(200)
        currentScreenRoute = "InfoApartmentScreen"
      }
    }

    @Composable
    fun RenderActiveModule() {
      if (baseUIState.mainLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
      }

      // Внутри RootNavGraph.kt -> MainApartmentScreen -> RenderActiveModule()

      when (currentScreenRoute) {
        "UserListScreen" -> {
          // Мониторинг идентификаторов для админов коммунальных служб Южного
          LaunchedEffect(baseUIState.userRole, baseUIState.osbbId) {
            val role = baseUIState.userRole
            if (role != UserRole.StandardUser) {
              val effectiveOsbbId = when (role) {
                UserRole.VodokanalUser -> 9999L
                UserRole.YtkeUser -> 9998L
                UserRole.TboUser -> 9997L
                else -> baseUIState.osbbId
              }
              chatScreenModel.trackUserIdentifiersWithRole(role, effectiveOsbbId.toInt())
            }
          }

          // ИСПРАВЛЕНО: Аргументы передаются в конструктор класса экрана согласно KMP/Voyager стандартам!
          val userListScreenInstance = remember(userList, navigationType, baseUIState.userRole) {
            UserListScreen(
              userList = userList,
              navigationType = navigationType,
              onDrawerClicked = {
                coroutineScope.launch { drawerState.open() }
              },
              onUserClicked = { selectedItem ->
                if (baseUIState.userRole == UserRole.StandardUser) {
                  println("[$className.MainApartmentScreen]: Стандартний користувач обрав квартиру ID: ${selectedItem.addressId}")
                  // ИСПРАВЛЕНО: Вызываем легитимный КМР-метод смены квартиры
                  apartmentScreenModel.setAddressId(selectedItem.addressId)
                } else {
                  val osbbId = when (baseUIState.userRole) {
                    UserRole.VodokanalUser -> 9999L
                    UserRole.YtkeUser -> 9998L
                    UserRole.TboUser -> 9997L
                    else -> baseUIState.osbbId
                  }
                  println("[$className.MainApartmentScreen]: Адмін відкриває чат з UID: ${selectedItem.uid} для підприємства: $osbbId")
                  chatScreenModel.openChatWithUser(selectedItem, baseUIState.userRole, osbbId.toInt())
                }
                currentScreenRoute = "ChatScreenStateful"
              }
            )
          }

          // Нативно рендерим холст созданного Voyager-экрана на месте вызова
          userListScreenInstance.Content()
        }

        // ... остальные ветки ("AddApartmentScreen", "InfoApartmentScreen", "ChatScreenStateful") остаются без изменений ...


        "AddApartmentScreen" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Вікно додавання особового рахунку БТІ") }
        }
        "InfoApartmentScreen" -> {
          key(baseUIState.addressId) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Характеристики БТІ квартири ID: ${baseUIState.addressId}") }
          }
        }
        "service_selector" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Селектор комунальних служб (Водоканал / Тепломережа)") }
        }
        "ChatScreenStateful" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Екран активної чат-кімнати обговорення") }
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Модуль ЖКХ") }
      }
    }

    // МАТРИЦА СБОРОК ИНТЕРФЕЙСА (Смартфон против Mac Desktop)
    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            selectedDestination = currentScreenRoute,
            navigateToDestination = { dest ->
              coroutineScope.launch { drawerState.close(); currentScreenRoute = dest }
            },
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
              BottomNavigationBar(
                selectedDestination = currentScreenRoute,
                baseUIState = baseUIState,
                onClick = { dest -> currentScreenRoute = dest }
              )
            }
          }
        ) { paddingValues ->
          Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) { RenderActiveModule() }
        }
      }
    } else {
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
          ApartmentNavigationRail(
            baseUIState = baseUIState,
            selectedDestination = currentScreenRoute,
            isRailExpanded = true,
            railWidth = 260.dp,
            isApartmentsEmpty = baseUIState.addressId == 0L,
            onMenuClick = {},
            navigateToDestination = { dest -> currentScreenRoute = dest },
            navigateToApartment = finalizeApartmentSelection
          )
          VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
          Box(modifier = Modifier.weight(1f).fillMaxHeight()) { RenderActiveModule() }
        }
      }
    }
  }
}
