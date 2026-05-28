package com.ykis.ykismobkmp.ui.navigation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.InfoApartmentScreen
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreen
import com.ykis.ykismobkmp.ui.screens.settings.SettingsScreenModel
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
    var isRailExpanded by androidx.compose.runtime.saveable.rememberSaveable {
      mutableStateOf(navigationType != NavigationType.BOTTOM_NAVIGATION)
    }

    // 2. Лямбда-переключатель бургер-кнопки (Инвертирует флаг сжатия панели)
    val onMenuClick: () -> Unit = { isRailExpanded = !isRailExpanded }

    // 3. Плавная КМР-анимация ширины боковой панели (Схлопывание с 280.dp до 80.dp)
    val railWidth by androidx.compose.animation.core.animateDpAsState(
      targetValue = if (isRailExpanded) 280.dp else 80.dp,
      animationSpec = androidx.compose.animation.core.tween(400),
      label = "RailWidth"
    )

    // ИНЖЕКЦИЯ КОММУНАЛЬНЫХ КМР-МОДЕЛЕЙ ЧЕРЕЗ СИНГЛТОН ПРОВАЙДЕР KOIN
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val chatScreenModel = koinInject<ChatScreenModel>()
    val settingsScreenModel = koinInject<SettingsScreenModel>()

    // РЕАКТИВНЫЙ СБОР ПОТОКОВ СОСТОЯНИЙ ИЗ ОПЕРАТИВНОЙ ПАМЯТИ СМАРТФОНА
    val baseUIState by apartmentScreenModel.apartmentUiState.collectAsState()
    val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()
    val userList by chatScreenModel.userList.collectAsState()

    // ТОТАЛЬНЫЙ СИНХРОННЫЙ ДАМП КАЖДОГО КАДРА РЕКОМПОЗИЦИИ ХАБА КВАРТИР
    println("[YkisLogKMP.$className.RECOMPOSITION]: ======= КАДР ОБНОВЛЕНИЯ ХАБА APARTMENT =======")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.mainLoading   = ${baseUIState.mainLoading}")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.apartments.size = ${baseUIState.apartments.size}")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.addressId       = ${baseUIState.addressId}L")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.userRole        = ${baseUIState.userRole}")
    println("[YkisLogKMP.$className.RECOMPOSITION]: ======================================================")

    // ИСПРАВЛЕНО НАМЕРТВО ДЛЯ ИСКЛЮЧЕНИЯ RACE CONDITION И НАВИГАЦИОННОГО ТУПИКА:
    // Мы перевели текущий роут в мутабельное изменяемое состояние 'var ... by remember { mutableStateOf(...) }'.
    // Ошибки 'val cannot be reassigned' во всех кнопках, кликах по шторке и списках чатов ликвидированы навсегда!
    var currentScreenRoute by remember { mutableStateOf("LoadingModule") }

    // РЕАКТИВНЫЙ СЛУШАТЕЛЬ ХОЛОДНОГО СТАРТА:
    // Пока база данных инициализируется (mainLoading == true), мы удерживаем состояние "LoadingModule".
    // Как только СУБД отдает данные, этот блок один раз мягко переключит роут на целевой InfoApartmentScreen,
    // но оставляет за пользователем полное право свободно кликать по нижнему бару и открывать чаты руками!
    LaunchedEffect(baseUIState.mainLoading, baseUIState.addressId, baseUIState.apartments.size, baseUIState.userRole) {
      if (baseUIState.mainLoading) {
        currentScreenRoute = "LoadingModule"
        println("[YkisLogKMP.$className.LaunchedEffect]: [WAIT] СУБД БТИ грузится, удерживаем загрузочную заглушку.")
      } else {
        val oldRoute = currentScreenRoute
        currentScreenRoute = when {
          baseUIState.addressId == 0L || baseUIState.apartments.isEmpty() -> "AddApartmentScreen"
          baseUIState.userRole == UserRole.StandardUser -> "InfoApartmentScreen"
          else -> "UserListScreen"
        }
        println("[YkisLogKMP.$className.LaunchedEffect]: [ROUTE_READY] Синхронізація роута завершена: \"$oldRoute\" ➔ \"$currentScreenRoute\"")
      }
    }
    // ФУНКЦИЯ СИНХРОНИЗАЦИИ И СМЕНЫ ЛИЦЕВОГО СЧЕТА В БОКОВОЙ ШТОРКЕ DRAWER
    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      println("[YkisLogKMP.$className.finalizeApartmentSelection]: Зміна о/р квартири на Long ID: ${id}L")

      // Обновляем ID активного лицевого счета в ОЗУ вьюмодели
      apartmentScreenModel.setAddressId(id)

      coroutineScope.launch {
        if (drawerState.isOpen) {
          println("[YkisLogKMP.$className.finalizeApartmentSelection]: Закриття бокової шторки Drawer...")
          drawerState.close()
        }
        delay(200) // Пауза для плавной нативной анимации закрытия шторки
        currentScreenRoute = "InfoApartmentScreen" // Теперь переприсваивание полностью ЛЕГИТИМНО!
        println("[YkisLogKMP.$className.finalizeApartmentSelection]: Кадр успішно синхронізовано з ID: ${id}L")
      }
    }

    // ВНУТРЕННИЙ ГРАФИЧЕСКИЙ ДИСПЕТЧЕР ОТРЕНДЕРЕННЫХ МОДУЛЕЙ
    @Composable
    fun RenderActiveModule(route: String) {
      println("[YkisLogKMP.$className.RenderActiveModule]: [РЕНДЕР] Отрисовка графического кадра для роута: \"$route\"")

      when (route) {
        "LoadingModule" -> {
          // ИСПРАВЛЕНО НАМЕРТВО: Аккуратная системная крутилка на время вычитки холодного старта из СУБД!
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }

        "UserListScreen" -> {
          // Моніторинг ідентифікаторів для адмінів комунальних служб м. Южне
          LaunchedEffect(baseUIState.userRole, baseUIState.osbbId) {
            val role = baseUIState.userRole
            if (role != UserRole.StandardUser) {
              val effectiveOsbbId = when (role) {
                UserRole.VodokanalUser -> 9999L
                UserRole.YtkeUser -> 9998L
                UserRole.TboUser -> 9997L
                else -> baseUIState.osbbId
              }
              println("[YkisLogKMP.$className.RenderActiveModule]: [ADMIN_TRACK] Запуск прослуховування заявок для організації: $effectiveOsbbId")
              chatScreenModel.trackUserIdentifiersWithRole(role, effectiveOsbbId.toInt())
            }
          }

          val userListScreenInstance = remember(userList, baseUIState.userRole) {
            UserListScreen(
              userList = userList,
              navigationType = navigationType,
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              onUserClicked = { selectedItem ->
                if (baseUIState.userRole == UserRole.StandardUser) {
                  println("[YkisLogKMP.$className]: Стандартний користувач обрав квартиру ID: ${selectedItem.addressId}L")
                  apartmentScreenModel.setAddressId(selectedItem.addressId)
                } else {
                  val osbbId = when (baseUIState.userRole) {
                    UserRole.VodokanalUser -> 9999L
                    UserRole.YtkeUser -> 9998L
                    UserRole.TboUser -> 9997L
                    else -> baseUIState.osbbId
                  }
                  println("[YkisLogKMP.$className]: Адмін відкриває чат з UID: ${selectedItem.uid} для підприємства: $osbbId")
                  chatScreenModel.openChatWithUser(selectedItem, baseUIState.userRole, osbbId.toInt())
                }
                // ИСПРАВЛЕНО НАМЕРТВО: Переприсваивание мутабельного var теперь полностью легитимно!
                currentScreenRoute = "ChatScreenStateful"
              }
            )
          }
          userListScreenInstance.Content()
        }

        "AddApartmentScreen" -> {
          val addApartmentScreenInstance = remember {
            AddApartmentScreen(
              onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
              // ИСПРАВЛЕНО НАМЕРТВО: Переприсваивание мутабельного var теперь полностью легитимно!
              closeContentDetail = { currentScreenRoute = "InfoApartmentScreen" }
            )
          }
          addApartmentScreenInstance.Content()
        }

        "InfoApartmentScreen" -> {
          // ИСПРАВЛЕНО НАМЕРТВО: Вся временная верстка карточек полностью УДАЛЕНА!
          // Мы создаем экземпляр твоего полноценного готового КМР-экрана InfoApartmentScreen
          // и бесшовно монтируем его Content() прямо внутрь графического холста хаба!
          val infoScreenInstance = remember(baseUIState.addressId) {
            InfoApartmentScreen(
              onDrawerClicked = {
                println("[YkisLogKMP.$className.RenderActiveModule]: Клік по бургер-кнопці на екрані БТІ. Відкриття Drawer.")
                coroutineScope.launch { drawerState.open() }
              }
            )
          }

          println("[YkisLogKMP.$className.RenderActiveModule]: [CONNECT] Підключення повноцінного InfoApartmentScreen в рантайм хаба.")
          infoScreenInstance.Content()
        }



        "service_selector" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Селектор комунальних служб (Водоканал / Тепломережа)", style = MaterialTheme.typography.bodyLarge)
          }
        }

        "ChatScreenStateful" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Екран активної чат-кімнати обговорення", style = MaterialTheme.typography.bodyLarge)
          }
        }

        "SettingsScreenDest" -> {
          val settingsScreenInstance = remember {
            SettingsScreen(
              onDrawerClick = {
                coroutineScope.launch { drawerState.open() }
              }
            )
          }
          println("[YkisLogKMP.$className.RenderActiveModule]: Запуск живого экрана настроек и логаута SettingsScreen")
          settingsScreenInstance.Content()
        }

        else -> {
          // Мягкий фоллбэк: если роут сбился, но у жителя нет квартир — выводим БТИ
          if (baseUIState.userRole == UserRole.StandardUser && baseUIState.addressId == 0L) {
            val addApartmentScreenInstance = remember {
              AddApartmentScreen(
                onDrawerClicked = { coroutineScope.launch { drawerState.open() } },
                closeContentDetail = { currentScreenRoute = "InfoApartmentScreen" }
              )
            }
            addApartmentScreenInstance.Content()
          } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(text = "Модуль ЖКХ. Поточний роут: $currentScreenRoute", style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      }
    }


    // Блокируем экран крутилкой только в том случае, если база данных РЕАЛЬНО еще грузится
    // и навигация находится в стартовом состоянии "LoadingModule".
    if (baseUIState.mainLoading && currentScreenRoute == "LoadingModule") {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
      return // Прерываем выполнение кадра, удерживая лоадер до вычитки диска смартфона
    }

    // --- МАТРИЦА СБОРОК ИНТЕРФЕЙСА VOYAGER (Смартфон против Планшета) ---
    // ВНИМАНИЕ: Используем проверку флага нативного перечисления из твоего конструктора
    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
      // 📱 ПРЕСЕТ СМАРТФОНА: Адаптивная боковая шторка расчетного центра Южного
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            selectedDestination = currentScreenRoute,
            navigateToDestination = { dest ->
              coroutineScope.launch {
                println("[YkisLogKMP.$className.Drawer]: Клієнт переключив вкладку шторки на: \"$dest\"")
                drawerState.close()
                // ИСПРАВЛЕНО НАМЕРТВО: Присваивание мутабельного var теперь полностью легитимно!
                currentScreenRoute = dest
              }
            },
            onMenuClick = {
              coroutineScope.launch { drawerState.close() }
            },
            navigateToApartment = finalizeApartmentSelection,
            isApartmentsEmpty = baseUIState.addressId == 0L
          )
        }
      ) {
        Scaffold(
          bottomBar = {
            // Нижнее меню навигации рендерится только если адрес привязан в БТИ
            val showBottomBar = baseUIState.addressId != 0L || baseUIState.userRole != UserRole.StandardUser
            if (showBottomBar) {
              BottomNavigationBar(
                selectedDestination = currentScreenRoute,
                baseUIState = baseUIState,
                onClick = { dest ->
                  println("[YkisLogKMP.$className.BottomBar]: Клієнт обрав вкладку нижнього бара: \"$dest\"")
                  // ИСПРАВЛЕНО НАМЕРТВО: Присваивание мутабельного var теперь полностью легитимно!
                  currentScreenRoute = dest
                }
              )
            }
          }
        ) { paddingValues ->
          Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Отрисовываем active модуль на основе вычисленного реактивного маршрута
            RenderActiveModule(route = currentScreenRoute)
          }
        }
      }
    } else {
      // 🖥️ ПРЕСЕТ ПЛАНШЕТА / MAC DESKTOP: Стационарный боковой рельс ApartmentNavigationRail
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
          ApartmentNavigationRail(
            baseUIState = baseUIState,
            selectedDestination = currentScreenRoute,
            isRailExpanded = isRailExpanded, // Реактивный флаг развернутости панели из конструктора
            onMenuClick = onMenuClick,       // Схлопывание рельса по клику на "бургер" верхнего бара
            navigateToDestination = { dest ->
              println("[YkisLogKMP.$className.NavRail]: Клієнт переключив бічну панель Rail на: \"$dest\"")
              // ИСПРАВЛЕНО НАМЕРТВО: Присваивание мутабельного var теперь полностью легитимно!
              currentScreenRoute = dest
            },
            navigateToApartment = finalizeApartmentSelection,
            railWidth = railWidth, // ИСПРАВЛЕНО НАМЕРТВО: Оставлен ровно один легитимный вызов ширины Dp
            isApartmentsEmpty = baseUIState.addressId == 0L
          )

          VerticalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
          )

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            RenderActiveModule(route = currentScreenRoute)
          }
        }
      }
    }
  }
}


