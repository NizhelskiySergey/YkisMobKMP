package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity // Твой оригинальный класс жилого фонда
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ModalNavigationDrawerContent"

/**
 * [DrawerItemContent] — Элемент верстки лицевого счета БТИ внутри скользящего меню смартфона.
 */
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
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
          ) {
            Text(apartment.address, fontWeight = FontWeight.Bold)
            Text("| о/р ${apartment.addressId}", style = MaterialTheme.typography.labelSmall)
          }

          if (listMode == ListMode.APARTMENTS) {
            apartment.nanim?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
          }
        }
        if (badgeCount > 0) {
          Badge(containerColor = MaterialTheme.colorScheme.error) { Text(badgeCount.toString()) }
        }
      }
    },
    selected = isSelected,
    onClick = onClick,
    icon = {
      Icon(
        if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home,
        null
      )
    },
    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
  )
}

/**
 * [ModalNavigationDrawerContent] — Кроссплатформенная панель слайдера (Drawer) для смартфонов ЮКИС.
 */
@Composable
fun ModalNavigationDrawerContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  selectedDestination: String,
  navigateToDestination: (String) -> Unit,
  onMenuClick: () -> Unit = {},
  navigateToApartment: (Long) -> Unit, // Сквозной Long стандарт из BaseUIState под каноны SQLDelight
  isApartmentsEmpty: Boolean
) {
  val methodName = "DrawerContent"
  val keyboardController = LocalSoftwareKeyboardController.current

  // Нативная КМР инжекция ScreenModels вместо Android ViewModel YkisMobKMP
  val apartmentScreenModel = koinInject<ApartmentScreenModel>()
  val chatScreenModel = koinInject<ChatScreenModel>()

  val searchQuery by apartmentScreenModel.searchQuery.collectAsState()
  val houses by apartmentScreenModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentScreenModel.drawerApartments.collectAsState()
  val filteredResults by apartmentScreenModel.filteredApartments.collectAsState()

  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val isOrgAdmin =
    baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.OsbbUser
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val listMode = baseUIState.listMode

  // Реактивный парсинг и суммирование бейджей ГИОЦ по лицевым счетам
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
    // Главный монолитный контейнер шторки на всю высоту экрана смартфона
    Column(modifier = Modifier.fillMaxSize()) {

      // --- 1. ШАПКА: ПОИСК СЛУЖБ ИЛИ ДОБАВЛЕНИЕ Л/С ---
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (isUserAdmin) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
              println("[$className.$methodName]: [SEARCH_INPUT] Поисковый ввод админа: $query")
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
            onClick = {
              println("[$className.$methodName]: [ADD_CLICK] Переход на привязку БТИ квартиры")
              onMenuClick()
              navigateToDestination("AddApartmentScreen")
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

        // Кнопка НАЗАД (Только для многоуровневых служб водоканала/теплосети)
        if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
          NavigationDrawerItem(
            label = { Text("Назад", fontWeight = FontWeight.Bold) },
            selected = false,
            icon = { Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(18.dp)) },
            onClick = {
              println("[$className.$methodName]: [BACK_LEVEL] Запрос возврата. Текущий слой: $listMode")
              apartmentScreenModel.goBackLevel()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
          HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
          if (searchQuery.isNotEmpty()) {
            // --- РЕЖИМ АКТИВНОГО ФИЛЬТРА / ПОИСКА ---
            items(filteredResults, key = { "search_${it.addressId}" }) { item ->
              DrawerItemContent(
                apartment = item,
                isSelected = baseUIState.addressId == item.addressId,
                listMode = listMode,
                badgeCount = 0,
                onClick = {
                  keyboardController?.hide()
                  if (listMode == ListMode.HOUSES) {
                    println("[$className.$methodName]: [SEARCH_SELECT_HOUSE] Выбран дом ID: ${item.addressId}")
                    apartmentScreenModel.onHouseSelected(item.addressId)
                  } else {
                    println("[$className.$methodName]: [SEARCH_SELECT_APT] Фиксация о/р квартиры ID: ${item.addressId}")
                    navigateToApartment(item.addressId)
                    onMenuClick()
                  }
                }
              )
            }
          } else {
            // --- ОБЫЧНЫЙ СТАТИЧЕСКИЙ РЕЖИМ ЖКХ-ФИЛЬТРАЦИИ ---
            when (listMode) {
              ListMode.RAIONS -> {
                items(baseUIState.raions, key = { "r_${it.raionId}" }) { raion ->
                  NavigationDrawerItem(
                    label = { Text(raion.raion ?: "") },
                    selected = baseUIState.selectedRaionId == raion.raionId,
                    icon = { Icon(Icons.Default.Map, null) },
                    onClick = {
                      println("[$className.$methodName]: [SELECT_RAION] Клик по району: ${raion.raion}")
                      apartmentScreenModel.onRaionSelected(raion)
                    },
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
                    onClick = {
                      println("[$className.$methodName]: [SELECT_HOUSE] Клик по дому ID: ${house.houseId}")
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
                      println("[$className.$methodName]: [SELECT_APT] Клик по квартире о/р Long: ${apt.addressId}")
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
      } // Конец Column(modifier = Modifier.weight(1f))


      Column(
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .padding(bottom = 16.dp)
      ) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        NavigationDrawerItem(
          label = { Text("Налаштування", fontWeight = FontWeight.SemiBold) },
          selected = selectedDestination == "SettingsScreenDest",
          icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
          onClick = {
            println("[$className.$methodName]: [SETTINGS_CLICK] Клик по системным настройкам профиля.")
            keyboardController?.hide()
            onMenuClick()
            navigateToDestination("SettingsScreenDest")
          },
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          )
        )
      }
    }
  }

}
