package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity // Твой оригинальный класс жилого фонда
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import ykismobkmp.composeapp.generated.resources.*
private const val className = "ModalNavigationDrawerContent"

@Composable
fun ModalNavigationDrawerContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  navigator: Navigator, // Единственный навигатор верхнего уровня (для системных окон)
  activeSubModule: String, // ДОБАВЛЕНО НАМЕРТВО: Сквозной стейт активного подмодуля Хаба ЮКІС
  onSubModuleChange: (String) -> Unit, // ДОБАВЛЕНО НАМЕРТВО: Сквозной коллбек смены кадра
  onMenuClick: () -> Unit = {}, // Закрытие шторки drawerState.close()
  navigateToApartment: (Long) -> Unit,
  isApartmentsEmpty: Boolean
) {
  val methodName = "DrawerContent"
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  // Сквозные КМР-координаторы фокуса для принудительного сброса курсора в Android/iOS
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

  // ИСПРАВЛЕНО НАМЕРТВО: Атомарный глушитель авто-фокуса при ПЕРВОМ выдвижении шторки смартфона!
  // Как только шторка инициализируется в памяти, мы принудительно переносим фокус на вибраную квартиру,
  // полностью блокируя автоматический перехват курсора строкой поиска и исключая ложный вылет клавиатуры!
  LaunchedEffect(Unit) {
    println("[YkisLogKMP.$className.$methodName]: Шторка висунута. Блокування автоматичного фокусу пошуку.")
    focusManager.clearFocus()
    // Мягко уводим фокус на выбранную карточку квартиры БТИ
    selectedDrawerApartmentFocusRequester.requestFocus()
  }

  // Упреждающее снятие фокуса при переключении подмодулей шторки смартфона
  DisposableEffect(activeSubModule) {
    onDispose {
      println("[YkisLogKMP.$className.$methodName]: Зміна екрану смартфона. Примусове анулювання фокусу.")
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

      // --- ШАПКА: ПОИСК АДМИНА ИЛИ КНОПКА ДОБАВЛЕНИЯ КВАРТИРЫ ---
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (isUserAdmin) {
          // Флаг того, что админ РЕАЛЬНО нажал пальцем на строку для ввода текста
          var isSearchEditingActive by remember { mutableStateOf(false) }

          if (!isSearchEditingActive && searchQuery.isEmpty()) {
            // ЭТАП А: Статичное зеркало поля поиска.
            // Оно физически НЕ МОЖЕТ перехватить фокус и никогда не вызовет клавиатуру при открытии шторки!
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // Жесткая высота Material 3 поля ввода
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clickable {
                  println("[$className.$methodName]: Користувач свідомо активував режим пошуку.")
                  isSearchEditingActive = true
                }
                .padding(horizontal = 16.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                  text = "Пошук адреси чи о/р",
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                  style = MaterialTheme.typography.bodyLarge
                )
              }
            }
          } else {
            // ЭТАП Б: Настоящее поле ввода. Включается только по ручному клику!
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { query ->
                apartmentScreenModel.onSearchQueryChanged(query)
              },
              modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
              placeholder = { Text("Пошук адреси чи о/р", fontSize = 14.sp) },
              leadingIcon = { Icon(Icons.Default.Search, null) },
              trailingIcon = {
                IconButton(onClick = {
                  println("[$className.$methodName]: Скидання тексту пошуку та закриття режиму редагування.")
                  apartmentScreenModel.onSearchQueryChanged("")
                  isSearchEditingActive = false // Выходим из режима поиска, пряча TextField
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
                isSearchEditingActive = false // Закрываем режим ввода при тапе на Done
                focusManager.clearFocus()
                selectedDrawerApartmentFocusRequester.requestFocus()
              })
            )

            // Как только текстовое поле появилось на экране, принудительно зажигаем курсор в нем
            LaunchedEffect(Unit) {
              searchFocusRequester.requestFocus()
            }
          }
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
        // Кнопка НАЗАД для многоуровневых слоев ЖКХ
        if (listMode != ListMode.RAIONS && isOrgAdmin && searchQuery.isEmpty()) {
          NavigationDrawerItem(
            label = { Text("Назад", fontWeight = FontWeight.Bold) },
            selected = false,
            icon = { Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(18.dp)) },
            onClick = {
              println("[$className.$methodName]: [BACK_LEVEL] Запрос возврата. Текущий слой: $listMode")
              focusManager.clearFocus()
              selectedDrawerApartmentFocusRequester.requestFocus()
              apartmentScreenModel.goBackLevel()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
          HorizontalDivider()
        }


        // --- ЛЕНТА КВАРТИР И ДОМОВ ---
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(vertical = 8.dp)
        ) {
          if (searchQuery.isNotEmpty()) {
            items(filteredResults, key = { "search_${it.addressId}" }) { item ->
              val isSelected = baseUIState.addressId == item.addressId

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .then(
                    if (isSelected) {
                      Modifier
                        .focusRequester(selectedDrawerApartmentFocusRequester)
                        .focusTarget()
                    } else {
                      Modifier
                    }
                  )
              ) {
                DrawerItemContent(
                  apartment = item,
                  isSelected = isSelected,
                  listMode = listMode,
                  badgeCount = 0,
                  onClick = {
                    println("[$className.$methodName]: Клік по елементу пошуку шторки. Анулювання курсору.")
                    focusManager.clearFocus()
                    selectedDrawerApartmentFocusRequester.requestFocus()
                    keyboardController?.hide()

                    if (listMode == ListMode.HOUSES) {
                      println("[$className.$methodName]: [SEARCH_SELECT_HOUSE] Выбран дом ID: ${item.addressId}")
                      apartmentScreenModel.onHouseSelected(item.addressId)
                    } else {
                      println("[$className.$methodName]: [SEARCH_SELECT_APT] Фиксация о/р квартиры ID: ${item.addressId}")
                      // ИСПРАВЛЕНО: Принудительный переход на экран Инфо при выборе квартиры из поиска
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

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .then(
                        if (isSelected) {
                          Modifier
                            .focusRequester(selectedDrawerApartmentFocusRequester)
                            .focusTarget()
                        } else {
                          Modifier
                        }
                      )
                  ) {
                    NavigationDrawerItem(
                      label = { Text(raion.raion ?: "") },
                      selected = isSelected,
                      icon = { Icon(Icons.Default.Map, null) },
                      onClick = {
                        println("[$className.$methodName]: [SELECT_RAION] Клик по району: ${raion.raion}")
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

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .then(
                        if (isSelected) {
                          Modifier
                            .focusRequester(selectedDrawerApartmentFocusRequester)
                            .focusTarget()
                        } else {
                          Modifier
                        }
                      )
                  ) {
                    NavigationDrawerItem(
                      label = { Text(house.house ?: "") },
                      selected = isSelected,
                      icon = { Icon(Icons.Default.Domain, null) },
                      onClick = {
                        println("[$className.$methodName]: [SELECT_HOUSE] Клик по дому ID: ${house.houseId}")
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

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      // Прошиваем выбранную админ-карточку БТИ фокус-маркером во избежание залипания клавиатуры
                      .then(
                        if (isSelected) {
                          Modifier
                            .focusRequester(selectedDrawerApartmentFocusRequester)
                            .focusTarget()
                        } else {
                          Modifier
                        }
                      )
                  ) {
                    DrawerItemContent(
                      apartment = apt,
                      isSelected = isSelected,
                      listMode = ListMode.APARTMENTS,
                      badgeCount = badgeCount,
                      onClick = {
                        println("[$className.$methodName]: [SELECT_APT] Клик по квартире о/р Long: ${apt.addressId}")
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

/**
 * [DrawerItemContent] — Внутрішній компонент отрисовки ячейки будинку або квартири ЮКІС на базі NavigationDrawerItem
 */
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
          ) {
            Text(
              text = apartment.address ?: "",
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "| о/р ${apartment.addressId}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }

          if (listMode == ListMode.APARTMENTS) {
            apartment.nanim?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        if (badgeCount > 0) {
          Badge(
            containerColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 4.dp)
          ) {
            Text(text = badgeCount.toString(), fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    selected = isSelected,
    onClick = {
      println("[YkisLogKMP.DrawerItemContent]: Клік по адресі у шторці смартфона. Скидання фокусу пошуку.")
      // ІСПРАВЛЕНО: Примусово знімаємо фокус введення в OutlinedTextField при тапі на будь-яку квартиру шторки
      focusManager.clearFocus()
      onClick()
    },
    icon = {
      Icon(
        imageVector = if (listMode == ListMode.HOUSES) Icons.Default.Domain else Icons.Default.Home,
        contentDescription = null
      )
    },
    modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
  )
}






