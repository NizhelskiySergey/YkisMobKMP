package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel // Кроссплатформенная модель чатов

private const val className = "BottomNavigationBar"

/**
 * [BottomNavigationBar] — Мобильная нижняя панель навигации ЮКИС Material 3.
 */
@Composable
fun BottomNavigationBar(
  selectedDestination: String,
  onClick: (String) -> Unit,
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
      val isSelected = selectedDestination.substringBefore("/") == destination.route.substringBefore("/")

      NavigationBarItem(
        selected = isSelected,
        onClick = {
          println("[$className.BottomNavigationBar]: Тап по нижней вкладке -> ${destination.route}")
          onClick(destination.route)
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
              contentDescription = null // ИСПРАВЛЕНО: Убран ложный id =
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
