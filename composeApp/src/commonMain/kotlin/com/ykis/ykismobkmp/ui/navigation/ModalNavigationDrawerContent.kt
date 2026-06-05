package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import org.koin.compose.koinInject

private const val className = "ModalNavigationDrawerContent"

@Composable
fun ModalNavigationDrawerContent(
  baseUIState: BaseUIState,
  navigator: Navigator,
  activeSubModule: String,
  onSubModuleChange: (String) -> Unit,
  onMenuClick: () -> Unit,
  navigateToApartment: (Long) -> Unit,
  isApartmentsEmpty: Boolean
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val selectedDrawerApartmentFocusRequester = remember { FocusRequester() }

  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()

  val searchQuery by apartmentScreenModel.searchQuery.collectAsState()
  val apartments by apartmentScreenModel.filteredApartments.collectAsState()
  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  
  val drawerHouses by apartmentScreenModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()

  LaunchedEffect(Unit) {
    if (!isApartmentsEmpty) {
      focusManager.clearFocus()
      selectedDrawerApartmentFocusRequester.requestFocus()
    }
  }

  DisposableEffect(activeSubModule) {
    onDispose {
      focusManager.clearFocus()
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

  ModalDrawerSheet(
    modifier = Modifier.width(320.dp),
    drawerContainerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (isUserAdmin) {
          Row(verticalAlignment = Alignment.CenterVertically) {
             if (baseUIState.listMode != ListMode.RAIONS) {
                IconButton(onClick = { apartmentScreenModel.goBackLevel() }) {
                   Icon(Icons.Default.ArrowBackIosNew, "Back")
                }
             }
             OutlinedTextField(
               value = searchQuery,
               onValueChange = { apartmentScreenModel.onSearchQueryChanged(it) },
               modifier = Modifier.weight(1f),
               placeholder = { Text("Пошук...", fontSize = 14.sp) },
               leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
               trailingIcon = {
                 if (searchQuery.isNotEmpty()) {
                   IconButton(onClick = { apartmentScreenModel.onSearchQueryChanged("") }) {
                     Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                   }
                 }
               },
               singleLine = true,
               shape = RoundedCornerShape(12.dp)
             )
          }
        } else {
          NavigationDrawerItem(
            label = { Text("Додати особовий рахунок") },
            selected = activeSubModule == "AddApartmentScreen",
            onClick = {
              onSubModuleChange("AddApartmentScreen")
              onMenuClick()
            },
            icon = { Icon(Icons.Default.Add, null) },
            shape = RoundedCornerShape(12.dp)
          )
        }
      }

      HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

      LazyColumn(
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        when (baseUIState.listMode) {
          ListMode.RAIONS -> {
            items(baseUIState.raions) { raion ->
              DrawerApartmentItem(
                title = raion.raion,
                subtitle = "Одеська обл.",
                extraInfo = "ID: ${raion.raionId}",
                icon = Icons.Default.Map,
                isSelected = baseUIState.selectedRaionId == raion.raionId,
                badgeCount = 0,
                onClick = { apartmentScreenModel.onRaionSelected(raion) }
              )
            }
          }
          ListMode.HOUSES -> {
            items(drawerHouses) { house ->
              DrawerApartmentItem(
                title = house.house,
                subtitle = "Житловий фонд",
                extraInfo = "ID: ${house.houseId}",
                icon = Icons.Default.LocationCity,
                isSelected = baseUIState.selectedHouseId == house.houseId,
                badgeCount = 0,
                onClick = { apartmentScreenModel.onHouseSelected(house.houseId) }
              )
            }
          }
          ListMode.APARTMENTS -> {
            val source = if (baseUIState.userRole == UserRole.StandardUser) apartments else drawerApartments
            if (source.isEmpty()) {
              item {
                 Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                     Text("Список порожній", style = MaterialTheme.typography.bodyMedium)
                 }
              }
            } else {
              items(source, key = { it.addressId }) { apartment ->
                val isSelected = apartment.addressId == baseUIState.addressId
                val badgeCount = apartmentBadges[apartment.addressId.toString()] ?: 0

                DrawerApartmentItem(
                  title = "кв. ${apartment.address ?: ""}",
                  subtitle = apartment.nanim,
                  extraInfo = "о/р ${apartment.addressId}",
                  icon = Icons.Default.Home,
                  isSelected = isSelected,
                  badgeCount = badgeCount,
                  focusRequester = if (isSelected) selectedDrawerApartmentFocusRequester else null,
                  onClick = {
                    keyboardController?.hide()
                    onMenuClick()
                    navigateToApartment(apartment.addressId)
                  }
                )
              }
            }
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
      
      NavigationDrawerItem(
        label = { Text("Вихід") },
        selected = false,
        onClick = { /* Логика выхода */ },
        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp)
      )
    }
  }
}

@Composable
fun DrawerApartmentItem(
  title: String,
  subtitle: String?,
  extraInfo: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Home,
  isSelected: Boolean,
  badgeCount: Int,
  focusRequester: FocusRequester? = null,
  onClick: () -> Unit
) {
  NavigationDrawerItem(
    label = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Text(extraInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        if (badgeCount > 0) {
          Surface(
            color = MaterialTheme.colorScheme.error,
            shape = CircleShape,
            modifier = Modifier.size(24.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onError
              )
            }
          }
        }
      }
    },
    selected = isSelected,
    onClick = onClick,
    icon = { Icon(icon, null) },
    modifier = Modifier.then(if (focusRequester != null) Modifier.focusRequester(focusRequester).focusTarget() else Modifier),
    shape = RoundedCornerShape(12.dp),
    colors = NavigationDrawerItemDefaults.colors(
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
      selectedIconColor = MaterialTheme.colorScheme.primary,
      selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
  )
}
