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
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatReadings
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterReadings
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res

private const val className = "MeterDetailScreen"

@Composable
fun MeterDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  meterUIState: BaseUIState,
  viewModel: MeterScreenModel,
  baseUIState: BaseUIState // Возвращаем базовый стейт (с UID и адресом)
) {
  LaunchedEffect(contentDetail) {
    println("[$className.Content]: Отрисовка формы ввода или истории ГИОЦ. Активный контент: $contentDetail")
  }

  Column(modifier = modifier.fillMaxSize()) {
    // 1. НАСТРОЙКА КРОСС ПЛАТФОРМЕННОГО ТУЛБАРА DefaultAppBar
    DefaultAppBar(
      title = when (contentDetail) {
        ContentDetail.WATER_METER -> "Водомір: ${meterUIState.selectedWaterMeter.model}"
        ContentDetail.HEAT_METER -> "Лічильник тепла: ${meterUIState.selectedHeatMeter.model}"
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
        val isMeterDetail = contentDetail == ContentDetail.HEAT_METER || contentDetail == ContentDetail.WATER_METER
        if (isMeterDetail) {
          IconButton(
            onClick = {
              val nextDetail = if (contentDetail == ContentDetail.WATER_METER)
                ContentDetail.WATER_READINGS else ContentDetail.HEAT_READINGS
              println("[YkisLogKMP.$className.Action]: Переход в историю -> $nextDetail")
              viewModel.setContentDetail(nextDetail)
            },
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = "История",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    )

    // Передаем оба стейта: базовый (для UID) и модульный (для данных счетчиков)
    MeterDetailContent(
      baseUIState = baseUIState,
      contentDetail = contentDetail,
      meterUIState = meterUIState,
      viewModel = viewModel
    )
  }
}

@Composable
fun MeterDetailContent(
  baseUIState: BaseUIState,
  contentDetail: ContentDetail,
  meterUIState: BaseUIState,
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
          waterMeterEntity = meterUIState.selectedWaterMeter,
          baseUIState = baseUIState,
          getLastReading = {
            println("[$currentClassName.Water]: Request last reading from Ktor API")
            viewModel.getLastWaterReading(
              vodomerId = meterUIState.selectedWaterMeter.vodomerId,
              uid = baseUIState.uid ?: ""
            )
          },
          lastReading = meterUIState.lastWaterReading,
          isWorking = meterUIState.selectedWaterMeter.spisan != 1L &&
            meterUIState.selectedWaterMeter.isOut != 1L,
          isLastReadingLoading = meterUIState.isLastReadingLoading,
          newWaterReading = meterUIState.newWaterReading,
          onNewReadingChange = { newValue ->
            viewModel.onNewWaterReadingChange(newValue.filter { it.isDigit() })
          },
          addReading = {
            println("[$currentClassName.Water]: Adding new digital reading to СУБД: ${meterUIState.newWaterReading}")
            viewModel.addWaterReading(
              uid = baseUIState.uid.toString(),
              currentValue = meterUIState.lastWaterReading?.current ?: 0L,
              newValue = meterUIState.newWaterReading.toLongOrNull() ?: 0L,
              vodomerId = meterUIState.selectedWaterMeter.vodomerId
            )
          },
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.WATER_READINGS)
          },
          deleteReading = {
            println("[$currentClassName.Water]: Request atomical deletion of last water reading")
            viewModel.deleteLastWaterReading(
              uid = baseUIState.uid.toString(),
              vodomerId = meterUIState.lastWaterReading?.vodomerId ?: 0L,
              readingId = meterUIState.lastWaterReading?.pokId ?: 0L
            )
          }
        )
      }
      ContentDetail.HEAT_METER -> {
        HeatMeterDetail(
          heatMeterEntity = meterUIState.selectedHeatMeter,
          baseUIState = baseUIState,
          getLastHeatReading = {
            viewModel.getLastHeatReading(
              uid = baseUIState.uid ?: "",
              teplomerId = meterUIState.selectedHeatMeter.teplomerId
            )
          },
          lastHeatReading = meterUIState.lastHeatReading,
          isWorking = meterUIState.selectedHeatMeter.spisan != 1L &&
            meterUIState.selectedHeatMeter.isOut != 1L,
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.HEAT_READINGS)
          },
          newHeatReading = meterUIState.newHeatReading,
          onNewReadingChange = { newValue ->
            // ИСПРАВЛЕНО: Убрана лишняя фильтрация, так как NumberField уже нормализует ввод
            viewModel.onNewHeatReadingChange(newValue)
          },
          addReading = {
            // ИСПРАВЛЕНО: Гарантированная нормализация разделителя перед парсингом в Double
            val normalizedValue = meterUIState.newHeatReading.replace(',', '.').toDoubleOrNull() ?: 0.0
            viewModel.addHeatReading(
              uid = baseUIState.uid.toString(),
              teplomerId = meterUIState.selectedHeatMeter.teplomerId,
              currentValue = meterUIState.lastHeatReading?.current ?: 0.0,
              newValue = normalizedValue
            )
          },
          deleteReading = {
            viewModel.deleteLastHeatReading(
              readingId = meterUIState.lastHeatReading?.pokId ?: 0L,
              teplomerId = meterUIState.selectedHeatMeter.teplomerId,
              uid = baseUIState.uid.toString()
            )
          }
        )
      }
      ContentDetail.WATER_READINGS -> {
        WaterReadings(
          baseUIState = baseUIState,
          meterUIState = meterUIState,
          getWaterReadings = {
            viewModel.getWaterReadings(
              uid = baseUIState.uid ?: "",
              vodomerId = meterUIState.selectedWaterMeter.vodomerId
            )
          }
        )
      }
      ContentDetail.HEAT_READINGS -> {
        HeatReadings(
          baseUIState = baseUIState,
          meterUIState = meterUIState,
          getHeatReadings = {
            viewModel.getHeatReadings(
              uid = baseUIState.uid.toString(),
              teplomerId = meterUIState.selectedHeatMeter.teplomerId
            )
          }
        )
      }
      else -> EmptyDetailPlaceholder("Оберіть розділ для зняття показань")
    }
  }
}


