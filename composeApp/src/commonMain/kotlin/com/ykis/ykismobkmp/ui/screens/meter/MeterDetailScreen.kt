package com.ykis.ykismobkmp.ui.screens.meter


import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatReadings
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterReadings
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_history

private const val className = "MeterDetailScreen"

@Composable
fun MeterDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  viewModel: MeterScreenModel,
  baseUIState: BaseUIState
) {
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
        when (contentDetail) {
          ContentDetail.WATER_READINGS -> viewModel.setContentDetail(ContentDetail.WATER_METER)
          ContentDetail.HEAT_READINGS -> viewModel.setContentDetail(ContentDetail.HEAT_METER)
          else -> viewModel.closeContentDetail()
        }
      },
      actionButton = {
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

              imageVector = Icons.Default.History,
              contentDescription = "Історія показань",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

        }
      }
    )

    // Передаем viewModel вниз, избавляясь от скрытых koinInject
    MeterDetailContent(
      baseUIState = baseUIState,
      contentDetail = contentDetail,
      waterMeterState = waterMeterState,
      heatMeterState = heatMeterState,
      viewModel = viewModel
    )
  }
}

@Composable
fun MeterDetailContent(
  baseUIState: BaseUIState,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  viewModel: MeterScreenModel
) {
  val currentClassName = "MeterDetailContent"

  LaunchedEffect(contentDetail) {
    println("[$currentClassName.Content]: Switching active sub-module view to $contentDetail")
  }

  Crossfade(targetState = contentDetail, label = "MeterDetailFade") { targetState ->
    when (targetState) {
      ContentDetail.WATER_METER -> {
        WaterMeterDetail(
          waterMeterEntity = waterMeterState.selectedWaterMeter,
          baseUIState = baseUIState,
          getLastReading = {
            println("[$currentClassName.Water]: Request last reading from Ktor API")
            viewModel.getLastWaterReading(
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId,
              uid = baseUIState.uid ?: ""
            )
          },
          lastReading = waterMeterState.lastWaterReading,
          isWorking = waterMeterState.selectedWaterMeter.spisan != 1L &&
            waterMeterState.selectedWaterMeter.isOut != 1L,
          isLastReadingLoading = waterMeterState.isLastReadingLoading,
          newWaterReading = waterMeterState.newWaterReading,
          onNewReadingChange = { newValue ->
            viewModel.onNewWaterReadingChange(newValue.filter { it.isDigit() })
          },
          addReading = {
            println("[$currentClassName.Water]: Adding new digital reading to СУБД: ${waterMeterState.newWaterReading}")
            viewModel.addWaterReading(
              uid = baseUIState.uid.toString(),
              currentValue = waterMeterState.lastWaterReading?.current ?: 0L,
              newValue = waterMeterState.newWaterReading.toLongOrNull() ?: 0L,
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId
            )
          },
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.WATER_READINGS)
          },
          deleteReading = {
            println("[$currentClassName.Water]: Request atomical deletion of last water reading")
            viewModel.deleteLastWaterReading(
              uid = baseUIState.uid.toString(),
              vodomerId = waterMeterState.lastWaterReading?.vodomerId ?: 0L,
              readingId = waterMeterState.lastWaterReading?.pokId ?: 0L
            )
          }
        )
      }
      ContentDetail.HEAT_METER -> {
        HeatMeterDetail(
          heatMeterEntity = heatMeterState.selectedHeatMeter,
          baseUIState = baseUIState,
          getLastHeatReading = {
            viewModel.getLastHeatReading(
              uid = baseUIState.uid ?: "",
              teplomerId = heatMeterState.selectedHeatMeter.teplomerId
            )
          },
          lastHeatReading = heatMeterState.lastHeatReading,
          isWorking = heatMeterState.selectedHeatMeter.spisan != 1L &&
            heatMeterState.selectedHeatMeter.isOut != 1L,
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.HEAT_READINGS)
          },
          newHeatReading = heatMeterState.newHeatReading,
          onNewReadingChange = { newValue ->
            viewModel.onNewHeatReadingChange(newValue.filter { it.isDigit() || it == '.' || it == ',' })
          },
          addReading = {
            viewModel.addHeatReading(
              uid = baseUIState.uid.toString(),
              teplomerId = heatMeterState.selectedHeatMeter.teplomerId,
              currentValue = heatMeterState.lastHeatReading?.current ?: 0.0,
              newValue = heatMeterState.newHeatReading.toDoubleOrNull() ?: 0.0
            )
          },
          deleteReading = {
            viewModel.deleteLastHeatReading(
              readingId = heatMeterState.lastHeatReading?.pokId ?: 0L,
              teplomerId = heatMeterState.selectedHeatMeter.teplomerId,
              uid = baseUIState.uid.toString()
            )
          }
        )
      }
      ContentDetail.WATER_READINGS -> {
        WaterReadings(
          baseUIState = baseUIState,
          waterMeterState = waterMeterState,
          getWaterReadings = {
            viewModel.getWaterReadings(
              uid = baseUIState.uid ?: "",
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId
            )
          }
        )
      }
      ContentDetail.HEAT_READINGS -> {
        HeatReadings(
          baseUIState = baseUIState,
          heatMeterState = heatMeterState,
          getHeatReadings = {
            viewModel.getHeatReadings(
              uid = baseUIState.uid.toString(),
              teplomerId = heatMeterState.selectedHeatMeter.teplomerId
            )
          }
        )
      }
      else -> EmptyDetailPlaceholder("Оберіть розділ для зняття показань")
    }
  }
}


