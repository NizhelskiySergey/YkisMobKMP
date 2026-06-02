package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.list.TotalServiceDebt
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

  // ИСПРАВЛЕНО НАМЕРТВО: Выставили оптимальные 64.dp и СБРОСИЛИ скрытые системные отступы windowInsets!
  // Это уберет лишние пустые 2 сантиметра снизу, но оставит кнопки КРУПНЫМИ и ЧИТАЕМЫМИ!
  NavigationBar(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    windowInsets = WindowInsets(0, 0, 0, 0) // Выталкиваем элементы наружу, убирая микро-сжатие
  ) {
    // Получаем динамический список дестинаций на основе роли текущей сессии
    val navDestinations = getNavDestinations(role = baseUIState.userRole)

    navDestinations.forEach { destination ->
      // Добавлен явный маркер "chat_user_list" в блок чата и убран из блока Главной.
      val isSelected = when (destination.route) {
        "InfoApartmentScreenDest", "AdminUserListScreenDest", "InfoApartmentScreen", "UserListScreen" -> {
          activeSubModule == "InfoApartmentScreen" || activeSubModule == "UserListScreen"
        }

        "MainMeterScreenDest", "MeterScreen", "service_selector" -> activeSubModule == "service_selector"

        // Иконка чата гарантированно подсветится активной на любом из 3 пошаговых этапов админ/жилец чата!
        "ChatScreenDest", "ChatScreenStateful", "chat_selector", "chat_user_list" -> {
          activeSubModule == "chat_selector" || activeSubModule == "chat_user_list" || activeSubModule == "chat_room_active"
        }

        "MainServiceScreenDest", "ServiceListScreen", "finance_selector" -> activeSubModule == "finance_selector"
        else -> false
      }

      NavigationBarItem(
        selected = isSelected,
        onClick = {
          println("[$className.BottomNavigationBar]: Тап по нижней вкладке смартфона -> ${destination.route}")

          when (destination.route) {
            "InfoApartmentScreenDest", "AdminUserListScreenDest", "InfoApartmentScreen", "UserListScreen" -> {
              val targetRoute = if (baseUIState.userRole == UserRole.StandardUser) "InfoApartmentScreen" else "UserListScreen"
              onSubModuleChange(targetRoute)
            }

            "MainMeterScreenDest", "MeterScreen", "service_selector" -> {
              chatScreenModel.setSelectedService(null as TotalServiceDebt?)
              onSubModuleChange("service_selector")
            }

            // Любой входящий роут чата админа или жильца перенаправляет строго на каноничную стартовую точку!
            "ChatScreenDest", "ChatScreenStateful", "chat_selector", "chat_user_list" -> {
              val targetRoute = if (baseUIState.userRole == UserRole.StandardUser) "chat_selector" else "chat_user_list"
              onSubModuleChange(targetRoute)
            }

            "MainServiceScreenDest", "ServiceListScreen", "finance_selector" -> {
              onSubModuleChange("finance_selector")
            }
          }
        },
        icon = {
          BadgedBox(
            badge = {
              // Проверка бейджа непрочитанных чатов для вывода красной сферы
              val isChatRoute = destination.route == "ChatScreenDest" ||
                destination.route == "ChatScreenStateful" ||
                destination.route == "chat_selector" ||
                destination.route == "chat_user_list" ||
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
              contentDescription = null,
              modifier = Modifier.size(24.dp) // ИСПРАВЛЕНО: Вернули полноценный КРУПНЫЙ размер иконки 24.dp!
            )
          }
        },
        label = {
          // ИСПРАВЛЕНО: Вернули стандартный, крупный и отлично читаемый шрифт bodyMedium вместо этикеточного!
          Text(
            text = stringResource(destination.labelId),
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            overflow = TextOverflow.Ellipsis
          )
        },
        alwaysShowLabel = false
      )
    }
  }
}








