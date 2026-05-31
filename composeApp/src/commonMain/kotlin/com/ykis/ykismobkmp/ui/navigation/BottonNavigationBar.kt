package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
private const val className = "BottomNavigationBar"
@Composable
fun BottomNavigationBar(
  navigator: Navigator, // Навигатор верхнего уровня
  baseUIState: BaseUIState, // Используем для динамической ролевой фильтрации вкладок жильца/админа
  activeSubModule: String, // Сквозной стейт активного подмодуля Хаба ЮКІС
  onSubModuleChange: (String) -> Unit // Сквозной коллбек смены кадра
) {
  // Нативная КМР-инжекция общей модели чатов YkisMobKMP
  val chatScreenModel = koinInject<ChatScreenModel>()

  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()

  // Считаем общую сумму непрочитанных сообщений ГИОЦ г. Южного
  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }

  // Логирование рантайма согласно правилу [Класс.Метод]
  LaunchedEffect(totalUnread) {
    println("[$className.BottomNavigationBar]: Перерасчет суммарного счетчика бейджей: $totalUnread")
  }

  NavigationBar(
    modifier = Modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.surfaceContainer
  ) {
    // 1. Получаем динамический список дестинаций на основе роли текущей сессии
    val navDestinations = getNavDestinations(role = baseUIState.userRole)

    navDestinations.forEach { destination ->
      // ИСПРАВЛЕНО НАМЕРТВО: Добавлена строка "MeterScreen" во все ветки проверок счетчиков!
      // Любые блокировки и несовпадения имен дестинаций на смартфонах уничтожены навсегда!
      val isSelected = when (destination.route) {
        "InfoApartmentScreenDest", "AdminUserListScreenDest", "InfoApartmentScreen", "UserListScreen" -> {
          activeSubModule == "InfoApartmentScreen" || activeSubModule == "UserListScreen"
        }
        "MainMeterScreenDest", "MeterScreen", "service_selector" -> activeSubModule == "service_selector"
        "ChatScreenDest", "ChatScreenStateful" -> activeSubModule == "ChatScreenStateful"
        "MainServiceScreenDest", "ServiceListScreen", "finance_selector" -> activeSubModule == "finance_selector"
        else -> false
      }

      NavigationBarItem(
        selected = isSelected,
        onClick = {
          println("[$className.BottomNavigationBar]: Тап по нижней вкладке -> ${destination.route}")

          // ИСПРАВЛЕНО НАМЕРТВО: Добавлено распознавание строки "MeterScreen" для вызова переключателя!
          when (destination.route) {
            "InfoApartmentScreenDest", "AdminUserListScreenDest", "InfoApartmentScreen", "UserListScreen" -> {
              val targetRoute = if (baseUIState.userRole == UserRole.StandardUser) "InfoApartmentScreen" else "UserListScreen"
              onSubModuleChange(targetRoute)
            }
            "MainMeterScreenDest", "MeterScreen", "service_selector" -> {
              chatScreenModel.setSelectedService(null as String?)
              onSubModuleChange("service_selector")
            }
            "ChatScreenDest", "ChatScreenStateful" -> {
              onSubModuleChange("ChatScreenStateful")
            }
            "MainServiceScreenDest", "ServiceListScreen", "finance_selector" -> {
              onSubModuleChange("finance_selector")
            }
          }
        },
        icon = {
          BadgedBox(
            badge = {
              // 2. УНИВЕРСАЛЬНАЯ ПРОВЕРКА БЕЙДЖА ЧАТОВ
              val isChatRoute = destination.route == "ChatScreenDest" ||
                destination.route == "ChatScreenStateful" ||
                destination.route == "AdminUserListScreenDest" ||
                destination.route == "UserListScreen"

              if (isChatRoute && totalUnread > 0) {
                Badge(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                ) {
                  Text(text = if (totalUnread > 9) "9+" else totalUnread.toString())
                }
              }
            }
          ) {
            Icon(
              imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
              contentDescription = null
            )
          }
        },
        label = {
          Text(stringResource(destination.labelId))
        },
        alwaysShowLabel = false
      )
    }
  }
}






