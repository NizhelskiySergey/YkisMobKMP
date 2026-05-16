package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ykis.ykismobkmp.domain.services.UserRole

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ ТИПОВ РЕСУРСОВ JETBRAINS:
import org.jetbrains.compose.resources.StringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "NavDestinations"

/**
 * [TopLevelDestination] — Модель метаданных для главных разделов меню ЮКИС.
 * ИСПРАВЛЕНО: Полностью очищена от Composable-зависимостей и интерфейсов Tab.
 */
data class TopLevelDestination(
  val route: String = "",
  val selectedIcon: ImageVector = Icons.Default.Adjust,
  val unselectedIcon: ImageVector = Icons.Default.Adjust,
  val labelId: StringResource,
  val alwaysVisible: Boolean
)

/**
 * [getChatRoute] — Определение целевого экрана чат-системы на основе роли сессии.
 */
fun getChatRoute(role: UserRole): String {
  return if (role == UserRole.StandardUser) "service_selector" else "UserListScreen"
}

/**
 * [getNavDestinations] — Сборка структуры навигации (Чистый Kotlin).
 */
fun getNavDestinations(role: UserRole): List<TopLevelDestination> {
  val chatRoute = getChatRoute(role)

  return listOf(
    TopLevelDestination(
      route = "InfoApartmentScreen",
      selectedIcon = Icons.Filled.Info,
      unselectedIcon = Icons.Outlined.Info,
      labelId = Res.string.info,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = "MeterScreen",
      selectedIcon = Icons.Default.Opacity,
      unselectedIcon = Icons.Default.WaterDrop,
      labelId = Res.string.meters,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = "ServiceListScreen",
      selectedIcon = Icons.Filled.Payments,
      unselectedIcon = Icons.Outlined.Payments,
      labelId = Res.string.accrued,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = chatRoute,
      selectedIcon = Icons.AutoMirrored.Filled.Chat,
      unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
      labelId = Res.string.chat,
      alwaysVisible = false
    ),
    TopLevelDestination(
      route = "SettingsScreen",
      selectedIcon = Icons.Default.Settings,
      unselectedIcon = Icons.Outlined.Settings,
      labelId = Res.string.settings,
      alwaysVisible = true
    )
  )
}
