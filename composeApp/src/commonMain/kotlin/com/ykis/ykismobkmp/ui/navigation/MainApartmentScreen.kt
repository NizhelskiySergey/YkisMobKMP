package com.ykis.ykismobkmp.ui.navigation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.ykis.ykismobkmp.ui.screens.appartment.AddApartmentScreen
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.UserListScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val className = "MainApartmentScreen"

/**
 * [MainApartmentScreen] — Головний адаптивний хаб житлового фонду біллінгу м. Южне.
 * МОДИФИЦИРОВАНО: Интегрирован детальный сквозной аудит Race Condition, вырезаны дублирующие
 * параллельные потоки вызова сети. Логи приведены строго под эталон [YkisLogKMP].
 */
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
    // ИНЖЕКЦИЯ КОММУНАЛЬНЫХ КМР-МОДЕЛЕЙ ЧЕРЕЗ ПРОВАЙДЕР KOIN
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val chatScreenModel = koinInject<ChatScreenModel>()

    // РЕАКТИВНЫЙ СБОР ПОТОКОВ СОСТОЯНИЙ ИЗ ОПЕРАТИВНОЙ ПАМЯТИ СМАРТФОНА
    val baseUIState by apartmentScreenModel.baseUIState.collectAsState()
    val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()
    val userList by chatScreenModel.userList.collectAsState()

    // ТОТАЛЬНЫЙ СИНХРОННЫЙ ДАМП КАЖДОГО КАДРА РЕКОМПОЗИЦИИ ХАБА КВАРТИР
    println("[YkisLogKMP.$className.RECOMPOSITION]: ======= КАДР ОБНОВЛЕНИЯ ХАБА APARTMENT =======")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.mainLoading   = ${baseUIState.mainLoading}")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.apartments.size = ${baseUIState.apartments.size}")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.addressId       = ${baseUIState.addressId}L")
    println("[YkisLogKMP.$className.RECOMPOSITION]: • baseUIState.userRole        = ${baseUIState.userRole}")
    println("[YkisLogKMP.$className.RECOMPOSITION]: ======================================================")

    // ИСПРАВЛЕНО НАМЕРТВО: Если адрес равен 0L или список в ОЗУ пуст — дефолтным роутом
    // ЖЕСТКО выставляется AddApartmentScreen, пробивая любые задержки корутин Ktor!
    var currentScreenRoute by remember(baseUIState.addressId, baseUIState.apartments.size, baseUIState.userRole) {
      val calculatedRoute = if (baseUIState.addressId == 0L || baseUIState.apartments.isEmpty()) {
        "AddApartmentScreen"
      } else {
        if (baseUIState.userRole == UserRole.StandardUser) "InfoApartmentScreen" else "UserListScreen"
      }

      println("[YkisLogKMP.$className.remember_Route]: ====== ИНИЦИАЛИЗАЦИЯ И ТРИГГЕР REMEMBER ======")
      println("[YkisLogKMP.$className.remember_Route]: • Ключ addressId       = ${baseUIState.addressId}L")
      println("[YkisLogKMP.$className.remember_Route]: • Ключ apartments.size = ${baseUIState.apartments.size}")
      println("[YkisLogKMP.$className.remember_Route]: • Ключ userRole        = ${baseUIState.userRole}")
      println("[YkisLogKMP.$className.remember_Route]: • ВЫЧИСЛЕННЫЙ МАРШРУТ  = \"$calculatedRoute\"")
      println("[YkisLogKMP.$className.remember_Route]: =====================================================")

      mutableStateOf(calculatedRoute)
    }

    // ИСПРАВЛЕНО НАМЕРТВО: Жесткий реактивный слушатель. Как только фоновый лоадер Ktor гаснет
    // и возвращает пустой список, этот блок мгновенно перерисует кадр, уничтожая белый экран!
    LaunchedEffect(baseUIState.mainLoading, baseUIState.apartments.size, baseUIState.addressId, baseUIState.userRole) {
      println("[YkisLogKMP.$className.LaunchedEffect]: ====== ТРИГГЕР СЛУШАТЕЛЯ МАРШРУТОВ ======")
      println("[YkisLogKMP.$className.LaunchedEffect]: • Излучаемый mainLoading   = ${baseUIState.mainLoading}")
      println("[YkisLogKMP.$className.LaunchedEffect]: • Излучаемый apartments.size = ${baseUIState.apartments.size}")
      println("[YkisLogKMP.$className.LaunchedEffect]: • Излучаемый addressId       = ${baseUIState.addressId}L")
      println("[YkisLogKMP.$className.LaunchedEffect]: • Излучаемый userRole        = ${baseUIState.userRole}")
      println("[YkisLogKMP.$className.LaunchedEffect]: ================================================")

      if (!baseUIState.mainLoading) {
        val oldRoute = currentScreenRoute

        currentScreenRoute = if (baseUIState.addressId == 0L || baseUIState.apartments.isEmpty()) {
          "AddApartmentScreen"
        } else {
          if (baseUIState.userRole == UserRole.StandardUser) "InfoApartmentScreen" else "UserListScreen"
        }

        println("[YkisLogKMP.$className.LaunchedEffect]: [ROUTE_CHANGED] Реагування на стейт мережі: \"$oldRoute\" ➔ \"$currentScreenRoute\"")
      } else {
        println("[YkisLogKMP.$className.LaunchedEffect]: [SKIP] Роут не обчислюється, фоновий лоадер мережі ще крутиться (mainLoading=true)")
      }
    }

    // ИСПРАВЛЕНО НАМЕРТВО: Полностью стерт дублирующий блок LaunchedEffect(currentFirebaseUid),
    // который спамил параллельные сетевые вызовы к getApartmentsByUser.php и вызывал UI Thread Jam!

    // ФУНКЦИЯ СИНХРОНИЗАЦИИ И СМЕНЫ ЛИЦЕВОГО СЧЕТА В БОКОВОЙ ШТОРКЕ DRAWER
    val finalizeApartmentSelection: (Long) -> Unit = { id ->
      println("[YkisLogKMP.$className.finalizeApartmentSelection]: Зміна о/р квартири на Long ID: ${id}L")
      apartmentScreenModel.setAddressId(id)

      coroutineScope.launch {
        if (drawerState.isOpen) {
          println("[YkisLogKMP.$className.finalizeApartmentSelection]: Закриття бокової шторки Drawer...")
          drawerState.close()
        }
        delay(200) // Пауза для плавной анимации интерфейса смартфона
        currentScreenRoute = "InfoApartmentScreen"
        println("[YkisLogKMP.$className.finalizeApartmentSelection]: Кадр успішно переведено на InfoApartmentScreen")
      }
    }

    // ====================================================================
    // --- ВНУТРЕННИЙ ИЗОЛИРОВАННЫЙ ДИСПЕТЧЕР ОТРЕНДЕРИВАНИЯ МОДУЛЕЙ ---
    // ====================================================================
    @Composable
    fun RenderActiveModule(route: String) {
      println("[YkisLogKMP.$className.RenderActiveModule]: [РЕНДЕР] Отрисовка графического кадра для роута: \"$route\"")

      when (route) {
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

          val userListScreenInstance = remember(userList, navigationType, baseUIState.userRole) {
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
              closeContentDetail = { currentScreenRoute = "InfoApartmentScreen" }
            )
          }
          addApartmentScreenInstance.Content()
        }

        "InfoApartmentScreen" -> {
          key(baseUIState.addressId) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              androidx.compose.material3.Text(
                text = "Характеристики БТІ квартири ID: ${baseUIState.addressId}",
                style = MaterialTheme.typography.bodyLarge
              )
            }
          }
        }

        "service_selector" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text("Селектор комунальних служб (Водоканал / Тепломережа)")
          }
        }

        "ChatScreenStateful" -> {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text("Екран активної чат-кімнати обговорення")
          }
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
              androidx.compose.material3.Text("Модуль ЖКХ. Поточний роут: $currentScreenRoute")
            }
          }
        }
      }
    }

    // БЛОКИРОВКА ЭКРАНА НА ВРЕМЯ АСИНХРОННЫХ ЗАПРОСОВ KTOR К СУБД MySQL ЮЖНОГО
    if (baseUIState.mainLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
      return // Прерываем выполнение кадра, удерживая лоадер
    }

    // --- МАТРИЦА СБОРОК ИНТЕРФЕЙСА VOYAGER (Смартфон против Планшета) ---
    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
      // 📱 ПРЕСЕТ СМАРТФ0НА: Адаптивная боковая шторка расчетного центра Южного
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalNavigationDrawerContent(
            baseUIState = baseUIState,
            selectedDestination = currentScreenRoute,
            // ИСПРАВЛЕНО НАМЕРТВО: Лишние аргументы ViewModel стерты.
            // Компонент шторки сам инжектирует всё необходимое через koinInject()!
            navigateToDestination = { dest ->
              coroutineScope.launch {
                println("[YkisLogKMP.$className.Drawer]: Клієнт переключив вкладку шторки на: \"$dest\"")
                drawerState.close()
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
                  currentScreenRoute = dest
                }
              )
            }
          }
        ) { paddingValues ->
          Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Отрисовываем активный модуль на основе вычисленного реактивного маршрута
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
              currentScreenRoute = dest
            },
            navigateToApartment = finalizeApartmentSelection,
            railWidth = railWidth, // ИСПРАВЛЕНО НАМЕРТВО: Оставлен ровно один легитимный вызов ширины Dp
            isApartmentsEmpty = baseUIState.addressId == 0L
          )

          androidx.compose.material3.VerticalDivider(
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

