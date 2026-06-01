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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * [ApartmentNavigationRail] — Нативное боковое КМР-меню для админов на Mac Desktop и планшетах.
 * Сбалансировано под управление внутренними подмодулями Единого Хаба расчетного центра ЮКІС.
 * Полностью переведено на каноничное управление Voyager-стеком навигатора верхнего уровня!
 */
@Composable
fun ApartmentNavigationRail(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  navigator: Navigator, // Единственный легитимный навигатор верхнего уровня
  activeSubModule: String,
  onSubModuleChange: (String) -> Unit,

  isRailExpanded: Boolean,
  onMenuClick: () -> Unit,
  navigateToApartment: (Long) -> Unit = {},
  railWidth: Dp,
  isApartmentsEmpty: Boolean
) {
  val keyboardController = LocalSoftwareKeyboardController.current

  val chatViewModel = koinInject<ChatScreenModel>()
  val apartmentViewModel = koinInject<ApartmentScreenModel>()

  val searchQuery by apartmentViewModel.searchQuery.collectAsState()
  val apartments by apartmentViewModel.filteredApartments.collectAsState()
  val isUserAdmin = baseUIState.userRole != UserRole.StandardUser
  val unreadCounts by chatViewModel.unreadCounts.collectAsState()
  val listMode = baseUIState.listMode
  val isOrgAdmin =
    baseUIState.userRole != UserRole.StandardUser && baseUIState.userRole != UserRole.OsbbUser
  val raions = baseUIState.raions

  val houses by apartmentViewModel.drawerHouses.collectAsState()
  val drawerApartments by apartmentViewModel.drawerApartments.collectAsState()
  val totalUnread = remember(unreadCounts) { unreadCounts.values.sum() }

  LaunchedEffect(totalUnread) {
    println("[$className.ApartmentNavigationRail]: Суммарный счетчик непрочитанных обновлен в фоне: $totalUnread")
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
                  println("[$className.ApartmentNavigationRail]: [SEARCH_INPUT] Ввод поискового запроса: $query")
                  apartmentViewModel.onSearchQueryChanged(query)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                placeholder = { Text("Пошук...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { apartmentViewModel.onSearchQueryChanged("") }) {
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
      // --- 1. ВЕРХНЯЯ ЧАСТЬ (СПИСКИ БТИ + ПОИСК) ---
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (isRailExpanded) {
          if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
            TextButton(
              onClick = {
                println("[$className.ApartmentNavigationRail]: Клик назад на предыдущий уровень БТИ")
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
                RailItemContent(
                  title = item.address ?: "",
                  subtitle = item.nanim,
                  extraInfo = "о/р ${item.addressId}",
                  icon = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home,
                  isSelected = baseUIState.addressId == item.addressId,
                  onClick = {
                    keyboardController?.hide()
                    if (listMode == ListMode.HOUSES) {
                      apartmentViewModel.onHouseSelected(item.addressId)
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
                      title = raion.raion ?: "",
                      icon = Icons.Default.Map,
                      isSelected = baseUIState.selectedRaionId == raion.raionId,
                      onClick = {
                        println("[$className.ApartmentNavigationRail]: Выбран район ID: ${raion.raionId}")
                        apartmentViewModel.onRaionSelected(raion)
                      }
                    )
                  }
                }

                ListMode.HOUSES -> {
                  items(houses, key = { "h_${it.houseId}" }) { house ->
                    RailItemContent(
                      title = house.house ?: "",
                      icon = Icons.Default.Domain,
                      isSelected = baseUIState.selectedHouseId == house.houseId,
                      onClick = {
                        println("[$className.ApartmentNavigationRail]: Выбран дом ID: ${house.houseId}")
                        apartmentViewModel.onHouseSelected(house.houseId)
                      }
                    )
                  }
                }

                ListMode.APARTMENTS -> {
                  val aptList = if (isOrgAdmin) drawerApartments else baseUIState.apartments
                  items(aptList, key = { "a_${it.addressId}" }) { apartment ->
                    val isSelected = baseUIState.addressId == apartment.addressId
                    val badgeCount = apartmentBadges[apartment.addressId.toString()] ?: 0

                    RailItemContent(
                      title = "кв. ${apartment.address ?: ""}",
                      subtitle = apartment.nanim,
                      extraInfo = "о/р ${apartment.addressId}",
                      icon = Icons.Default.Home,
                      isSelected = isSelected,
                      badgeCount = badgeCount,
                      onClick = {
                        keyboardController?.hide()
                        println("[$className.ApartmentNavigationRail]: Вибрана квартира о/р Long: ${apartment.addressId}")
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

      // --- 2. НИЖНЯЯ СИСТЕМНАЯ ЧАСТЬ (МЕНЮ БЕЗ ДЕСТРУКТИВНЫХ REPLACEALL) ---
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

        // Подсветка иконки чата активна на любом из 3 пошаговых этапов контура чат-системы
        val isChatSelected = activeSubModule == "chat_selector" || activeSubModule == "chat_user_list" || activeSubModule == "chat_room_active"

        // 1. Кнопка Главная (БТИ / Список абонентов диспетчера)
        NavigationRailItem(
          selected = isHomeSelected,
          onClick = {
            val targetRoute = if (baseUIState.userRole == UserRole.StandardUser) "InfoApartmentScreen" else "UserListScreen"
            onSubModuleChange(targetRoute)
          },
          icon = { Icon(Icons.Default.Home, null) },
          label = if (isRailExpanded) {
            { Text("Головна", fontSize = 11.sp) }
          } else null
        )

        // 2. Кнопка Финансы ЮКІС (Сводные балансы ГИОЦ)
        NavigationRailItem(
          selected = isFinanceSelected,
          onClick = {
            println("[$className.ApartmentNavigationRail]: Перехід на модуль комунальних нарахувань та заборгованостей.")
            onSubModuleChange("finance_selector")
          },
          icon = { Icon(Icons.Default.CreditCard, null) },
          label = if (isRailExpanded) { { Text("Фінанси", fontSize = 11.sp) } } else null
        )

        // 3. Кнопка Приборы учета ЮКІС (Ввод показаний водомеров)
        NavigationRailItem(
          selected = isMetersSelected,
          onClick = {
            // Исправлено: Связано с легитимным именем переменной chatScreenModel в ОЗУ!
            chatViewModel.setSelectedService(null as TotalServiceDebt?)
            onSubModuleChange("service_selector")
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
              Icon(Icons.Default.ElectricMeter, null)
            }
          },
          label = if (isRailExpanded) {
            { Text("Лічильники", fontSize = 11.sp) }
          } else null
        )

        // 4. Кнопка Чат обсуждения ОСББ
        NavigationRailItem(
          selected = isChatSelected,
          onClick = {
            // Исправлено: Клик переводит на "chat_selector" — первый слой выбора служб чата!
            println("[$className.ApartmentNavigationRail]: Перехід до модуля обговорень ЮКІС.")
            onSubModuleChange("chat_selector")
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
              Icon(Icons.Default.Chat, null)
            }
          },
          label = if (isRailExpanded) {
            { Text("Чат", fontSize = 11.sp) }
          } else null
        )

        // 5. Кнопка Системные Настройки профиля абонента
        NavigationRailItem(
          selected = activeSubModule == "SettingsScreenDest",
          onClick = {
            println("[$className.ApartmentNavigationRail]: Перехід на модуль системних налаштувань профілю.")
            onSubModuleChange("SettingsScreenDest")
          },
          icon = { Icon(Icons.Default.Settings, null) },
          label = if (isRailExpanded) { { Text("Налаштування", fontSize = 11.sp) } } else null
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






