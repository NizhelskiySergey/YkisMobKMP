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
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenStateful
import com.ykis.ykismobkmp.ui.screens.chat.ServiceSelectorScreen
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import com.ykis.ykismobkmp.ui.screens.ledger.MainServiceScreen
import com.ykis.ykismobkmp.ui.screens.meter.MainMeterScreen
import com.ykis.ykismobkmp.ui.screens.meter.MeterListScreen
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementListScreen
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
    val announcementModel = koinInject<com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel>()

    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    val userList by chatScreenModel.userList.collectAsState()

    // ИСПРАВЛЕНО: Ранний старт мониторинга объявлений для корректного отображения бейджей при запуске
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

    // ИСПРАВЛЕНО НАМЕРТВО: Реактивный диспетчер переключения подмодулей.
    // Теперь приложение САМО переходит на нужный экран сразу после успешной привязки счета или авторизации админа.
    LaunchedEffect(baseUIState.addressId, baseUIState.userRole, baseUIState.osbbId, baseUIState.mainLoading) {
      val role = baseUIState.userRole
      val addressId = baseUIState.addressId
      val osbbId = baseUIState.osbbId

      // 1. Обработка ПЕРВОГО запуска (Холодный старт)
      if (isInitialBoot && !baseUIState.mainLoading) {
        activeSubModule = when {
          role == UserRole.StandardUser -> {
            if (addressId != 0L) "InfoApartmentScreen" else "AddApartmentScreen"
          }
          role != UserRole.Unknown -> "chat_user_list"
          else -> "AddApartmentScreen"
        }
        isInitialBoot = false
        println("[YkisLogKMP.$className.Navigation]: Холодний старт завершено. Екран: $activeSubModule")
        return@LaunchedEffect
      }

      // 2. Обработка ДИНАМИЧЕСКИХ переходов (Успешная привязка/авторизация)
      if (!isInitialBoot && activeSubModule == "AddApartmentScreen") {
        if (role == UserRole.StandardUser && addressId != 0L) {
          println("[YkisLogKMP.$className.Navigation]: Рахунок прив'язано. Перехід на InfoApartmentScreen")
          activeSubModule = "InfoApartmentScreen"
        } else if (role != UserRole.StandardUser && role != UserRole.Unknown && osbbId != 0L) {
          println("[YkisLogKMP.$className.Navigation]: Адмін авторизований. Перехід на список чатів")
          activeSubModule = "chat_user_list"
        }
      }
    }

    // 2. Слушатель роли для администраторов коммунальных служб Южного
    LaunchedEffect(baseUIState.userRole, baseUIState.osbbId, baseUIState.apartments) {
      val role = baseUIState.userRole
      val osbbId = baseUIState.osbbId ?: 0L
      
      if (role != UserRole.Unknown) {
        // Если житель — запускаем мониторинг всех его квартир
        if (role == UserRole.StandardUser) {
          if (baseUIState.apartments.isNotEmpty()) {
             chatScreenModel.trackUserIdentifiersWithRole(role, 0L, baseUIState.apartments)
          }
          return@LaunchedEffect
        }

        // КРИТИЧЕСКИЙ ЧЕК: Если админ ОСББ, но ID еще не подтянулся (равен 0), ждем.
        if (role == UserRole.OsbbUser && osbbId == 0L) {
          println("[YkisLogKMP.$className.Navigation]: Очікування завантаження osbbId для адміна...")
          return@LaunchedEffect
        }

        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> Constants.WATER_SERVICE_ID
          UserRole.YtkeUser      -> Constants.WARM_SERVICE_ID
          UserRole.TboUser       -> Constants.GARBAGE_SERVICE_ID
          else                   -> osbbId
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

          "AddApartmentScreen" -> {
            AddApartmentScreen(
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              closeContentDetail = {
                println("[YkisLogKMP.MainApartmentScreen.AddApartmentScreen]: Успешное закрытие формы привязки. Возврат на анкету БТИ.")
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
            // ОПТИМИЗАЦИЯ: Если админ попал сюда случайно — редирект в список чатов
            if (baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.Unknown) {
              println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Админ в селекторе — редирект в chat_user_list.")
              activeSubModule = "chat_user_list"
            }

            println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Слой 1 — отображение селектора служб.")
            ServiceSelectorScreen(
              baseUIState = baseUIState,
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              onServiceClick = { selectedServiceDebt ->
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Служба выбрана: ${selectedServiceDebt.name}. Переход на список комнат.")
                activeSubModule = "chat_user_list"
              }
            ).Content()
          }

          "chat_user_list" -> {
            println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Слой 2 — отображение списка абонентов.")
            UserListScreen(
              onDrawerClicked = {
                if (baseUIState.userRole == UserRole.StandardUser) {
                  println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Повернення назад на вибір компаній.")
                  activeSubModule = "chat_selector"
                } else {
                  println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Адмін чат — повернення заблоковано.")
                }
              },
              navigationType = navigationType,
              onUserClicked = { selectedUserEntity ->
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Квартира вибрана: ${selectedUserEntity.address}. Налаштування контексту...")
                
                val role = baseUIState.userRole
                val targetAddrId = selectedUserEntity.addressId

                if (role == UserRole.OsbbUser || role == UserRole.StandardUser) {
                  // Для адміна ОСББ та Мешканця — повна синхронізація (Firestore + Rail + БТІ)
                  println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Роль $role — фіксація addressId: $targetAddrId")
                  apartmentScreenModel.setAddressId(targetAddrId)
                } else {
                  // Для міських служб — «легке» завантаження даних БТІ без зміни глобального якоря
                  println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Роль міської служби — фонове завантаження анкети.")
                  apartmentScreenModel.getApartment(targetAddrId)
                }
                
                // Активация комнаты в модели чата
                chatScreenModel.openChatWithUser(
                  user = selectedUserEntity,
                  currentRole = role,
                  currentOsbbId = baseUIState.osbbId ?: 0L
                )

                activeSubModule = "chat_room_active"
              }
            ).Content()
          }

          "chat_room_active" -> {
            println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Слой 3 — открытие активной комнаты сообщений ChatScreen.")
            ChatScreen(
              onBackClick = {
                println("[YkisLogKMP.MainApartmentScreen.ChatRouter]: Выход из комнаты назад к списку квартир.")
                activeSubModule = "chat_user_list"
              }
            ).Content()
          }

          "announcements" -> {
            println("[YkisLogKMP.MainApartmentScreen]: Перехід до розділу Оголошення.")
            AnnouncementListScreen(
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              navigationType = navigationType
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
              println("[YkisLogKMP.MainApartmentScreen.Drawer]: Изменение подмодуля из шторки на: $newModule")
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
          // ИСПРАВЛЕНО: Учитываем только верхнюю статусную полосу, игнорируя нижние системные кнопки,
          // чтобы нижнее меню было максимально компактным и "прижатым".
          contentWindowInsets = WindowInsets.statusBars,
          bottomBar = {
            val isChatRoomActive = activeSubModule == "chat_room_active"
            val showBottomBar = (baseUIState.addressId != 0L || baseUIState.userRole != UserRole.StandardUser) && !isChatRoomActive

            if (showBottomBar) {
              BottomNavigationBar(
                navigator = globalNavigator,
                baseUIState = baseUIState,
                activeSubModule = activeSubModule,
                onSubModuleChange = { newModule ->
                  // Прямая Stateless-запись прилетевшего строкового роута чата ("chat_selector") или счетчиков
                  println("[YkisLogKMP.MainApartmentScreen.BottomNav]: Изменение подмодуля из нижней панели на: $newModule")
                  activeSubModule = newModule
                }
              )
            }
          }
        ) { paddingValues ->
          val isChatRoomActive = activeSubModule == "chat_room_active"
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(
                top = paddingValues.calculateTopPadding(),
                bottom = if (isChatRoomActive) 0.dp else paddingValues.calculateBottomPadding()
              )
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


