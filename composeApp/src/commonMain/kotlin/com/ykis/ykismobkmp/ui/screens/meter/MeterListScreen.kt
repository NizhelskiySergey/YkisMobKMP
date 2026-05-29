package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterList
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterList
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState

private const val className = "MeterListScreen"

/**
 * [MeterListScreen] — Екран списків та перемикання приладів обліку Водоканалу та Тепломережі м. Южне.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterListScreen(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  navigationType: NavigationType,
  onDrawerClick: () -> Unit,
  viewModel: MeterScreenModel,
  onWaterMeterClick: (WaterMeterEntity) -> Unit,
  onHeatMeterClick: (HeatMeterEntity) -> Unit,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  selectedTab: Int,
  onTabClick: (Int) -> Unit
) {
  // Трасування зміни вкладок по нашому правилу [Клас.Метод] через КМР-команду println()
  LaunchedEffect(selectedTab) {
    println("[YkisLogKMP.$className.Content]: Отрисовка списков приборов учета ЮКІС. Активный таб: $selectedTab")
  }

  Row(modifier = modifier.fillMaxSize()) {
    Column(Modifier.weight(1f)) {

      // Мультиплатформенный DefaultAppBar ( subtitle принимает адрес квартиры из биллинга ЮКІС )
      DefaultAppBar(
        title = "Прилади обліку",
        subtitle = baseUIState.address,
        onBackClick = {},
        onDrawerClick = onDrawerClick,
        canNavigateBack = false,
        navigationType = navigationType
      )

      // Переключатель вкладок биллинга (Вода / Тепло) ЮКІС г. Южный
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
        divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) }
      ) {
        val tabs = listOf("Водопостачання", "Опалення")
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = {
              println("[YkisLogKMP.$className.Tab]: Переключение таба ЖКХ на индекс: $index")
              onTabClick(index)
            },
            text = {
              Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
          )
        }
      }

      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
      ) {
        // Плавное кроссплатформенное переключение списков без лишней нагрузки на Skiko-конвейер
        Crossfade(
          targetState = selectedTab,
          label = "MeterListFade"
        ) { targetState ->
          when (targetState) {
            0 -> WaterMeterList(
              onWaterMeterClick = onWaterMeterClick,
              waterMeterState = waterMeterState
            )
            else -> HeatMeterList(
              onHeatMeterClick = onHeatMeterClick,
              heatMeterState = heatMeterState
            )
          }
        }
      }
    }
    if (navigationType == NavigationType.BOTTOM_NAVIGATION || navigationType == NavigationType.NAVIGATION_RAIL_EXPANDED) {
      VerticalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
      )
    }
  }
}


