package com.ykis.ykismobkmp.ui.screens.meter
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * [MeterListScreen] — Кроссплатформенный компонент списка счетчиков (Вода / Тепло) на базе Compose Multiplatform.
 * ИСПРАВЛЕНО: Избыточный класс Voyager Screen удален. Функция переведена в формат @Composable
 * со строгим соответствием сигнатуры вызова внутри MainMeterScreen.kt.
 */
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
  // Трассировка смены вкладок по нашему правилу [Класс.Метод] через КМР-команду println()
  LaunchedEffect(selectedTab) {
    println("[$className.Content]: Отрисовка списков приборов учета ГИОЦ. Активный таб: $selectedTab")
  }

  Row(modifier = modifier.fillMaxSize()) {
    Column(Modifier.weight(1f)) {

      // Мультиплатформенный DefaultAppBar ( subtitle принимает адрес квартиры из биллинга ЮКИС )
      DefaultAppBar(
        title = "Прилади обліку",
        subtitle = baseUIState.address,
        onBackClick = {},
        onDrawerClick = onDrawerClick,
        canNavigateBack = false,
        navigationType = navigationType
      )

      // Переключатель вкладок биллинга (Вода / Тепло) ЮКИС г. Южный
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
        divider = { HorizontalDivider(thickness = 0.5.dp) }
      ) {
        val tabs = listOf("Водопостачання", "Опалення")
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = {
              println("[$className.Tab]: Переключение таба ЖКХ на индекс: $index")
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

    // Вертикальный разделитель для DualPane/Развернутого режима (Mac Desktop / iPad)
    VerticalDivider(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      thickness = 0.5.dp
    )
  }
}

