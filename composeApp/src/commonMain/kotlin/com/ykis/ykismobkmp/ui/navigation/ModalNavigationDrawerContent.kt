package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "ModalNavigationDrawerContent"

/**
 * [ModalNavigationDrawerContent] — Кроссплатформенный боковой слайдер управления лицевыми счетами.
 * ИСПРАВЛЕНО: Убран вызов .route, импортирован DrawerItemContent, зафиксирована Lazy-команда items.
 */
@Composable
fun ModalNavigationDrawerContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  selectedDestination: String,
  navigateToDestination: (String) -> Unit,
  onMenuClick: () -> Unit = {},
  navigateToApartment: (Long) -> Unit,
  isApartmentsEmpty: Boolean
) {
  val methodName = "DrawerContent"
  val keyboardController = LocalSoftwareKeyboardController.current

  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()

  val searchQuery by apartmentScreenModel.searchQuery.collectAsState()
  val houses by apartmentScreenModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()
  val filteredResults by apartmentScreenModel.filteredApartments.collectAsState()
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()

  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val isOrgAdmin = baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.OsbbUser
  val listMode = baseUIState.listMode

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
      // 1. ШАПКА: ПОИСК ИЛИ КНОПКА ДОБАВЛЕНИЯ КВАРТИРЫ
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (isUserAdmin) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
              println("[$tag.$methodName]: [SEARCH_INPUT] $query")
              apartmentScreenModel.onSearchQueryChanged(query)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Пошук адреси чи о/р", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { apartmentScreenModel.onSearchQueryChanged("") }) {
                  Icon(Icons.Default.Close, null)
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
          )
        } else {
          Text(
            text = stringResource(Res.string.list_apartment),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            // ИСПРАВЛЕНО: .route удален, передаем имя дестинации строковым КМР-литералом
            onClick = {
              println("[$tag.$methodName]: [ADD_CLICK]")
              onMenuClick()
              navigateToDestination("AddApartmentScreen")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.AddHome, null)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(Res.string.add_appartment))
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

      // 2. СРЕДНЯЯ ЧАСТЬ: СПИСКИ БТИ С ДИНАМИЧЕСКИМИ БЕЙДЖАМИ ЧАТОВ ОСМД
      Column(modifier = Modifier.weight(1f)) {
        if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
          NavigationDrawerItem(
            label = { Text("Назад", fontWeight = FontWeight.Bold) },
            selected = false,
            icon = { Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(18.dp)) },
            onClick = {
              println("[$tag.$methodName]: [BACK_LEVEL] Поточний: $listMode")
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
              DrawerItemContent(
                apartment = item,
                isSelected = baseUIState.addressId == item.addressId,
                listMode = listMode,
                badgeCount = 0,
                onClick = {
                  keyboardController?.hide()
                  if (listMode == ListMode.HOUSES) {
                    println("[$tag.$methodName]: [SEARCH_SELECT_HOUSE] ID: ${item.addressId}")
                    apartmentScreenModel.onHouseSelected(item.addressId)
                  } else {
                    println("[$tag.$methodName]: [SEARCH_SELECT_APT] ID: ${item.addressId}")
                    navigateToApartment(item.addressId)
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
                    label = { Text(raion.raion) },
                    selected = baseUIState.selectedRaionId == raion.raionId,
                    icon = { Icon(Icons.Default.Map, null) },
                    onClick = {
                      println("[$tag.$methodName]: [SELECT_RAION] ${raion.raion}")
                      apartmentScreenModel.onRaionSelected(raion)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                  )
                }
              }
              ListMode.HOUSES -> {
                items(houses, key = { "h_${it.houseId}" }) { house ->
                  NavigationDrawerItem(
                    label = { Text(house.address) },
                    selected = baseUIState.selectedHouseId == house.houseId,
                    icon = { Icon(Icons.Default.Domain, null) },
                    onClick = {
                      println("[$tag.$methodName]: [SELECT_HOUSE] ${house.address}")
                      apartmentScreenModel.onHouseSelected(house.houseId)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                  )
                }
              }
              ListMode.APARTMENTS -> {
                val aptList = if (isOrgAdmin) drawerApartments else baseUIState.apartments
                items(aptList, key = { "a_${it.addressId}" }) { apt ->
                  DrawerItemContent(
                    apartment = apt,
                    isSelected = baseUIState.addressId == apt.addressId,
                    listMode = ListMode.APARTMENTS,
                    badgeCount = apartmentBadges[apt.addressId.toString()] ?: 0,
                    onClick = {
                      println("[$tag.$methodName]: [SELECT_APT] Final Choice: ${apt.addressId}")
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







