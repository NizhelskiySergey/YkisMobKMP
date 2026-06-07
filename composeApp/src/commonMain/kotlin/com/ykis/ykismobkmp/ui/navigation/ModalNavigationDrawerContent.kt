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
          Text(
            text = shortName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = { onMenuClick() }) {
            Icon(Icons.Default.Close, contentDescription = "Закрити")
          }
        }
      }

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (isUserAdmin) 8.dp else 16.dp)) {
        if (isUserAdmin) {
          var isSearchEditingActive by remember { mutableStateOf(false) }

          if (!isSearchEditingActive && searchQuery.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clickable { isSearchEditingActive = true }
                .padding(horizontal = 16.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Spacer(Modifier.width(12.dp))
                Text(text = "Пошук адреси чи о/р", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
              }
            }
          } else {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { apartmentScreenModel.onSearchQueryChanged(it) },
              modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
              placeholder = { Text("Пошук адреси чи о/р", fontSize = 14.sp) },
              leadingIcon = { Icon(Icons.Default.Search, null) },
              trailingIcon = {
                IconButton(onClick = {
                  apartmentScreenModel.onSearchQueryChanged("")
                  isSearchEditingActive = false
                  focusManager.clearFocus()
                  selectedDrawerApartmentFocusRequester.requestFocus()
                }) {
                  Icon(Icons.Default.Close, null)
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = {
                isSearchEditingActive = false
                focusManager.clearFocus()
                selectedDrawerApartmentFocusRequester.requestFocus()
              })
            )
            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
          }
        } else {
          Text(text = stringResource(Res.string.list_apartment), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              selectedDrawerApartmentFocusRequester.requestFocus()
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
            onClick = {
              focusManager.clearFocus()
              selectedDrawerApartmentFocusRequester.requestFocus()
              apartmentScreenModel.goBackLevel()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
          HorizontalDivider()
        }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(vertical = 8.dp)
        ) {
          if (searchQuery.isNotEmpty()) {
            items(filteredResults, key = { "search_${it.addressId}" }) { item ->
              val isSelected = baseUIState.addressId == item.addressId
              Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedDrawerApartmentFocusRequester).focusTarget() else Modifier)) {
                DrawerItemContent(
                  apartment = item,
                  isSelected = isSelected,
                  listMode = listMode,
                  badgeCount = 0,
                  onClick = {
                    focusManager.clearFocus()
                    selectedDrawerApartmentFocusRequester.requestFocus()
                    keyboardController?.hide()
                    if (listMode == ListMode.HOUSES) apartmentScreenModel.onHouseSelected(item.addressId)
                    else {
                      onSubModuleChange("InfoApartmentScreen")
                      navigateToApartment(item.addressId)
                      onMenuClick()
                    }
                  }
                )
              }
            }
          } else {
            when (listMode) {
              ListMode.RAIONS -> {
                items(baseUIState.raions, key = { "r_${it.raionId}" }) { raion ->
                  val isSelected = baseUIState.selectedRaionId == raion.raionId
                  Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedDrawerApartmentFocusRequester).focusTarget() else Modifier)) {
                    NavigationDrawerItem(
                      label = { Text(raion.raion ?: "") },
                      selected = isSelected,
                      icon = { Icon(Icons.Default.Map, null) },
                      onClick = {
                        focusManager.clearFocus()
                        selectedDrawerApartmentFocusRequester.requestFocus()
                        apartmentScreenModel.onRaionSelected(raion)
                      },
                      modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                  }
                }
              }
              ListMode.HOUSES -> {
                items(houses, key = { "h_${it.houseId}" }) { house ->
                  val isSelected = baseUIState.selectedHouseId == house.houseId
                  Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedDrawerApartmentFocusRequester).focusTarget() else Modifier)) {
                    NavigationDrawerItem(
                      label = { Text(house.house ?: "") },
                      selected = isSelected,
                      icon = { Icon(Icons.Default.Domain, null) },
                      onClick = {
                        focusManager.clearFocus()
                        selectedDrawerApartmentFocusRequester.requestFocus()
                        apartmentScreenModel.onHouseSelected(house.houseId)
                      },
                      modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                  }
                }
              }
              ListMode.APARTMENTS -> {
                val aptList = if (isOrgAdmin) drawerApartments else baseUIState.apartments
                items(aptList, key = { "a_${it.addressId}" }) { apt ->
                  val isSelected = baseUIState.addressId == apt.addressId
                  val badgeCount = apartmentBadges[apt.addressId.toString()] ?: 0
                  Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedDrawerApartmentFocusRequester).focusTarget() else Modifier)) {
                    DrawerItemContent(
                      apartment = apt,
                      isSelected = isSelected,
                      listMode = ListMode.APARTMENTS,
                      badgeCount = badgeCount,
                      onClick = {
                        focusManager.clearFocus()
                        selectedDrawerApartmentFocusRequester.requestFocus()
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
}

@Composable
fun DrawerItemContent(
  modifier: Modifier = Modifier,
  apartment: ApartmentEntity,
  isSelected: Boolean,
  listMode: ListMode,
  badgeCount: Int,
  onClick: () -> Unit
) {
  val focusManager = LocalFocusManager.current
  NavigationDrawerItem(
    label = {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            Text(text = apartment.address, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "| ${apartment.addressId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
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
    onClick = { focusManager.clearFocus(); onClick() },
    icon = { Icon(imageVector = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home, contentDescription = null) },
    modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
  )
}
