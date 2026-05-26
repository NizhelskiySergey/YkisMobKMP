package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.screens.bti.BtiPanelContent
import com.ykis.ykismobkmp.ui.screens.family.FamilyContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.delete_account_title
import ykismobkmp.composeapp.generated.resources.delete_my_account
import ykismobkmp.composeapp.generated.resources.info

private const val className = "InfoApartmentScreen"

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
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    // ИСПРАВЛЕНО: Чтение переведено на легитимный baseUIState во избежание конфликтов final-супертипа
    val baseUIState by apartmentScreenModel.baseUIState.collectAsState()

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
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    // ИСПРАВЛЕНО: Считываем реактивный baseUIState
    val baseUIState by apartmentScreenModel.baseUIState.collectAsState()

    FamilyContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
  }
}

/**
 * [InfoApartmentScreen] — Кроссплатформенный экран характеристик жилья БТИ ЮКИС.
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

    val apartmentScreenModel = koinInject<ApartmentScreenModel>()
    // ИСПРАВЛЕНО: Поток направлен на baseUIState
    val baseUIState by apartmentScreenModel.baseUIState.collectAsState()

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
        confirmButton = {
          TextButton(onClick = {
            println("[YkisLogKMP.$className.Content]: [ACTION] Підтверджено видалення особового рахунку для addressId: ${baseUIState.addressId}")

            apartmentScreenModel.deleteApartmentFromProfile(
              addressId = baseUIState.addressId,
              onNavigateToAddScreen = {
                println("[YkisLogKMP.$className.Content]: Заміна кореня стеку Voyager на екран прив'язки квартири БТІ")

                // ИСПРАВЛЕНО: Имена аргументов приведены в точное соответствие с конструктором AddApartmentScreen
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
        println("[YkisLogKMP.$className.$methodName]: [WAIT] Дані профілю або список квартир БТИ ще не готові")
        return@LaunchedEffect
      }

      val targetId = if (baseUIState.addressId != 0L) baseUIState.addressId else baseUIState.apartments.firstOrNull()?.addressId ?: 0L

      if (targetId != 0L && targetId != apartmentScreenModel.lastLoadedAddressId) {
        println("[YkisLogKMP.$className.$methodName]: [LOAD] Запит ГІОЦ даних для ID: $targetId | UID: $currentFirebaseUid")
        apartmentScreenModel.getApartment(addressId = targetId)
      } else {
        println("[YkisLogKMP.$className.$methodName]: [SKIP] ID: $targetId вже є поточним. Циклічне завантаження запобіжено.")
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
            // Широкоформатний режим (Mac Desktop / Планшети) — виводимо дві панелі паралельно
            InfoScreenDualPanelContent(
              baseUIState = baseUIState,
              apartmentScreenModel = apartmentScreenModel
            )
          } else {
            // Мобільний режим (Вкладки) зі стабільними Skiko-індикаторами зсуву
            val tabs = remember { listOf(BtiTab, FamilyTab) }
            val activeIndex = tabNavigator.current.options.index.toInt()

            // ИСПРАВЛЕНО: Синхронизировано с актуальным TabIndicatorScope из Jetpack Compose Material 3 1.2+
            PrimaryTabRow(
              selectedTabIndex = activeIndex,
              containerColor = MaterialTheme.colorScheme.surface,
              divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) },
              indicator = {
                // Внутри TabIndicatorScope модификатор tabIndicatorOffset вызывается как расширение
                // и принимает строго выбранный Int-индекс activeIndex
                TabRowDefaults.PrimaryIndicator(
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


            // Відмальовуємо полотно поточної обраної Voyager-вкладки на місці виклику
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
 * [InfoScreenDualPanelContent] — Верстка планшетного і десктопного двопанельного режиму ГІОЦ.
 */
@Composable
fun InfoScreenDualPanelContent(
  baseUIState: BaseUIState,
  apartmentScreenModel: ApartmentScreenModel
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

