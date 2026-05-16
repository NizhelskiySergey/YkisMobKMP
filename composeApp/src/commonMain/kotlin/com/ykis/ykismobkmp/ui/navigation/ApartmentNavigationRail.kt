package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // КРИТИЧЕСКИЙ КМР-ИМПОРТ: Разрешает List<T> вместо Int в items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject

// Импорты инфраструктуры, навигации, стейтов и моделей ЮКИС г. Южный
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode

import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ JETBRAINS:
import org.jetbrains.compose.resources.stringResource

private const val tag = "ApartmentNavigationRail"

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

/**
 * [ApartmentNavigationRail] — Адаптивное боковое меню управления жилым фондом ОСМД г. Южное.
 * ИСПРАВЛЕНО: Полная монолитная сборка, типы ID приведены к Long, убраны коллизии перегрузки null.
 */
@Composable
fun ApartmentNavigationRail(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  selectedDestination: String,
  navigateToDestination: (String) -> Unit = {},
  isRailExpanded: Boolean,
  onMenuClick: () -> Unit,
  navigateToApartment: (Long) -> Unit = {}, // ИСПРАВЛЕНО: Сквозной Long стандарт
  railWidth: Dp,
  isApartmentsEmpty: Boolean
) {
  val keyboardController = LocalSoftwareKeyboardController.current

  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()

  val searchQuery by apartmentScreenModel.searchQuery.collectAsState()
  val apartments by apartmentScreenModel.filteredApartments.collectAsState()
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()

  val houses by apartmentScreenModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()

  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val listMode = baseUIState.listMode
  val isOrgAdmin = baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.OsbbUser
  val raions = baseUIState.raions

  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }
  LaunchedEffect(totalUnread) {
    println("[$tag]: RailMenu: Total sum calculated: $totalUnread")
  }

  val apartmentBadges = remember(unreadCounts) {
    unreadCounts.map { (fullKey, count) ->
      val parts = fullKey.split("_")
      val addressId = parts.find { it.length >= 3 && it.all { char -> char.isDigit() } } ?: ""
      addressId to count
    }.filter { it.first.isNotEmpty() }.toMap()
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
      if (isRailExpanded) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
          if (isUserAdmin) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                  println("[$tag]: Rail: [SEARCH_INPUT] Query: $query")
                  apartmentScreenModel.onSearchQueryChanged(query)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                placeholder = { Text("Пошук...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { apartmentScreenModel.onSearchQueryChanged("") }) {
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
              onClick = { navigateToDestination("AddApartmentScreen") },
              modifier = Modifier.fillMaxWidth().height(40.dp),
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddHome, null, Modifier.size(18.dp))
                if (railWidth > 150.dp) {
                  Text(" Додати", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // --- 1. ВЕРХНЯЯ ЧАСТЬ (СПИСКИ + ПОИСК) ---
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (isRailExpanded) {
          if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
            TextButton(
              onClick = { apartmentScreenModel.goBackLevel() },
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
                RailItemContent(
                  title = item.address,
                  subtitle = item.nanim,
                  extraInfo = "о/р ${item.addressId}",
                  icon = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home,
                  isSelected = baseUIState.addressId == item.addressId,
                  onClick = {
                    keyboardController?.hide()
                    if (listMode == ListMode.HOUSES) {
                      apartmentScreenModel.onHouseSelected(item.addressId)
                    } else {
                      navigateToApartment(item.addressId)
                    }
                  }
                )
              }
            } else {
              when (listMode) {
                ListMode.RAIONS -> {
                  items(raions, key = { "r_${it.raionId}" }) { raion ->
                    RailItemContent(
                      title = raion.raion,
                      icon = Icons.Default.Map,
                      isSelected = baseUIState.selectedRaionId == raion.raionId,
                      onClick = { apartmentScreenModel.onRaionSelected(raion) }
                    )
                  }
                }
                ListMode.HOUSES -> {
                  items(houses, key = { "h_${it.houseId}" }) { house ->
                    RailItemContent(
                      title = house.house,
                      icon = Icons.Default.Domain,
                      isSelected = baseUIState.selectedHouseId == house.houseId,
                      onClick = { apartmentScreenModel.onHouseSelected(house.houseId) }
                    )
                  }
                }
                ListMode.APARTMENTS -> {
                  items(drawerApartments, key = { "f_${it.addressId}" }) { flat ->
                    val badgeCount = apartmentBadges[flat.addressId.toString()] ?: 0
                    RailItemContent(
                      title = "кв. ${flat.address}",
                      subtitle = flat.nanim,
                      icon = Icons.Default.Home,
                      isSelected = baseUIState.addressId == flat.addressId,
                      badgeCount = badgeCount,
                      onClick = { navigateToApartment(flat.addressId) }
                    )
                  }
                }
              }
            }
          }
        }
      }

      // --- 2. НИЖНЯЯ ЧАСТЬ (МЕНЮ ДЕСТИНАЦИЙ) ---
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .padding(bottom = 16.dp)
      ) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

        val navDestinations = getNavDestinations(role = baseUIState.userRole)

        navDestinations.forEach { destination: TopLevelDestination ->
          val shouldShow = destination.alwaysVisible || !isApartmentsEmpty

          if (shouldShow) {
            val isSelected = selectedDestination.substringBefore("/") == destination.route.substringBefore("/")

            NavigationRailItem(
              selected = isSelected,
              onClick = {
                println("[$tag]: Rail: [CLICK] Target: ${destination.route} | Role: ${baseUIState.userRole}")

                if (destination.route == "service_selector") {
                  // ИСПРАВЛЕНО: Добавлен каст во избежание неоднозначности вызова перегрузок (Overload ambiguity)
                  chatScreenModel.setSelectedService(null as String?)
                }

                navigateToDestination(destination.route)
              },
              icon = {
                BadgedBox(
                  badge = {
                    val isChatRoute = destination.route == "service_selector" ||
                      destination.route == "UserListScreen"

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
                    contentDescription = stringResource(destination.labelId)
                  )
                }
              },
              label = if (isRailExpanded) {
                { Text(stringResource(destination.labelId), fontSize = 11.sp) }
              } else null,
              alwaysShowLabel = false
            )
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
  Box(
    modifier = Modifier
      .padding(horizontal = 8.dp, vertical = 2.dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent)
      .clickable { onClick() }
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
          Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
          if (extraInfo != null) {
            Text(" $extraInfo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
          }
        }
        subtitle?.let {
          Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
      }
      if (badgeCount > 0) {
        Badge { Text(badgeCount.toString()) }
      }
    }
  }
}
