package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

// Импорты общих сущностей, стейтов и моделей ЮКИС г. Южный
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ JETBRAINS:
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "BottomNavigationBar"

/**
 * [BottomNavigationBar] — Кроссплатформенная нижняя панель навигации для смартфонов (Android/iOS).
 * Полностью очищена от Composable-конфликтов внутри remember.
 */
@Composable
fun BottomNavigationBar(
  modifier: Modifier = Modifier,
  selectedDestination: String,
  onClick: (String) -> Unit,
  baseUIState: BaseUIState // Используем стейт для динамического определения роли
) {
  // Инжектируем очищенную кроссплатформенную модель чатов через Koin KMP мост
  val chatScreenModel = koinInject<ChatScreenModel>()

  // 1. ИСПРАВЛЕНО: Чистый Kotlin вызов внутри remember, так как getNavDestinations больше не Composable
  val navDestinations = remember(baseUIState.userRole) {
    getNavDestinations(role = baseUIState.userRole)
  }

  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }

  NavigationBar(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.surfaceContainer
  ) {
    navDestinations.forEach { destination ->
      val isSelected = selectedDestination.substringBefore("/") == destination.route.substringBefore("/")

      NavigationBarItem(
        selected = isSelected,
        onClick = { onClick(destination.route) },
        icon = {
          BadgedBox(
            badge = {
              val isChatRoute = destination.route == "service_selector" ||
                destination.route == "user_list"

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
              // ВЫЗОВ @Composable ТЕПЕРЬ ПРОИСХОДИТ СТРОГО ТУТ (Внутри контента NavigationBarItem)
              contentDescription = stringResource(destination.labelId)
            )
          }
        },
        // ВЫЗОВ @Composable ТЕПЕРЬ ПРОИСХОДИТ СТРОГО ТУТ (Внутри контента NavigationBarItem)
        label = { Text(stringResource(destination.labelId)) },
        alwaysShowLabel = false
      )
    }
  }
}

/**
 * [getNavDestinations] — Чистая Kotlin функция без аннотации @Composable.
 * Возвращает только ссылки на ресурсы метаданных, не вызывая инфлейт интерфейса.
 */
private fun getNavDestinations(role: UserRole): List<NavDestination> {
  return listOf(
    NavDestination(
      route = "service_selector",
      labelId = Res.string.chats,
      selectedIcon = Icons.Default.Chat,
      unselectedIcon = Icons.Default.Chat
    ),
    NavDestination(
      route = "settings",
      labelId = Res.string.settings,
      selectedIcon = Icons.Default.Settings,
      unselectedIcon = Icons.Default.Settings,
      alwaysVisible = true
    )
  )
}
