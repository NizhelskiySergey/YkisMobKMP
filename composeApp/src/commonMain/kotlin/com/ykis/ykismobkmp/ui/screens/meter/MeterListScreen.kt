package com.ykis.ykismobkmp.ui.screens.meter.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.ui.NavigationType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.meter.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.MeterViewModel
import com.ykis.ykismobkmp.ui.screens.meter.WaterMeterState
import com.ykis.ykismobkmp.ui.screens.meter.utils.METER_TAB_ITEM
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.meters
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterList
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterList

private const val className = "MeterListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterListScreen(
  modifier: Modifier = Modifier,
  viewModel: MeterViewModel,
  baseUIState: BaseUIState,
  navigationType: NavigationType,
  onWaterMeterClick: (WaterMeterEntity) -> Unit,
  onHeatMeterClick: (HeatMeterEntity) -> Unit,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  selectedTab: Int,
  onTabClick: (Int) -> Unit,
  onDrawerClick: () -> Unit,
) {
  // Лог отрисовки согласно правилу [Класс.Метод]
  Log.d("YkisLog", "[$className.Content]: Rendering with tab $selectedTab")

  Row(modifier.fillMaxSize()) {
    Column(Modifier.weight(1f)) {
      // Используем наш мультиплатформенный DefaultAppBar
      DefaultAppBar(
        title = stringResource(Res.string.meters),
        subtitle = baseUIState.address,
        onBackClick = {},
        onDrawerClick = onDrawerClick,
        canNavigateBack = false,
        navigationType = navigationType
      )

      // Переключатель вкладок (Вода / Тепло)
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
        divider = { HorizontalDivider(thickness = 0.5.dp) }
      ) {
        METER_TAB_ITEM.forEachIndexed { index, tabItem ->
          LeadingIconTab(
            selected = selectedTab == index,
            onClick = {
              Log.d("YkisLog", "[$className.Tab]: Switched to $index")
              onTabClick(index)
            },
            text = {
              Text(text = stringResource(tabItem.titleRes)) // Используем Res
            },
            icon = {
              Icon(
                imageVector = if (index == selectedTab) tabItem.selectedIcon else tabItem.unselectedIcon,
                contentDescription = stringResource(tabItem.titleRes)
              )
            }
          )
        }
      }

      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
      ) {
        // Плавное переключение списков
        Crossfade(
          targetState = selectedTab,
          label = "MeterListFade"
        ) { targetState ->
          when (targetState) {
            0 -> WaterMeterList(
              viewModel = viewModel,
              baseUIState = baseUIState,
              onWaterMeterClick = onWaterMeterClick,
              waterMeterState = waterMeterState
            )
            else -> HeatMeterList(
              viewModel = viewModel,
              baseUIState = baseUIState,
              onHeatMeterClick = onHeatMeterClick,
              heatMeterState = heatMeterState
            )
          }
        }
      }
    }

    // Разделитель для DualPane режима (Mac/Планшет)
    VerticalDivider(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      thickness = 0.5.dp
    )
  }
}
