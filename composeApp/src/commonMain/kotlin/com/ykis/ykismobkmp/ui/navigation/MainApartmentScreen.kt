package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// Импорты инфраструктуры, сервисов и моделей ЮКИС г. Южный
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState


// Прямые вызовы твоих графических экранов (Замени на актуальные Composable вызовы, если нужно)
import com.ykis.ykismobkmp.ui.navigation.InfoApartmentScreenDest
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel

private const val tag = "MainApartmentScreen"

/**
 * [MainApartmentScreen] — Главный адаптивный КМР-контейнер приложения ЮКИС.
 * ИСПРАВЛЕНО: Полный отказ от TabNavigator библиотеки Voyager, переключение реализовано на стандартном Compose when.
 */
class MainApartmentScreen(
  private val contentType: ContentType,
  private val navigationType: NavigationType
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    val screenModel = koinInject<ApartmentScreenModel>()
    val firebaseService = koinInject<FirebaseService>()

    val baseUIState by screenModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // 1. КОНТРОЛЬ ГОТОВНОСТИ (Золотой фонд ЖКХ-расчетов)
    val isDataReady = remember(baseUIState.uid, baseUIState.mainLoading) {
      !baseUIState.uid.isNullOrBlank() && !baseUIState.mainLoading
    }

    // 2. ИСПРАВЛЕНО: Храним активный строковый маршрут нативно через Compose mutableStateOf
    var currentScreenRoute by remember(baseUIState.userRole, isDataReady) {
      mutableStateOf(
        if (!isDataReady || baseUIState.userRole == UserRole.StandardUser) {
          "InfoApartmentScreen"
        } else {
          "service_selector"
        }
      )
    }

    // Лоадер холодного старта сессии Firebase
    if (!isDataReady) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
      }
      return
    }

    // Внутренняя КМР-фабрика рендеринга активного экрана на месте вызова
    @Composable
    fun RenderCurrentScreen() {
      when (currentScreenRoute) {
        "service_selector", "UserListScreen" -> {
          // TODO: Сюда мы следующим шагом поставим вызов твоей ленты чатов ChatListScreen()
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Лента обговорень та заявок ОСМД")
          }
        }
        else -> {
          // Рендерим твои характеристики БТИ квартиры
          InfoApartmentScreenDest(addressId = baseUIState.addressId).Content()
        }
      }
    }

    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
      // ============================================================
      // --- ВЕРСИЯ ДЛЯ СМАРТФОНОВ (Нижняя навигация + Слайдер) ---
      // ============================================================
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            selectedDestination = currentScreenRoute,
            navigateToDestination = { targetRoute ->
              println("[$tag.Mobile]: Навігація зі слайдера -> $targetRoute")
              currentScreenRoute = targetRoute
            },
            onMenuClick = { coroutineScope.launch { drawerState.close() } },
            navigateToApartment = { selectedAddressId ->
              println("[$tag.Mobile]: Зміна квартири в слайдері ID: $selectedAddressId")
              screenModel.setAddressId(selectedAddressId)
            },
            isApartmentsEmpty = baseUIState.apartments.isEmpty()
          )
        }
      ) {
        Scaffold(
          bottomBar = {
            BottomNavigationBar(
              selectedDestination = currentScreenRoute,
              baseUIState = baseUIState,
              onClick = { targetRoute ->
                println("[$tag.Mobile]: Натиснута вкладка нижнього меню -> $targetRoute")
                currentScreenRoute = targetRoute
              }
            )
          }
        ) { paddingValues ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
          ) {
            RenderCurrentScreen() // Прямой нативный вызов экрана
          }
        }
      }
    } else {
      // ============================================================
      // --- ВЕРСИЯ ДЛЯ MAC DESKTOP / ПЛАНШЕТОВ (Боковой рельс) ---
      // ============================================================
      Row(modifier = Modifier.fillMaxSize()) {
        ApartmentNavigationRail(
          baseUIState = baseUIState,
          selectedDestination = currentScreenRoute,
          isRailExpanded = true,
          railWidth = 240.dp,
          isApartmentsEmpty = baseUIState.apartments.isEmpty(),
          onMenuClick = { println("[$tag.Desktop]: Переключение состояния бокового меню") },
          navigateToDestination = { targetRoute ->
            println("[$tag.Desktop]: Перехід з бокового рельсу -> $targetRoute")
            currentScreenRoute = targetRoute
          },
          navigateToApartment = { selectedAddressId ->
            println("[$tag.Desktop]: Вибрано о/р квартири: $selectedAddressId")
            screenModel.setAddressId(selectedAddressId)
          }
        )

        VerticalDivider(
          thickness = 0.5.dp,
          color = MaterialTheme.colorScheme.outlineVariant
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          RenderCurrentScreen() // Прямой нативный вызов экрана
        }
      }
    }
  }
}

