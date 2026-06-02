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
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenStateful
import com.ykis.ykismobkmp.ui.screens.chat.ServiceSelectorScreen
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
    // 1. Единый реактивный КМР-триггер переключения стартовых экранов Хаба ЮКІС
    LaunchedEffect(baseUIState.mainLoading, baseUIState.addressId) {
      if (!baseUIState.mainLoading && baseUIState.addressId != 0L) {
        activeSubModule = when {
          // Для жильца открываем экран Info со сводными балансами и анкетой БТИ
          baseUIState.userRole == UserRole.StandardUser -> "InfoApartmentScreen"

          // Исправлено: Для председателя ОСББ стартовым экраном также выставляем "InfoApartmentScreen"!
          // Система автоматически подтянет БТИ первой квартиры дома, полностью исключая зависания!
          baseUIState.userRole == UserRole.OsbbUser -> "InfoApartmentScreen"

          // Для диспетчеров других городских служб оставляем список чатов абонентов
          else -> "UserListScreen"
        }
        println("[YkisLogKMP.$className.NavigationTrigger]: Стан навантаження завершено. Маршрут activeSubModule переведено на: $activeSubModule")
      }
    }

    // 2. Слушатель роли для администраторов коммунальных служб Южного
    LaunchedEffect(baseUIState.userRole, baseUIState.osbbId) {
      val role = baseUIState.userRole
      if (role != UserRole.StandardUser && role != UserRole.Unknown) {
        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> 9999L
          UserRole.YtkeUser      -> 9998L
          UserRole.TboUser       -> 9997L
          else                   -> baseUIState.osbbId ?: 0L
        }

        // Настраиваем префикс для сокет-контура админ-панели чата
        if (role == UserRole.OsbbUser) {
          chatScreenModel.onServiceSelectedForResident("OSBB")
        }

        // Атомарный запуск фонового трекера ключей Firebase для админ-уведомлений
        chatScreenModel.trackUserIdentifiersWithRole(role, effectiveOsbbId)
      }
    }

    // 3. Функция финального выбора квартиры (Переключение локального контекста БТИ жильца/админа)
    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      println("[YkisLogKMP.$className.finalizeApartmentSelection]: Зміна о/р квартири на Long ID: ${id}L")

      // Переключаем активный подмодуль на анкету БТИ выбранного лицевого счета
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
              navigationType = navigationType,
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              onUserClicked = { selectedItem ->
                // Используем оригинальную логику переключения на анкету БТИ квартиры
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

          "chat_selector" -> {
            println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Шар 1 — відображення селектора служб.")
            ServiceSelectorScreen(
              baseUIState = baseUIState,
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              onServiceClick = { selectedServiceDebt ->
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Службу обрано: ${selectedServiceDebt.name}. Перехід на список кімнат.")
                // Переключаем Crossfade смартфона на шаг 2, сохраняя целостность стейтов в ОЗУ!
                activeSubModule = "chat_user_list"
              }
            ).Content()
          }

          "chat_user_list" -> {
            println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Шар 2 — відображення списку абонентів.")
            UserListScreen(
              onDrawerClicked = {
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Повернення назад на вибір компаній.")
                activeSubModule = "chat_selector"
              },
              navigationType = navigationType,
              onUserClicked = { selectedUserEntity ->
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Квартиру обрано: ${selectedUserEntity.address}. Вхід в кімнату повідомлень.")
                activeSubModule = "chat_room_active"
              }
            ).Content()
          }

          "chat_room_active" -> {
            println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Шар 3 — відкриття активної кімнати повідомлень ChatScreen.")
            ChatScreen(
              onBackClick = {
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Вихід з кімнати назад до списку квартир.")
                activeSubModule = "chat_user_list"
              }
            ).Content()
          }

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

    // Вычитываем контроллеры фокуса и ввода на уровне корня адаптивной матрицы сборки
    val localFocusManager = LocalFocusManager.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    // --- АДАПТИВНАЯ МАТРИЦА СБОРКИ ИНТЕРФЕЙСА (Смартфон против Планшета) ---
    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            navigator = globalNavigator,
            activeSubModule = activeSubModule,
            onSubModuleChange = { newModule ->
              println("[YkisLogKMP.MainApartmentScreen.Drawer]: Зміна підмодуля зі шторки на: $newModule")
              activeSubModule = newModule
            },
            onMenuClick = {
              // Прячем клавиатуру и сбрасываем фокус при закрытии шторки смартфона
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
          bottomBar = {
            val showBottomBar = baseUIState.addressId != 0L || baseUIState.userRole != UserRole.StandardUser
            if (showBottomBar) {
              BottomNavigationBar(
                navigator = globalNavigator,
                baseUIState = baseUIState,
                activeSubModule = activeSubModule,
                onSubModuleChange = { newModule ->
                  // Прямая Stateless-запись прилетевшего строкового роута чата ("chat_selector") или счетчиков
                  println("[YkisLogKMP.MainApartmentScreen.BottomNav]: Зміна підмодуля з нижньої панелі на: $newModule")
                  activeSubModule = newModule
                }
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
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Настройка вызова боковой панели внутри MainApartmentScreen.kt
        Row(modifier = Modifier.fillMaxSize()) {
          ApartmentNavigationRail(
            baseUIState = baseUIState,
            navigator = globalNavigator,
            activeSubModule = activeSubModule,
            onSubModuleChange = { newModule -> activeSubModule = newModule },
            isRailExpanded = isRailExpanded,

            // ИСПРАВЛЕНО: Сброс текста поисковой строки при скрытии/раскрытии рельса!
            // Это автоматически аннулирует фокус ввода TextField и мягко скроет клавиатуру.
            onMenuClick = {
              println("[YkisLogKMP.MainApartmentScreen.Rail]: Клік по бургер-кнопці. Анулювання фокусу пошуку.")
              apartmentScreenModel.onSearchQueryChanged("") // Сбрасываем текст и фокус ввода
              onMenuClick() // Вызываем оригинальный коллбек изменения ширины рельса
            },

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


