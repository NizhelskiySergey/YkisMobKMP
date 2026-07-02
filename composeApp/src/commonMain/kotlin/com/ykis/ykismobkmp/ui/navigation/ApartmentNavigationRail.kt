package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ykis.ykismobkmp.ui.screens.help.ManualScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ApartmentNavigationRail"

@Composable
fun CustomNavigationRail(
  currentWidth: Dp,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = contentColorFor(containerColor),
  header: @Composable (ColumnScope.() -> Unit)? = null,
  windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    modifier = modifier.fillMaxHeight().width(currentWidth),
  ) {
    Column(
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(windowInsets)
        .padding(vertical = 4.dp)
        .selectableGroup(),
    ) {
      if (header != null) {
        header()
        Spacer(Modifier.height(8.dp))
      }
      content()
    }
  }
}

@Composable
fun ApartmentNavigationRail(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  navigator: Navigator,
  activeSubModule: String,
  onSubModuleChange: (String) -> Unit,
  isRailExpanded: Boolean,
  onMenuClick: () -> Unit,
  navigateToApartment: (Long) -> Unit = {},
  railWidth: Dp,
  isApartmentsEmpty: Boolean
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val selectedApartmentFocusRequester = remember { FocusRequester() }

  val chatViewModel = koinInject<ChatScreenModel>()
  val apartmentViewModel = koinInject<ApartmentScreenModel>()

  val searchQuery by apartmentViewModel.searchQuery.collectAsState()
  val apartments by apartmentViewModel.filteredApartments.collectAsState()
  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val unreadCounts by chatViewModel.unreadCounts.collectAsState()
  
  val listMode = baseUIState.listMode
  // Org Admin або Osbb Admin — обидва можуть мати багаторівневе меню
  val isAnyAdmin = baseUIState.userRole != UserRole.StandardUser
  val raions = baseUIState.raions

  val houses by apartmentViewModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentViewModel.drawerApartments.collectAsState()

  val apartmentBadges = remember(unreadCounts) {
    unreadCounts.map { (fullKey, count) ->
      val parts = fullKey.split("_")
      val addressId = parts.getOrNull(parts.size - 2) ?: ""
      addressId to count
    }.filter { it.first.isNotEmpty() }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.sum() }
  }

  CustomNavigationRail(
    modifier = modifier,
    currentWidth = railWidth,
    header = {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
      ) {
        if (isRailExpanded && isUserAdmin) {
          val title = when (baseUIState.userRole) {
            UserRole.VodokanalUser -> stringResource(Res.string.vodokanal)
            UserRole.YtkeUser -> stringResource(Res.string.ytke)
            UserRole.TboUser -> stringResource(Res.string.yzhtrans)
            else -> baseUIState.osbb
          }
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
          )
        }
      }

      val searchFocusRequester = remember { FocusRequester() }

      DisposableEffect(isRailExpanded) {
        if (!isRailExpanded) {
          focusManager.clearFocus()
          selectedApartmentFocusRequester.requestFocus()
        }
        onDispose { }
      }

      if (isRailExpanded) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
          // ПОШУК доступний тільки для адмінів і ТІЛЬКИ в режимі списку квартир
          if (isUserAdmin && listMode == ListMode.APARTMENTS) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              var localSearchQuery by remember { mutableStateOf(searchQuery) }

              OutlinedTextField(
                value = localSearchQuery,
                onValueChange = { query ->
                  localSearchQuery = query
                  apartmentViewModel.onSearchQueryChanged(query)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .focusRequester(searchFocusRequester),
                placeholder = { Text(stringResource(Res.string.search_apartment_hint), fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                trailingIcon = {
                  if (localSearchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                      localSearchQuery = ""
                      apartmentViewModel.onSearchQueryChanged("")
                      focusManager.clearFocus()
                      selectedApartmentFocusRequester.requestFocus()
                    }) {
                      Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                    }
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
              )
            }
          } else if (!isUserAdmin) {
            FloatingActionButton(
              onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                selectedApartmentFocusRequester.requestFocus()
                onSubModuleChange("AddApartmentScreen")
              },
              modifier = Modifier.fillMaxWidth().height(40.dp),
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, Modifier.size(18.dp))
                if (railWidth > 150.dp) {
                  Text(
                    stringResource(Res.string.add_appartment),
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              }
            }
          }
        }
      }
    }
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 1. ОСНОВНИЙ СПИСОК (Займає вільне місце)
      Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        if (isRailExpanded) {
          Column(modifier = Modifier.fillMaxSize()) {
              // Кнопка НАЗАД
              val showBackInRail = remember(listMode, baseUIState.raions.size, houses.size, searchQuery) {
                val raionCount = baseUIState.raions.size
                val houseCount = houses.size
                searchQuery.isEmpty() && (
                  (listMode == ListMode.APARTMENTS && houseCount > 1) ||
                  (listMode == ListMode.HOUSES && raionCount > 1)
                )
              }

              if (showBackInRail) {
                  TextButton(
                    onClick = {
                      focusManager.clearFocus()
                      selectedApartmentFocusRequester.requestFocus()
                      apartmentViewModel.goBackLevel()
                    },
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                  ) {
                    Icon(Icons.Default.ArrowBackIosNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.back_button))
                  }
              }

              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
              ) {
                if (searchQuery.isNotEmpty()) {
                  items(apartments, key = { "search_${it.addressId}" }) { item ->
                    val isSelected = baseUIState.addressId == item.addressId
                    Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget() else Modifier)) {
                      RailItemContent(
                        title = item.address, subtitle = item.nanim, extraInfo = "| ${item.addressId}",
                        icon = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home,
                        isSelected = isSelected,
                        onClick = {
                          focusManager.clearFocus()
                          selectedApartmentFocusRequester.requestFocus()
                          keyboardController?.hide()
                          if (listMode == ListMode.HOUSES) apartmentViewModel.onHouseSelected(item.addressId)
                          else { onSubModuleChange("InfoApartmentScreen"); navigateToApartment(item.addressId) }
                        }
                      )
                    }
                  }
                } else {
                  when (listMode) {
                    ListMode.RAIONS -> {
                      items(raions, key = { "r_${it.raionId}" }) { raion ->
                        val isSelected = baseUIState.selectedRaionId == raion.raionId
                        Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget() else Modifier)) {
                          RailItemContent(title = raion.raion, icon = Icons.Default.Map, isSelected = isSelected,
                            onClick = { focusManager.clearFocus(); selectedApartmentFocusRequester.requestFocus(); apartmentViewModel.onRaionSelected(raion) }
                          )
                        }
                      }
                    }
                    ListMode.HOUSES -> {
                      items(houses, key = { "h_${it.houseId}" }) { house ->
                        val isSelected = baseUIState.selectedHouseId == house.houseId
                        Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget() else Modifier)) {
                          RailItemContent(title = house.house, icon = Icons.Default.Domain, isSelected = isSelected,
                            onClick = { focusManager.clearFocus(); selectedApartmentFocusRequester.requestFocus(); apartmentViewModel.onHouseSelected(house.houseId) }
                          )
                        }
                      }
                    }
                    ListMode.APARTMENTS -> {
                      val aptList = if (isAnyAdmin && drawerApartments.isNotEmpty()) drawerApartments else baseUIState.apartments
                      items(aptList, key = { "a_${it.addressId}" }) { apartment ->
                        val isSelected = baseUIState.addressId == apartment.addressId
                        val badgeCount = apartmentBadges[apartment.addressId.toString()] ?: 0
                        Box(modifier = Modifier.fillMaxWidth().then(if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget() else Modifier)) {
                          RailItemContent(title = apartment.address, subtitle = apartment.nanim, extraInfo = "| ${apartment.addressId}", icon = Icons.Default.Home, isSelected = isSelected, badgeCount = badgeCount,
                            onClick = { focusManager.clearFocus(); selectedApartmentFocusRequester.requestFocus(); keyboardController?.hide(); navigateToApartment(apartment.addressId) }
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

      // 2. НИЖНЯ ПАНЕЛЬ (Фіксована кнопка інструкції)
      Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        if (isRailExpanded) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(
              onClick = {
                focusManager.clearFocus()
                navigator.push(ManualScreen(role = baseUIState.userRole, onBackClick = { navigator.pop() }))
              },
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.HelpOutline, null, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(12.dp))
              Text("Інструкція", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            IconButton(
                onClick = { navigator.push(ManualScreen(role = baseUIState.userRole, onBackClick = { navigator.pop() })) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null)
            }
        }
      }
    }
  }
}


@Composable
fun RailItemContent(
  title: String,
  subtitle: String? = null,
  extraInfo: String? = null,
  icon: ImageVector,
  isSelected: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit
) {
  val focusManager = LocalFocusManager.current

  Box(
    modifier = Modifier
      .padding(horizontal = 8.dp, vertical = 2.dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent)
      .clickable {
        focusManager.clearFocus()
        onClick()
      }
      .padding(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(20.dp)
      )
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (extraInfo != null) {
            Text(
              " $extraInfo",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
        subtitle?.let {
          Text(
            it,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      if (badgeCount > 0) {
        Badge { Text(badgeCount.toString()) }
      }
    }
  }
}
