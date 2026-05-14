package com.ykis.ykismobkmp.ui.screens.meter.components

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.apartment.BaseUIState
import com.ykis.ykismobkmp.ui.screens.meter.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.MeterScreenModel
import com.ykis.ykismobkmp.ui.screens.meter.WaterMeterState
import android.util.Log
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.reading.HeatReadings
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.water.reading.WaterReadings

private const val className = "MeterDetailContent"

@Composable
fun MeterDetailContent(
    baseUIState: BaseUIState,
    contentDetail: ContentDetail,
    waterMeterState: WaterMeterState,
    viewModel: MeterScreenModel,
    heatMeterState: HeatMeterState
) {
  // Логируем смену контента согласно правилу [Класс.Метод]
  Log.d("YkisLog", "[$className.Content]: Switching to $contentDetail")

  Crossfade(targetState = contentDetail, label = "MeterDetailFade") { targetState ->
    when (targetState) {
      ContentDetail.WATER_METER -> {
        WaterMeterDetail(
          waterMeterEntity = waterMeterState.selectedWaterMeter,
          baseUIState = baseUIState,
          getLastReading = {
            Log.d("YkisLog", "[$className.Water]: Request last reading")
            viewModel.getLastWaterReading(
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId,
              uid = baseUIState.uid ?: ""
            )
          },
          lastReading = waterMeterState.lastWaterReading,
          // Используем безопасную логику KMP для проверки состояния счетчика
          isWorking = waterMeterState.selectedWaterMeter.spisan != 1 &&
            waterMeterState.selectedWaterMeter.out_ != 1,
          isLastReadingLoading = waterMeterState.isLastReadingLoading,
          newWaterReading = waterMeterState.newWaterReading,
          onNewReadingChange = { newValue ->
            viewModel.onNewWaterReadingChange(newValue.filter { it.isDigit() })
          },
          addReading = {
            Log.i(
              "YkisLog",
              "[$className.Water]: Adding reading ${waterMeterState.newWaterReading}"
            )
            viewModel.addWaterReading(
              uid = baseUIState.uid.toString(),
              currentValue = waterMeterState.lastWaterReading.current,
              newValue = waterMeterState.newWaterReading.toIntOrNull() ?: 0,
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId
            )
          },
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.WATER_READINGS)
          },
          deleteReading = {
            Log.w("YkisLog", "[$className.Water]: Deleting last reading")
            viewModel.deleteLastWaterReading(
              uid = baseUIState.uid.toString(),
              vodomerId = waterMeterState.lastWaterReading.vodomerId,
              readingId = waterMeterState.lastWaterReading.pokId
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
          isWorking = heatMeterState.selectedHeatMeter.spisan != 1 &&
            heatMeterState.selectedHeatMeter.out_ != 1,
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.HEAT_READINGS)
          },
          newHeatReading = heatMeterState.newHeatReading,
          onNewReadingChange = { newValue ->
            // Для тепла разрешаем точку/запятую
            viewModel.onNewHeatReadingChange(newValue.filter { it.isDigit() || it == '.' || it == ',' })
          },
          addReading = {
            viewModel.addHeatReading(
              uid = baseUIState.uid.toString(),
              teplomerId = heatMeterState.selectedHeatMeter.teplomerId,
              currentValue = heatMeterState.lastHeatReading.current,
              newValue = heatMeterState.newHeatReading.toDoubleOrNull() ?: 0.0
            )
          },
          deleteReading = {
            viewModel.deleteLastHeatReading(
              readingId = heatMeterState.lastHeatReading.pokId,
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

      else -> EmptyDetailPlaceholder("Оберіть розділ")
    }
  }
}
