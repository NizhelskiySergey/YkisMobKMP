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

/**
 * [BottomNavigationBar] — Мобильная нижняя панель навигации ЮКИС Material 3.
 * Полностью переписана на каноничное управление Voyager-стеком навигатора верхнего уровня!
 */
@Composable
fun BottomNavigationBar(
  navigator: Navigator, // ИСПРАВЛЕНО НАМЕРТВО: Принимаем чистый навигатор Voyager взамен текстовых строк
  baseUIState: BaseUIState // Используем для динамической ролевой фильтрации вкладок жильца/админа
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
      // ИСПРАВЛЕНО НАМЕРТВО: Вычисляем активность вкладки на основе типа текущего экрана в стеке Voyager
      val isSelected = when (destination.route) {
        "InfoApartmentScreen", "UserListScreen" -> {
          navigator.lastItem is InfoApartmentScreenDest || navigator.lastItem is AdminUserListScreenDest
        }
        "service_selector" -> navigator.lastItem is MainMeterScreenDest
        "ChatScreenStateful" -> navigator.lastItem is ChatScreenDest
        else -> false
      }

      NavigationBarItem(
        selected = isSelected,
        onClick = {
          println("[$className.BottomNavigationBar]: Тап по нижней вкладке -> ${destination.route}")

          // ИСПРАВЛЕНО НАМЕРТВО: Переключаем экраны через замену корня стека на чистые Voyager-объекты из ScreensRegistry
          when (destination.route) {
            "InfoApartmentScreen", "UserListScreen" -> {
              val targetScreen = if (baseUIState.userRole == UserRole.StandardUser) {
                InfoApartmentScreenDest(baseUIState.addressId)
              } else {
                AdminUserListScreenDest
              }
              navigator.replaceAll(targetScreen)
            }
            "service_selector" -> {
              navigator.replaceAll(MainMeterScreenDest)
            }
            "ChatScreenStateful" -> {
              navigator.replaceAll(ChatScreenDest(chatId = baseUIState.uid))
            }
          }
        },
        icon = {
          BadgedBox(
            badge = {
              // 2. УНИВЕРСАЛЬНАЯ ПРОВЕРКА БЕЙДЖА ЧАТОВ
              val isChatRoute = destination.route == "service_selector" ||
                destination.route == "UserListScreen" // Ссылаемся на КМР-имя строки экрана

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


