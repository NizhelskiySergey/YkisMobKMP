package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterList
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterList
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState
import org.koin.compose.koinInject

private const val className = "MeterListScreen"

/**
 * [MeterListScreen] — Кроссплатформенный экран счетчиков (Вода / Тепло) на базе Voyager.
 * Одинаково стабильно работает на смартфонах жителей и в админке на Mac Desktop (JVM).
 */
class MeterListScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Внедряем единый кроссплатформенный ScreenModel через Koin
    val screenModel = koinInject<MeterScreenModel>()

    // Подписываемся на реактивные потоки состояний (КМР-стандарт collectAsState)
    val baseUIState by screenModel.baseUIState.collectAsState()
    val waterMeterState by screenModel.waterMeterState.collectAsState()
    val heatMeterState by screenModel.heatMeterState.collectAsState()
    val selectedTab by screenModel.selectedTab.collectAsState()

    MeterListScreenStateless(
      baseUIState = baseUIState,
      waterMeterState = waterMeterState,
      heatMeterState = heatMeterState,
      selectedTab = selectedTab,
      onTabClick = screenModel::onTabSelect,
      onWaterMeterClick = { waterMeter ->
        println("[$className]: Клик по водомеру ID=${waterMeter.vodomerId}, переход в историю")
      },
      onHeatMeterClick = { heatMeter ->
        println("[$className]: Клик по тепломеру ID=${heatMeter.teplomerId}, переход в историю")
        // navigator.push(HeatReadingsScreen(heatMeter.id))
      },
      onDrawerClick = {
        println("[$className]: Открытие бокового меню")
      }
    )
  }
}

/**
 * [MeterListScreenStateless] — Чистая верстка экрана, изолированная от DI и навигации.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterListScreenStateless(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  selectedTab: Int,
  onTabClick: (Int) -> Unit,
  onWaterMeterClick: (WaterMeterEntity) -> Unit,
  onHeatMeterClick: (HeatMeterEntity) -> Unit,
  onDrawerClick: () -> Unit,
) {
  // ИСПРАВЛЕНО: Заменен платформозависимый Log.d на универсальный println()
  LaunchedEffect(selectedTab) {
    println("[$className.Content]: Rendering with tab $selectedTab")
  }

  Row(modifier = modifier.fillMaxSize()) {
    Column(Modifier.weight(1f)) {

      // Мультиплатформенный DefaultAppBar ( subtitle принимает адрес квартиры из биллинга ЮКИС )
      DefaultAppBar(
        title = "Прилади обліку", // Заменено на чистую строку для Mac JVM совместимости
        subtitle = baseUIState.address,
        onBackClick = {},
        onDrawerClick = onDrawerClick,
        canNavigateBack = false
      )

      // Переключатель вкладок (Вода / Тепло)
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
        divider = { HorizontalDivider(thickness = 0.5.dp) }
      ) {
        // Декларативный проход по двум вкладкам
        val tabs = listOf("Водопостачання", "Опалення")
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = {
              println("[$className.Tab]: Switched to $index")
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
        // Плавное кроссплатформенное переключение списков
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
