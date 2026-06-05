package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import org.koin.compose.koinInject

private const val className = "ApartmentNavigationRail"

@Composable
fun ApartmentNavigationRail(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  navigator: Navigator,
  activeSubModule: String,
  onSubModuleChange: (String) -> Unit,
  isRailExpanded: Boolean,
  onMenuClick: () -> Unit,
  navigateToApartment: (Long) -> Unit,
  railWidth: Dp,
  isApartmentsEmpty: Boolean
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val selectedApartmentFocusRequester = remember { FocusRequester() }

  val chatViewModel = koinInject<ChatScreenModel>()
  val apartmentViewModel = koinInject<ApartmentScreenModel>()
  val announcementModel = koinInject<com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel>()

  val searchQuery by apartmentViewModel.searchQuery.collectAsState()
  val apartments by apartmentViewModel.filteredApartments.collectAsState()
  val unreadCounts by chatViewModel.unreadCounts.collectAsState()
  val announcementState by announcementModel.uiState.collectAsState()
  
  val drawerHouses by apartmentViewModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentViewModel.drawerApartments.collectAsState()

  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }
  val unreadAnnouncements = announcementState.unreadAnnouncementsCount

  LaunchedEffect(isApartmentsEmpty) {
    if (!isApartmentsEmpty) {
      focusManager.clearFocus()
      selectedApartmentFocusRequester.requestFocus()
    }
  }

  val apartmentBadges = remember(unreadCounts) {
    unreadCounts.map { (fullKey, count) ->
      val parts = fullKey.split("_")
      val addressId = parts.getOrNull(parts.size - 2) ?: ""
      addressId to count
    }.filter { it.first.isNotEmpty() }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.sum() }
  }

  val isInfoSelected = activeSubModule == "InfoApartmentScreen" || activeSubModule == "AddApartmentScreen"
  val isMeterSelected = activeSubModule == "service_selector"
  val isFinanceSelected = activeSubModule == "finance_selector"
  val isAnnouncementSelected = activeSubModule == "announcements"
  val isChatSelected = activeSubModule == "chat_selector" || activeSubModule == "chat_user_list" || activeSubModule == "chat_room_active"
  val isSettingsSelected = activeSubModule == "SettingsScreenDest"

  Surface(
    modifier = modifier.width(railWidth).fillMaxHeight(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isRailExpanded && baseUIState.listMode != ListMode.RAIONS && baseUIState.userRole != UserRole.StandardUser) {
           IconButton(onClick = { apartmentViewModel.goBackLevel() }) {
             Icon(Icons.Default.ArrowBackIosNew, "Back", modifier = Modifier.size(18.dp))
           }
        }
        Spacer(Modifier.weight(1f))
        IconButton(
          onClick = onMenuClick,
          modifier = Modifier.padding(vertical = 12.dp)
        ) {
          Icon(
            imageVector = if (isRailExpanded) Icons.Default.Menu else Icons.Default.Menu,
            contentDescription = "Toggle Rail",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (isRailExpanded) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { apartmentViewModel.onSearchQueryChanged(it) },
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          placeholder = { Text("Пошук...", fontSize = 14.sp) },
          leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      LazyColumn(
        modifier = Modifier.weight(1f).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        when (baseUIState.listMode) {
          ListMode.RAIONS -> {
            items(baseUIState.raions) { raion ->
               RailItemContent(
                 title = raion.raion,
                 subtitle = "Одеська обл.",
                 extraInfo = "ID: ${raion.raionId}",
                 icon = Icons.Default.Map,
                 isSelected = baseUIState.selectedRaionId == raion.raionId,
                 badgeCount = 0,
                 isExpanded = isRailExpanded,
                 onClick = { apartmentViewModel.onRaionSelected(raion) }
               )
            }
          }
          ListMode.HOUSES -> {
            items(drawerHouses) { house ->
               RailItemContent(
                 title = house.house,
                 subtitle = "Житловий фонд",
                 extraInfo = "ID: ${house.houseId}",
                 icon = Icons.Default.LocationCity,
                 isSelected = baseUIState.selectedHouseId == house.houseId,
                 badgeCount = 0,
                 isExpanded = isRailExpanded,
                 onClick = { apartmentViewModel.onHouseSelected(house.houseId) }
               )
            }
          }
          ListMode.APARTMENTS -> {
            val source = if (baseUIState.userRole == UserRole.StandardUser) apartments else drawerApartments
            items(source, key = { it.addressId }) { apartment ->
              val isSelected = apartment.addressId == baseUIState.addressId
              val badgeCount = apartmentBadges[apartment.addressId.toString()] ?: 0

              Box(
                modifier = Modifier.fillMaxWidth().then(
                  if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget()
                  else Modifier
                )
              ) {
                RailItemContent(
                  title = "кв. ${apartment.address}",
                  subtitle = apartment.nanim,
                  extraInfo = "о/р ${apartment.addressId}",
                  icon = Icons.Default.Home,
                  isSelected = isSelected,
                  badgeCount = badgeCount,
                  isExpanded = isRailExpanded,
                  onClick = {
                    focusManager.clearFocus()
                    selectedApartmentFocusRequester.requestFocus()
                    keyboardController?.hide()
                    navigateToApartment(apartment.addressId)
                  }
                )
              }
            }
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      Column(modifier = Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (baseUIState.userRole == UserRole.StandardUser) {
            RailDestinationItem(
              icon = Icons.Default.Info,
              label = "БТІ",
              isSelected = isInfoSelected,
              isExpanded = isRailExpanded,
              onClick = { onSubModuleChange("InfoApartmentScreen") }
            )
            RailDestinationItem(
              icon = Icons.Default.Opacity,
              label = "Лічильники",
              isSelected = isMeterSelected,
              isExpanded = isRailExpanded,
              onClick = { onSubModuleChange("service_selector") }
            )
            RailDestinationItem(
              icon = Icons.Default.CreditCard,
              label = "Фінанси",
              isSelected = isFinanceSelected,
              isExpanded = isRailExpanded,
              onClick = { onSubModuleChange("finance_selector") }
            )
        } else {
            // Для админов показываем кнопку возврата к выбору района
            RailDestinationItem(
              icon = Icons.Default.Domain,
              label = "Фонд",
              isSelected = false,
              isExpanded = isRailExpanded,
              onClick = { apartmentViewModel.resetToAdminMode() }
            )
        }
        
        RailDestinationItem(
          icon = Icons.Default.Campaign,
          label = "Оголошення",
          isSelected = isAnnouncementSelected,
          isExpanded = isRailExpanded,
          badgeCount = unreadAnnouncements,
          onClick = { onSubModuleChange("announcements") }
        )
        RailDestinationItem(
          icon = Icons.AutoMirrored.Filled.Chat,
          label = "Чат",
          isSelected = isChatSelected,
          isExpanded = isRailExpanded,
          badgeCount = totalUnread,
          onClick = {
            val target = if (baseUIState.userRole == UserRole.StandardUser) "chat_selector" else "chat_user_list"
            onSubModuleChange(target)
          }
        )
        RailDestinationItem(
          icon = Icons.Default.Settings,
          label = "Налаштування",
          isSelected = isSettingsSelected,
          isExpanded = isRailExpanded,
          onClick = { onSubModuleChange("SettingsScreenDest") }
        )
      }
    }
  }
}

@Composable
fun RailItemContent(
  title: String,
  subtitle: String?,
  extraInfo: String,
  icon: ImageVector,
  isSelected: Boolean,
  badgeCount: Int,
  isExpanded: Boolean,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      BadgedBox(
        badge = {
          if (badgeCount > 0) {
            Badge { Text(if (badgeCount > 9) "9+" else badgeCount.toString()) }
          }
        }
      ) {
        Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (isExpanded) {
        Spacer(Modifier.width(12.dp))
        Column {
          Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Text(extraInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
      }
    }
  }
}

@Composable
fun RailDestinationItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  isExpanded: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit
) {
  NavigationRailItem(
    selected = isSelected,
    onClick = onClick,
    icon = {
      BadgedBox(
        badge = {
          if (badgeCount > 0) {
            Badge { Text(if (badgeCount > 9) "9+" else badgeCount.toString()) }
          }
        }
      ) {
        Icon(icon, null)
      }
    },
    label = if (isExpanded) { { Text(label, fontSize = 11.sp) } } else null,
    alwaysShowLabel = false
  )
}
