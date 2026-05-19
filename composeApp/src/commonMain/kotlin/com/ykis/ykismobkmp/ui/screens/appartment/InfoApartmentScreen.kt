package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobPAM / YkisMobKMP
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentType

// ИМПОРТЫ ТВОИХ РЕАЛЬНЫХ ПАНЕЛЕЙ БТИ И СОСТАВА СЕМЬИ
import com.ykis.ykismobkmp.ui.screens.bti.BtiPanelContent
import com.ykis.ykismobkmp.ui.screens.family.FamilyContent

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ СТРОК JETBRAINS
import ykismobkmp.composeapp.generated.resources.*
import kotlin.collections.get

private const val className = "InfoApartmentScreen"

// ====================================================================
// --- ИЗОЛИРОВАННЫЕ VOYAGER ВКЛАДКИ ДЛЯ КОНТЕНТА (YkisMobPAM) ---
// ====================================================================

object BtiTab : Tab {
  override val options: TabOptions
    @Composable
    get() {
      val title = stringResource(Res.string.info)
      val icon = rememberVectorPainter(Icons.Default.Home)
      return remember { TabOptions(index = 0u, title = title, icon = icon) }
    }

  @Composable
  override fun Content() {
    // ИСПРАВЛЕНО НАМЕРТВО: Переменная названа строго apartmentScreenModel
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    BtiPanelContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
  }
}

object FamilyTab : Tab {
  override val options: TabOptions
    @Composable
    get() {
      val title = "Склад сім'ї"
      val icon = rememberVectorPainter(Icons.Default.People)
      return remember { TabOptions(index = 1u, title = title, icon = icon) }
    }

  @Composable
  override fun Content() {
    // ИСПРАВЛЕНО: Никаких ViewModel. Строго КМР-наименование apartmentScreenModel
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    FamilyContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
  }
}

/**
 * [InfoApartmentScreen] — Кроссплатформенный экран характеристик жилья БТИ ЮКИС.
 * ИСПРАВЛЕНО: Имя переменной тотально приведено к единственному стандарту apartmentScreenModel.
 */
class InfoApartmentScreen(
  private val onDrawerClicked: () -> Unit = {}
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    val adaptiveContentType = LocalContentType.current
    val adaptiveNavigationType = LocalNavigationType.current

    // ИСПРАВЛЕНО: Инжектируем под правильным КМР именем apartmentScreenModel!
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    val baseUIState by apartmentScreenModel.uiState.collectAsState()

    var showWarningDialog by remember { mutableStateOf(false) }

    // 1. ДИАЛОГ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ КВАРТИРЫ ИЗ ЛОКАЛЬНОГО ЖКХ УЧЕТА
    if (showWarningDialog) {
      AlertDialog(
        onDismissRequest = { showWarningDialog = false },
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(Res.string.delete_account_title)) },
        text = { Text("Ви дійсно хочете видалити цю квартиру з облікового запису? Дані неможливо буде відновити.") },
        dismissButton = {
          TextButton(onClick = { showWarningDialog = false }) {
            Text(stringResource(Res.string.cancel))
          }
        },
        // Внутри InfoApartmentScreen.kt в блоке AlertDialog -> confirmButton

        // Внутри InfoApartmentScreen.kt в блоке AlertDialog -> confirmButton

        confirmButton = {
          TextButton(onClick = {
            println("[$className.Content]: [ACTION] Підтверджено видалення особового рахунку для addressId: ${baseUIState.addressId}")

            apartmentScreenModel.deleteApartmentFromProfile(
              addressId = baseUIState.addressId,
              onNavigateToAddScreen = {
                println("[$className.Content]: Заміна кореня стеку Voyager на екран прив'язки квартири БТІ")

                // РЕШЕНИЕ: Передаем пустые лямбды в конструктор экрана, полностью удовлетворяя компилятор!
                navigator.replaceAll(
                  AddApartmentScreen(
                    onDrawerClicked = {},
                    closeContentDetail = {}
                  )
                )
              }
            )

            showWarningDialog = false
          }) {
            Text(
              text = stringResource(Res.string.delete_my_account),
              color = MaterialTheme.colorScheme.error
            )
          }
        }


      )
    }

    // 2. ПРЕДОХРАНИТЕЛЬ ОТ ЦИКЛИЧЕСКИХ ДУБЛИКАТОВ СЕТЕВЫХ ЗАГРУЗОК ГИОЦ
    LaunchedEffect(baseUIState.addressId, baseUIState.apartments.size, baseUIState.uid) {
      val methodName = "LaunchedEffect"
      val currentFirebaseUid = baseUIState.uid

      if (currentFirebaseUid.isNullOrBlank() || baseUIState.apartments.isEmpty()) {
        println("[$className.$methodName]: [WAIT] Дані профілю або список квартир БТИ ще не готові")
        return@LaunchedEffect
      }

      val targetId = if (baseUIState.addressId != 0L) baseUIState.addressId else baseUIState.apartments.firstOrNull()?.addressId ?: 0L

      // ИСПРАВЛЕНО: Вызов метода getApartment() на правильном КМР-объекте apartmentScreenModel
      if (targetId != 0L && targetId != apartmentScreenModel.lastLoadedAddressId) {
        println("[$className.$methodName]: [LOAD] Запит ГІОЦ даних для ID: $targetId | UID: $currentFirebaseUid")
        apartmentScreenModel.getApartment( addressId = targetId)
      } else {
        println("[$className.$methodName]: [SKIP] ID: $targetId вже є поточним. Циклічне завантаження запобіжено.")
      }
    }

    // Инициализируем нативный TabNavigator от Voyager
    TabNavigator(BtiTab) {
      val tabNavigator = LocalTabNavigator.current

      Scaffold(
        topBar = {
          DefaultAppBar(
            title = baseUIState.address,
            subtitle = " о/р ${baseUIState.addressId}",
            canNavigateBack = false,
            onDrawerClick = onDrawerClicked,
            navigationType = adaptiveNavigationType,
            actionButton = {
              if (baseUIState.userRole == UserRole.StandardUser) {
                IconButton(onClick = { showWarningDialog = true }) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          )
        }
      ) { innerPadding ->
        Column(
          modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
          if (adaptiveContentType == ContentType.DUAL_PANE) {
            // Широкоформатный режим (Mac Desktop / Планшеты) — выводим две панели параллельно
            InfoScreenDualPanelContent(
              baseUIState = baseUIState,
              apartmentScreenModel = apartmentScreenModel // ИСПРАВЛЕНО: Передаем правильный объект
            )
          } else {
            // Мобильный режим (Вкладки) со стабильными Skiko-индикаторами сдвига
            val tabs = remember { listOf(BtiTab, FamilyTab) }

            // Внутри InfoApartmentScreen.kt в мобильной ветке рендеринга PrimaryTabRow

            val activeIndex = tabNavigator.current.options.index.toInt()

            PrimaryTabRow(
              selectedTabIndex = activeIndex,
              containerColor = MaterialTheme.colorScheme.surface,
              divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) },
              // ИСПРАВЛЕНО: Современная КМР-лямбда индикатора без параметров!
              indicator = {
                TabRowDefaults.PrimaryIndicator(
                  // РЕШЕНИЕ: Нативно вызываем tabIndicatorOffset, передавая туда чистый Int-индекс activeIndex!
                  modifier = Modifier.tabIndicatorOffset(activeIndex),
                  width = 64.dp,
                  shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
              }
            ) {
              tabs.forEach { tab ->
                LeadingIconTab(
                  selected = tabNavigator.current == tab,
                  onClick = { tabNavigator.current = tab },
                  text = {
                    Text(
                      text = tab.options.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = if (tabNavigator.current == tab) FontWeight.Bold else FontWeight.Medium
                    )
                  },
                  icon = {
                    Icon(
                      painter = tab.options.icon ?: rememberVectorPainter(Icons.Default.Home),
                      contentDescription = null
                    )
                  }
                )
              }
            }


            // Отрисовываем холст текущей выбранной Voyager-вкладки на месте вызова
            Box(modifier = Modifier.fillMaxSize()) {
              CurrentTab()
            }
          }
        }
      }
    }
  }
}

/**
 * [InfoScreenDualPanelContent] — Верстка планшетного и десктопного двухпанельного режима ГИОЦ.
 * ИСПРАВЛЕНО: Аргумент строго типизирован как ApartmentScreenModel.
 */
@Composable
fun InfoScreenDualPanelContent(
  baseUIState: BaseUIState,
  apartmentScreenModel: ApartmentScreenModel // ИСПРАВЛЕНО НАМЕРТВО
) {
  Row(
    modifier = Modifier.fillMaxSize(),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      modifier = Modifier
        .weight(0.45f)
        .fillMaxHeight(),
      color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
      Column {
        DualPaneHeader(Icons.Default.Home, stringResource(Res.string.info))
        BtiPanelContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
      }
    }

    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

    Column(
      modifier = Modifier
        .weight(0.55f)
        .fillMaxHeight()
    ) {
      DualPaneHeader(Icons.Default.People, "Склад сім'ї")
      FamilyContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
    }
  }
}

@Composable
private fun DualPaneHeader(icon: ImageVector, title: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )
  }
  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp)
}
