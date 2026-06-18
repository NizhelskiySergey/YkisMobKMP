package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import ykismobkmp.composeapp.generated.resources.*

@Composable
fun ModalNavigationDrawerContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  navigator: Navigator,
  activeSubModule: String,
  onSubModuleChange: (String) -> Unit,
  onMenuClick: () -> Unit = {},
  navigateToApartment: (Long) -> Unit,
  isApartmentsEmpty: Boolean
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  val selectedDrawerApartmentFocusRequester = remember { FocusRequester() }
  val searchFocusRequester = remember { FocusRequester() }

  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()

  val searchQuery by apartmentScreenModel.searchQuery.collectAsState()
  val houses by apartmentScreenModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()
  val filteredResults by apartmentScreenModel.filteredApartments.collectAsState()

  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val isOrgAdmin = baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.OsbbUser
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val listMode = baseUIState.listMode

  LaunchedEffect(Unit) {
    if (!isApartmentsEmpty) {
      focusManager.clearFocus()
      selectedDrawerApartmentFocusRequester.requestFocus()
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
    modifier = modifier.width(320.dp),
    drawerContainerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (isUserAdmin) {
        val shortName = when (baseUIState.userRole) {
          UserRole.VodokanalUser -> "КП \"ЮЖВОДОКАНАЛ\""
          UserRole.YtkeUser -> "КП тм \"ЮТКЕ\""
          UserRole.TboUser -> "КП \"СПЕЦТРАНС\""
          UserRole.OsbbUser -> baseUIState.osbb.takeIf { it.isNotBlank() && it != "0" } ?: "ОСББ"
          else -> "Адмін"
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = shortName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          IconButton(onClick = { onMenuClick() }) { Icon(Icons.Default.Close, contentDescription = "Закрити") }
        }
      }

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // ПОШУК (Тільки в режимі квартир)
        if (isUserAdmin && listMode == ListMode.APARTMENTS) {
          
          // ФІКС: Пряме використання OutlinedTextField з мінімальними побічними ефектами
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
              apartmentScreenModel.onSearchQueryChanged(it) 
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Пошук о/р чи адреси", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = {
                  apartmentScreenModel.onSearchQueryChanged("")
                }) { Icon(Icons.Default.Close, null) }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
          )
        } else if (!isUserAdmin) {
          Button(
            onClick = {
              onMenuClick()
              onSubModuleChange("AddApartmentScreen")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.AddHome, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(Res.string.add_appartment))
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

      Column(modifier = Modifier.weight(1f)) {
        if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
          NavigationDrawerItem(
            label = { Text("Назад", fontWeight = FontWeight.Bold) },
            selected = false,
            icon = { Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(18.dp)) },
            onClick = { apartmentScreenModel.goBackLevel() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
          HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
          if (searchQuery.isNotEmpty()) {
            items(filteredResults, key = { "search_${it.addressId}" }) { item ->
              val isSelected = baseUIState.addressId == item.addressId
              DrawerItemContent(
                apartment = item, isSelected = isSelected, listMode = listMode, badgeCount = 0,
                onClick = {
                    keyboardController?.hide()
                    if (listMode == ListMode.HOUSES) apartmentScreenModel.onHouseSelected(item.addressId)
                    else {
                      navigateToApartment(item.addressId)
                      onSubModuleChange("InfoApartmentScreen")
                      onMenuClick()
                    }
                }
              )
            }
          } else {
            when (listMode) {
              ListMode.RAIONS -> {
                items(baseUIState.raions, key = { "r_${it.raionId}" }) { raion ->
                  NavigationDrawerItem(
                    label = { Text(raion.raion ?: "") },
                    selected = baseUIState.selectedRaionId == raion.raionId,
                    icon = { Icon(Icons.Default.Map, null) },
                    onClick = { apartmentScreenModel.onRaionSelected(raion) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                  )
                }
              }
              ListMode.HOUSES -> {
                items(houses, key = { "h_${it.houseId}" }) { house ->
                  NavigationDrawerItem(
                    label = { Text(house.house ?: "") },
                    selected = baseUIState.selectedHouseId == house.houseId,
                    icon = { Icon(Icons.Default.Domain, null) },
                    onClick = { apartmentScreenModel.onHouseSelected(house.houseId) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                  )
                }
              }
              ListMode.APARTMENTS -> {
                val aptList = if (isOrgAdmin) drawerApartments else baseUIState.apartments
                items(aptList, key = { "a_${it.addressId}" }) { apt ->
                  val isSelected = baseUIState.addressId == apt.addressId
                  DrawerItemContent(
                    apartment = apt, isSelected = isSelected, listMode = ListMode.APARTMENTS,
                    badgeCount = apartmentBadges[apt.addressId.toString()] ?: 0,
                    onClick = {
                        keyboardController?.hide()
                        navigateToApartment(apt.addressId)
                        onMenuClick()
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DrawerItemContent(
  apartment: ApartmentEntity,
  isSelected: Boolean,
  listMode: ListMode,
  badgeCount: Int,
  onClick: () -> Unit
) {
  NavigationDrawerItem(
    label = {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = apartment.address, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "| ${apartment.addressId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (listMode == ListMode.APARTMENTS) {
            apartment.nanim?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
          }
        }
        if (badgeCount > 0) {
          Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = if (badgeCount > 99) "99+" else badgeCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError)
            }
          }
        }
      }
    },
    selected = isSelected,
    onClick = onClick,
    icon = { Icon(imageVector = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home, contentDescription = null) },
    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
  )
}
