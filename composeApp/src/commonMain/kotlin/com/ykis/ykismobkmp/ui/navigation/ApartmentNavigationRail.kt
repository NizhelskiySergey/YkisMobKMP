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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.ykis.ykismobkmp.ui.screens.ledger.list.TotalServiceDebt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.add_appartment

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
  val announcementModel = koinInject<com.ykis.ykismobkmp.ui.screens.announcement.AnnouncementScreenModel>()

  val searchQuery by apartmentViewModel.searchQuery.collectAsState()
  val apartments by apartmentViewModel.filteredApartments.collectAsState()
  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val unreadCounts by chatViewModel.unreadCounts.collectAsState()
  val announcementState by announcementModel.uiState.collectAsState()
  
  val listMode = baseUIState.listMode
  val isOrgAdmin = baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.OsbbUser
  val raions = baseUIState.raions

  val houses by apartmentViewModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentViewModel.drawerApartments.collectAsState()
  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }
  val unreadAnnouncements = announcementState.unreadAnnouncementsCount

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
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onMenuClick) {
          Icon(Icons.Default.Menu, contentDescription = "Menu")
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
          if (isUserAdmin) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                  apartmentViewModel.onSearchQueryChanged(query)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .focusRequester(searchFocusRequester),
                placeholder = { Text("Пошук...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
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
          } else {
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
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (isRailExpanded) {
          if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
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
              Text("Назад")
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
          ) {
            if (searchQuery.isNotEmpty()) {
              items(apartments, key = { "search_${it.addressId}" }) { item ->
                val isSelected = baseUIState.addressId == item.addressId

                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .then(
                      if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget()
                      else Modifier
                    )
                ) {
                  RailItemContent(
                    title = item.address,
                    subtitle = item.nanim,
                    extraInfo = "| ${item.addressId}",
                    icon = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home,
                    isSelected = isSelected,
                    onClick = {
                      focusManager.clearFocus()
                      selectedApartmentFocusRequester.requestFocus()
                      keyboardController?.hide()
                      if (listMode == ListMode.HOUSES) {
                        apartmentViewModel.onHouseSelected(item.addressId)
                      } else {
                        onSubModuleChange("InfoApartmentScreen")
                        navigateToApartment(item.addressId)
                      }
                    }
                  )
                }
              }
            } else {
              when (listMode) {
                ListMode.RAIONS -> {
                  items(raions, key = { "r_${it.raionId}" }) { raion ->
                    val isSelected = baseUIState.selectedRaionId == raion.raionId

                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .then(
                          if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget()
                          else Modifier
                        )
                    ) {
                      RailItemContent(
                        title = raion.raion ?: "",
                        icon = Icons.Default.Map,
                        isSelected = isSelected,
                        onClick = {
                          focusManager.clearFocus()
                          selectedApartmentFocusRequester.requestFocus()
                          apartmentViewModel.onRaionSelected(raion)
                        }
                      )
                    }
                  }
                }

                ListMode.HOUSES -> {
                  items(houses, key = { "h_${it.houseId}" }) { house ->
                    val isSelected = baseUIState.selectedHouseId == house.houseId

                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .then(
                          if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget()
                          else Modifier
                        )
                    ) {
                      RailItemContent(
                        title = house.house ?: "",
                        icon = Icons.Default.Domain,
                        isSelected = isSelected,
                        onClick = {
                          focusManager.clearFocus()
                          selectedApartmentFocusRequester.requestFocus()
                          apartmentViewModel.onHouseSelected(house.houseId)
                        }
                      )
                    }
                  }
                }

                ListMode.APARTMENTS -> {
                  val aptList = if (isOrgAdmin) drawerApartments else baseUIState.apartments
                  items(aptList, key = { "a_${it.addressId}" }) { apartment ->
                    val isSelected = baseUIState.addressId == apartment.addressId
                    val badgeCount = apartmentBadges[apartment.addressId.toString()] ?: 0

                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .then(
                          if (isSelected) Modifier.focusRequester(selectedApartmentFocusRequester).focusTarget()
                          else Modifier
                        )
                    ) {
                      RailItemContent(
                        title = apartment.address,
                        subtitle = apartment.nanim,
                        extraInfo = "| ${apartment.addressId}",
                        icon = Icons.Default.Home,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
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
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .padding(bottom = 16.dp)
      ) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

        val isHomeSelected = activeSubModule == "InfoApartmentScreen" || activeSubModule == "UserListScreen"
        val isFinanceSelected = activeSubModule == "finance_selector"
        val isMetersSelected = activeSubModule == "service_selector"
        val isChatSelected = activeSubModule == "chat_selector" || activeSubModule == "chat_user_list" || activeSubModule == "chat_room_active"
        val isAnnouncementsSelected = activeSubModule == "announcements"

        val isAptSelected = baseUIState.addressId != 0L
        val isAdmin = baseUIState.userRole != UserRole.StandardUser

        NavigationRailItem(
          selected = isHomeSelected,
          enabled = if (isAdmin) isAptSelected else true,
          onClick = {
            focusManager.clearFocus()
            selectedApartmentFocusRequester.requestFocus()
            onSubModuleChange("InfoApartmentScreen")
          },
          icon = { Icon(Icons.Default.Home, null) },
          label = if (isRailExpanded) {
            { Text("Головна", fontSize = 11.sp, maxLines = 1, softWrap = false) }
          } else null
        )

        NavigationRailItem(
          selected = isFinanceSelected,
          enabled = if (isAdmin) isAptSelected else true,
          onClick = {
            focusManager.clearFocus()
            selectedApartmentFocusRequester.requestFocus()
            onSubModuleChange("finance_selector")
          },
          icon = { Icon(Icons.Default.CreditCard, null) },
          label = if (isRailExpanded) {
            { Text("Фінанси", fontSize = 11.sp, maxLines = 1, softWrap = false) }
          } else null
        )

        NavigationRailItem(
          selected = isMetersSelected,
          enabled = if (isAdmin) isAptSelected else true,
          onClick = {
            focusManager.clearFocus()
            selectedApartmentFocusRequester.requestFocus()
            chatViewModel.setSelectedService(null as TotalServiceDebt?)
            onSubModuleChange("service_selector")
          },
          icon = { Icon(Icons.Default.ElectricMeter, null) },
          label = if (isRailExpanded) {
            { Text("Лічильники", fontSize = 11.sp, maxLines = 1, softWrap = false) }
          } else null
        )

        NavigationRailItem(
          selected = isAnnouncementsSelected,
          enabled = true,
          onClick = {
            focusManager.clearFocus()
            selectedApartmentFocusRequester.requestFocus()
            onSubModuleChange("announcements")
          },
          icon = {
            BadgedBox(
              badge = {
                if (unreadAnnouncements > 0) {
                  Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text(text = if (unreadAnnouncements > 9) "9+" else unreadAnnouncements.toString())
                  }
                }
              }
            ) {
              Icon(Icons.Default.Campaign, null)
            }
          },
          label = if (isRailExpanded) {
            { Text("Оголошення", fontSize = 11.sp, maxLines = 1, softWrap = false) }
          } else null
        )

        NavigationRailItem(
          selected = isChatSelected,
          onClick = {
            focusManager.clearFocus()
            selectedApartmentFocusRequester.requestFocus()
            val targetRoute = if (baseUIState.userRole == UserRole.StandardUser) "chat_selector" else "chat_user_list"
            onSubModuleChange(targetRoute)
          },
          icon = {
            BadgedBox(
              badge = {
                if (totalUnread > 0) {
                  Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text(text = if (totalUnread > 9) "9+" else totalUnread.toString())
                  }
                }
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.Chat, null)
            }
          },
          label = if (isRailExpanded) {
            { Text("Чат", fontSize = 11.sp, maxLines = 1, softWrap = false) }
          } else null
        )

        NavigationRailItem(
          selected = activeSubModule == "SettingsScreenDest",
          enabled = true,
          onClick = {
            focusManager.clearFocus()
            selectedApartmentFocusRequester.requestFocus()
            onSubModuleChange("SettingsScreenDest")
          },
          icon = { Icon(Icons.Default.Settings, null) },
          label = if (isRailExpanded) {
            { Text("Налаштування", fontSize = 11.sp, maxLines = 1, softWrap = false) }
          } else null
        )
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
