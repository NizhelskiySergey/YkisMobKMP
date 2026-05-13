package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.ui.ContentType
import com.ykis.ykismobkmp.ui.NavigationType
import com.ykis.ykismobkmp.ui.YkisPamAppState
import com.ykis.ykismobkmp.ui.components.DialogCancelButton
import com.ykis.ykismobkmp.ui.components.DialogConfirmButton
import com.ykis.ykismobkmp.ui.components.DefaultAppBar // Предполагаем наличие общего AppBar
import com.ykis.ykismobkmp.ui.screens.apartment.components.BtiPanelContent
import com.ykis.ykismobkmp.ui.screens.apartment.components.FamilyContent
import com.ykis.ykismobkmp.ui.screens.apartment.utils.INFO_APARTMENT_TAB_ITEM
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.UserRole

private const val className = "InfoApartmentScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoApartmentScreen(
  modifier: Modifier = Modifier,
  contentType: ContentType,
  baseUIState: BaseUIState,
  apartmentViewModel: ApartmentViewModel,
  appState: YkisPamAppState,
  deleteApartment: () -> Unit,
  onDrawerClicked: () -> Unit,
  navigationType: NavigationType,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var showWarningDialog by remember { mutableStateOf(false) }

  // 1. Диалог подтверждения удаления
  if (showWarningDialog) {
    AlertDialog(
      onDismissRequest = { showWarningDialog = false },
      icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
      title = { Text(stringResource(Res.string.title_delete_appartment)) },
      text = { Text("Ви дійсно хочете видалити цю квартиру? Дані не можна буде відновити.") },
      dismissButton = {
        DialogCancelButton(Res.string.cancel) { showWarningDialog = false }
      },
      confirmButton = {
        DialogConfirmButton(Res.string.title_delete_appartment) {
          Log.d("YkisLog", "[$className.delete]: Confirmation received")
          deleteApartment()
          showWarningDialog = false
        }
      }
    )
  }

  // 2. Логика загрузки данных БТИ (Золотой фонд)
  LaunchedEffect(baseUIState.addressId, baseUIState.apartments.size, baseUIState.uid) {
    val currentFirebaseUid = baseUIState.uid

    if (currentFirebaseUid == null || baseUIState.apartments.isEmpty()) {
      Log.d("YkisLog", "[$className.LaunchedEffect]: [WAIT] Profile or list not ready")
      return@LaunchedEffect
    }

    val targetId = if (baseUIState.addressId != 0) {
      baseUIState.addressId
    } else {
      baseUIState.apartments.firstOrNull()?.addressId ?: 0
    }

    if (targetId != 0 && targetId != apartmentViewModel.lastLoadedAddressId) {
      Log.d("YkisLog", "[$className.LaunchedEffect]: [LOAD] Requesting ID: $targetId")
      apartmentViewModel.getApartment(targetId)
    }
  }

  Scaffold(
    topBar = {
      DefaultAppBar(
        title = baseUIState.address,
        subtitle = " о/р ${baseUIState.addressId}",
        canNavigateBack = false,
        onDrawerClick = onDrawerClicked,
        navigationType = navigationType,
        actionButton = {
          if (baseUIState.userRole == UserRole.StandardUser) {
            IconButton(onClick = { showWarningDialog = true }) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.delete_appartment),
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .padding(innerPadding)
        .fillMaxSize()
    ) {
      if (contentType == ContentType.DUAL_PANE) {
        InfoScreenDualPanelContent(
          baseUIState = baseUIState,
          apartmentViewModel = apartmentViewModel
        )
      } else {
        // Мобильный режим / Узкое окно Mac
        PrimaryTabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surface,
          divider = { HorizontalDivider(thickness = 0.5.dp) },
          indicator = {
            TabRowDefaults.PrimaryIndicator(
              modifier = Modifier.tabIndicatorOffset(selectedTab),
              width = 64.dp,
              shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
          }
        ) {
          INFO_APARTMENT_TAB_ITEM.forEachIndexed { index, tabItem ->
            LeadingIconTab(
              selected = selectedTab == index,
              onClick = {
                Log.d("YkisLog", "[$className.Tab]: Switch to $index")
                selectedTab = index
              },
              text = {
                Text(
                  text = stringResource(tabItem.titleRes), // Используем Res
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                )
              },
              icon = {
                Icon(
                  imageVector = if (index == selectedTab) tabItem.selectedIcon else tabItem.unselectedIcon,
                  contentDescription = null
                )
              }
            )
          }
        }

        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            if (targetState > initialState) {
              (slideInHorizontally { it } + fadeIn())
                .togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
              (slideInHorizontally { -it } + fadeIn())
                .togetherWith(slideOutHorizontally { it } + fadeOut())
            }.using(SizeTransform(clip = false))
          },
          label = "TabContentAnimation"
        ) { targetIndex ->
          Box(modifier = Modifier.fillMaxSize()) {
            when (targetIndex) {
              0 -> BtiPanelContent(baseUIState = baseUIState, viewModel = apartmentViewModel)
              else -> FamilyContent(baseUIState = baseUIState)
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
  apartmentViewModel: ApartmentViewModel
) {
  Row(modifier = Modifier.fillMaxSize()) {
    // Левая панель: БТИ (45% ширины)
    Surface(
      modifier = Modifier.weight(0.45f).fillMaxHeight(),
      color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
      Column {
        DualPaneHeader(Icons.Default.Home, stringResource(Res.string.bti))
        BtiPanelContent(baseUIState = baseUIState, viewModel = apartmentViewModel)
      }
    }

    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

    // Правая панель: Состав семьи (55% ширины)
    Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
      DualPaneHeader(Icons.Default.People, "Склад сім'ї")
      FamilyContent(baseUIState = baseUIState)
    }
  }
}

@Composable
private fun DualPaneHeader(icon: ImageVector, title: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )
  }
  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp)
}
