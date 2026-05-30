package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.delete_account_title
import ykismobkmp.composeapp.generated.resources.delete_my_account
import ykismobkmp.composeapp.generated.resources.info
private const val className = "InfoApartmentScreen"

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
    val baseUIState by apartmentScreenModel.apartmentUiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showWarningDialog by remember { mutableStateOf(false) }

    if (showWarningDialog) {
      AlertDialog(
        onDismissRequest = { showWarningDialog = false },
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(Res.string.delete_account_title), fontWeight = FontWeight.Bold) },
        text = { Text("Ви дійсно хочете видалити цю квартиру з облікового запису? Дані нарахувань на сервері неможливо буде відновити локально.") },
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

                // ИСПРАВЛЕНО НАМЕРТВО: Указываем полный Package Path синглтон-объекта из ScreensRegistry,
                // полностью исключая конфликт имен с @Composable-функцией в рантайме!
                navigator.replaceAll(com.ykis.ykismobkmp.ui.navigation.AddApartmentScreen)
              }
            )
            showWarningDialog = false
          }) {
            Text(text = stringResource(Res.string.delete_my_account), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
          }
        }

      )
    }

    LaunchedEffect(baseUIState.addressId, baseUIState.apartments.size, baseUIState.uid) {
      val methodName = "LaunchedEffect"
      val currentFirebaseUid = baseUIState.uid

      if (currentFirebaseUid.isNullOrBlank() || baseUIState.apartments.isEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: [WAIT] Дані профілю або список квартир ЮКІС ще не готові в СУБД.")
        return@LaunchedEffect
      }

      // Обчислюємо цільовий ID адреси строго на основі вибраного в стейті addressId
      val targetId = if (baseUIState.addressId != 0L) {
        baseUIState.addressId
      } else {
        baseUIState.apartments.firstOrNull()?.addressId ?: 0L
      }
      val isAnketaValid = baseUIState.apartment.addressId == targetId &&
        !baseUIState.apartment.address.isNullOrBlank()

      if (targetId != 0L && !isAnketaValid) {
        println("[YkisLogKMP.$className.$methodName]: [LOAD] Анкета пуста або ID змінено. Запит детальних даних ЮКІС з мережі Ktor для ID: ${targetId}L")
        apartmentScreenModel.getApartment(addressId = targetId)
      } else {
        println("[YkisLogKMP.$className.$methodName]: [SKIP] Об'єкт БТІ ${targetId}L вже повністю наповнений даними (Адрес: ${baseUIState.apartment.address}). Мережевий спам відсічено.")
      }
    }

    Scaffold(
      topBar = {
        DefaultAppBar(
          title = baseUIState.address,
          subtitle = " о/р ${baseUIState.addressId}",
          canNavigateBack = false,
          onDrawerClick = onDrawerClicked,
          navigationType = adaptiveNavigationType,
          actionButton = {
            if (baseUIState.userRole == UserRole.StandardUser && baseUIState.addressId != 0L) {
              IconButton(onClick = { showWarningDialog = true }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
          // Мобільний режим (Вкладки) — чистый Material 3 без багов сторонних библиотек!
          PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) },
            indicator = {
              TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTab),
                width = 64.dp,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
              )
            }
          ) {
            // ВКЛАДКА №1: ТЕХНИЧЕСКИЕ ХАРАКТЕРИСТИКИ
            LeadingIconTab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Text(
                  text = stringResource(Res.string.info),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                )
              },
              icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) }
            )

            // ВКЛАДКА №2: СКЛАД СІМ'Ї
            LeadingIconTab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Text(
                  text = "Склад сім'ї",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                )
              },
              icon = { Icon(imageVector = Icons.Default.People, contentDescription = null) }
            )
          }

          AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
              if (targetState > initialState) {
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn())
                  .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut())
              } else {
                (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn())
                  .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut())
              }.using(SizeTransform(clip = false))
            },
            label = "TabContentAnimation"
          ) { targetIndex ->
            Box(modifier = Modifier.fillMaxSize()) {
              // СТЫКОВКА МОДЕЛЕЙ: Передаем отлаженный кроссплатформенный apartmentScreenModel синглтон!
              when (targetIndex) {
                0 -> BtiPanelContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
                else -> FamilyContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun InfoScreenDualPanelContent(
  baseUIState: BaseUIState,
  apartmentScreenModel: ApartmentScreenModel
) {
  Row(
    modifier = Modifier.fillMaxSize(),
    verticalAlignment = Alignment.Top
  ) {
    // ЛЕВАЯ ПАНЕЛЬ: Характеристики БТИ квартиры
    Surface(
      modifier = Modifier
        .weight(0.45f)
        .fillMaxHeight(),
      color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
      Column {
        DualPaneHeader(Icons.Default.Home, stringResource(Res.string.info))
        BtiPanelContent(baseUIState = baseUIState, viewModel = apartmentScreenModel)
      }
    }

    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

    // ПРАВАЯ ПАНЕЛЬ: Состав семьи (Паспортный стол ГИОЦ)
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

/**
 * [DualPaneHeader] — Універсальний графічний заголовок для секцій двопанельного режиму.
 */
@Composable
private fun DualPaneHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
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




