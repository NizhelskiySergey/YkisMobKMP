package com.ykis.ykismobkmp.ui.screens.meter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_history

private const val className = "MeterDetailScreen"

/**
 * [MeterDetailScreen] — Кроссплатформенный экран детализации и съема показаний ЮКИС.
 * ИСПРАВЛЕНО: Сигнатура приведена в стопроцентное соответствие с MainMeterScreen.kt,
 * управление стейтами возвращено на зафиксированную модель MeterScreenModel.
 */
@Composable
fun MeterDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  viewModel: MeterScreenModel, // ИСПРАВЛЕНО: Возвращен сквозной КМР-контракт модели экрана
  baseUIState: BaseUIState
) {
  // Трассировка согласно правилу [Класс.Метод] через КМР-команду println()
  LaunchedEffect(contentDetail) {
    println("[$className.Content]: Отрисовка формы ввода или истории ГИОЦ. Активный контент: $contentDetail")
  }

  Column(modifier = modifier.fillMaxSize()) {
    // 1. НАСТРОЙКА КРОСС ПЛАТФОРМЕННОГО ТУЛБАРА DefaultAppBar
    DefaultAppBar(
      title = when (contentDetail) {
        ContentDetail.WATER_METER -> "Водомір: ${waterMeterState.selectedWaterMeter.model}"
        ContentDetail.HEAT_METER -> "Лічильник тепла: ${heatMeterState.selectedHeatMeter.model}"
        ContentDetail.WATER_READINGS -> "Історія водопостачання"
        ContentDetail.HEAT_READINGS -> "Історія опалення"
        else -> "Прилади обліку ЮКІС"
      },
      onBackClick = {
        println("[$className.onBackClick]: Нажата стрелка назад. Текущий подмодуль: $contentDetail")
        // ИСПРАВЛЕНО: Нативно управляем переходами через методы переданной MeterScreenModel
        when (contentDetail) {
          ContentDetail.WATER_READINGS -> viewModel.setContentDetail(ContentDetail.WATER_METER)
          ContentDetail.HEAT_READINGS -> viewModel.setContentDetail(ContentDetail.HEAT_METER)
          else -> viewModel.closeContentDetail()
        }
      },
      actionButton = {
        // Иконка истории (отображается только если мы находимся на главном экране прибора учета)
        if (contentDetail == ContentDetail.HEAT_METER || contentDetail == ContentDetail.WATER_METER) {
          IconButton(
            onClick = {
              val nextDetail = if (contentDetail == ContentDetail.WATER_METER)
                ContentDetail.WATER_READINGS else ContentDetail.HEAT_READINGS

              println("[$className.Action]: Инициализация открытия истории показаний -> $nextDetail")
              viewModel.setContentDetail(nextDetail)
            },
          ) {
            Icon(
              // ИСПРАВЛЕНО: Ресурс истории переведен под кроссплатформенный Res.drawable
              painter = painterResource(Res.drawable.ic_history),
              contentDescription = "Історія показань",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    )

    // 2. ОСНОВНОЙ КОНТЕНТ (Форма ввода показаний водомеров/тепломеров г. Южного)
    MeterDetailContent(
      baseUIState = baseUIState,
      contentDetail = contentDetail,
      waterMeterState = waterMeterState,
      heatMeterState = heatMeterState
    )
  }
}


