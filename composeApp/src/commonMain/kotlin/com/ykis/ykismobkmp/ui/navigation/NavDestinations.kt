package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// Импорты инфраструктуры, ролей и кроссплатформенных ресурсов JetBrains Res
import com.ykis.ykismobkmp.domain.services.UserRole
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
 * [getNavDestinations] — Сборка структуры навигации (Чистый Kotlin для всех платформ KMP).
 * Логирование рантайма согласно правилу [Класс.Метод].
 */
fun getNavDestinations(role: UserRole): List<TopLevelDestination> {
  val chatRoute = getChatRoute(role)

  // Логирование согласно стандарту YkisMobKMP
  println("[$className.getNavDestinations]: Розрахунок дестинацій меню для ролі: $role | Чат-маршрут: $chatRoute")

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
      unselectedIcon = Icons.Default.ElectricMeter,
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

/**
 * [RouteRegistry] — Текстовые маркеры маршрутов для бесконфликтного переключения вкладок в when-контейнерах.
 */
object RouteRegistry {
  const val SIGN_IN = "SignInScreen"
  const val VERIFY_EMAIL = "VerifyEmailScreen"
  const val SIGN_UP = "SignUpScreen"
  const val ADD_APARTMENT = "AddApartmentScreen"
  const val METER = "MeterScreen"
  const val SERVICE_LIST = "ServiceListScreen"
  const val USER_LIST = "UserListScreen"
  const val SEND_IMAGE = "SendImageScreen"
  const val CAMERA = "CameraScreen"
  const val IMAGE_DETAIL = "ImageDetailScreen"
  const val CHAT = "ChatScreen"
  const val PROFILE = "ProfileScreen"
  const val SETTINGS = "SettingsScreen"
  const val BTI = "BtiScreen"
  const val FAMILY = "FamilyScreen"
  const val INFO_APARTMENT = "InfoApartmentScreen"
  const val WEB_VIEW = "WebViewScreen"
}

/**
 * КМР-константы аргументов пуш-уведомлений и транзакций ГИОЦ г. Южный.
 */
object YkisNavConstants {
  const val APARTMENT_SCREEN = "ApartmentScreen"
  const val WATER_SCREEN = "WaterScreen"
  const val SERVICE_DETAIL_SCREEN = "ServiceDetailScreen"
  const val ADDRESS_ID = "addressId"
  const val ADDRESS_DEFAULT_ID = "0"
  const val HOUSE_ID = "houseId"
  const val HOUSE_DEFAULT_ID = "0"
  const val SERVICE = "service"
  const val SERVICE_DEFAULT = "1"
  const val SERVICE_NAME = "serviceName"
  const val SERVICE_DEFAULT_NAME = "ОСББ"
  const val ADDRESS = "address"
  const val ADDRESS_DEFAULT = "адреса"
  const val ADDRESS_ID_ARG = "?$ADDRESS_ID={$ADDRESS_ID}"
  const val FLAT_ARG = "?$ADDRESS_ID={$ADDRESS_ID},$ADDRESS={$ADDRESS}"
  const val SERVICE_ARG = "?$ADDRESS_ID={$ADDRESS_ID},$ADDRESS={$ADDRESS},$HOUSE_ID={$HOUSE_ID},$SERVICE={$SERVICE},$SERVICE_NAME={$SERVICE_NAME}"
}
