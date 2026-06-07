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
  navigator: Navigator,
  baseUIState: BaseUIState,
  activeSubModule: String,
  onSubModuleChange: (String) -> Unit
) {
  val chatScreenModel = koinInject<ChatScreenModel>()
  val announcementModel = koinInject<com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel>()

  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val announcementState by announcementModel.uiState.collectAsState()

  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }
  val unreadAnnouncements = announcementState.unreadAnnouncementsCount

  NavigationBar(
    modifier = Modifier.fillMaxWidth().height(56.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    windowInsets = WindowInsets(0, 0, 0, 0)
  ) {
    val navDestinations = getNavDestinations(role = baseUIState.userRole)

    navDestinations.forEach { destination ->
      val isAptSelected = baseUIState.addressId != 0L
      val isAdmin = baseUIState.userRole != UserRole.StandardUser
      
      // Настройки, Чат и Объявления доступны всегда. Остальное только после выбора квартиры.
      val isSettingsRoute = destination.route == "SettingsScreen" || destination.route == "SettingsScreenDest"
      val isChatRoute = destination.route == "chat_user_list" || destination.route == "chat_selector"
      val isAnnounceRoute = destination.route == "announcements"

      val isEnabled = if (isAdmin && !isAptSelected) {
        isChatRoute || isSettingsRoute || isAnnounceRoute
      } else true

      val isSelected = when (destination.route) {
        "InfoApartmentScreenDest", "AdminUserListScreenDest", "InfoApartmentScreen", "UserListScreen" -> {
          activeSubModule == "InfoApartmentScreen" || activeSubModule == "UserListScreen"
        }
        "MainMeterScreenDest", "MeterScreen", "service_selector" -> activeSubModule == "service_selector"
        "ChatScreenDest", "ChatScreenStateful", "chat_selector", "chat_user_list" -> {
          activeSubModule == "chat_selector" || activeSubModule == "chat_user_list" || activeSubModule == "chat_room_active"
        }
        "MainServiceScreenDest", "ServiceListScreen", "finance_selector" -> activeSubModule == "finance_selector"
        "announcements" -> activeSubModule == "announcements"
        else -> false
      }

      NavigationBarItem(
        selected = isSelected,
        enabled = isEnabled,
        onClick = {
          when (destination.route) {
            "InfoApartmentScreenDest", "AdminUserListScreenDest", "InfoApartmentScreen", "UserListScreen" -> {
              onSubModuleChange("InfoApartmentScreen")
            }
            "MainMeterScreenDest", "MeterScreen", "service_selector" -> {
              chatScreenModel.setSelectedService(null as TotalServiceDebt?)
              onSubModuleChange("service_selector")
            }
            "ChatScreenDest", "ChatScreenStateful", "chat_selector", "chat_user_list" -> {
              val targetRoute = if (baseUIState.userRole == UserRole.StandardUser) "chat_selector" else "chat_user_list"
              onSubModuleChange(targetRoute)
            }
            "MainServiceScreenDest", "ServiceListScreen", "finance_selector" -> {
              onSubModuleChange("finance_selector")
            }
            "announcements" -> {
              onSubModuleChange("announcements")
            }
            "SettingsScreen", "SettingsScreenDest" -> {
              onSubModuleChange("SettingsScreenDest")
            }
          }
        },
        icon = {
          BadgedBox(
            badge = {
              val isChat = destination.route == "ChatScreenDest" ||
                destination.route == "ChatScreenStateful" ||
                destination.route == "chat_selector" ||
                destination.route == "chat_user_list" ||
                destination.route == "AdminUserListScreenDest" ||
                destination.route == "UserListScreen"
              
              val isAnnounce = destination.route == "announcements"

              if (isChat && totalUnread > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                  Text(text = if (totalUnread > 9) "9+" else totalUnread.toString())
                }
              } else if (isAnnounce && unreadAnnouncements > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                  Text(text = if (unreadAnnouncements > 9) "9+" else unreadAnnouncements.toString())
                }
              }
            }
          ) {
            Icon(
              imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
              contentDescription = null,
              modifier = Modifier.size(24.dp)
            )
          }
        },
        label = {
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
