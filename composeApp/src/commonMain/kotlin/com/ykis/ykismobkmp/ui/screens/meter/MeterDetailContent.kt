package com.ykis.ykismobkmp.ui.screens.meter
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.heat.reading.HeatReadings
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterDetail
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.reading.WaterReadings
import org.koin.compose.koinInject

private const val className = "MeterDetailContent"

/**
 * [MeterDetailContent] — Кроссплатформенный Stateful-контейнер переключения форм ввода показаний ЖКХ.
 * ИСПРАВЛЕНО: Сигнатура приведена в стопроцентное соответствие с MeterDetailScreen.kt.
 * Все ID переведены на сквозной Long-стандарт, Log заменен на println().
 */
@Composable
fun MeterDetailContent(
  baseUIState: BaseUIState,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState
) {
  // Нативная КМР инжекция ScreenModel для вызова доменных Use Cases
  val viewModel = koinInject<MeterScreenModel>()

  // Логируем смену контента согласно правилу [Класс.Метод] через КМР-команду println()
  LaunchedEffect(contentDetail) {
    println("[$className.Content]: Switching active sub-module view to $contentDetail")
  }

  Crossfade(targetState = contentDetail, label = "MeterDetailFade") { targetState ->
    when (targetState) {
      ContentDetail.WATER_METER -> {
        WaterMeterDetail(
          waterMeterEntity = waterMeterState.selectedWaterMeter,
          baseUIState = baseUIState,
          getLastReading = {
            println("[$className.Water]: Request last reading from Ktor API")
            viewModel.getLastWaterReading(
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId,
              uid = baseUIState.uid ?: ""
            )
          },
          lastReading = waterMeterState.lastWaterReading,
          // Используем безопасную логику KMP для проверки состояния списания прибора
          isWorking = waterMeterState.selectedWaterMeter.spisan != 1 &&
            waterMeterState.selectedWaterMeter.out_ != 1,
          isLastReadingLoading = waterMeterState.isLastReadingLoading,
          newWaterReading = waterMeterState.newWaterReading,
          onNewReadingChange = { newValue ->
            viewModel.onNewWaterReadingChange(newValue.filter { it.isDigit() })
          },
          addReading = {
            println("[$className.Water]: Adding new digital reading to СУБД: ${waterMeterState.newWaterReading}")
            viewModel.addWaterReading(
              uid = baseUIState.uid.toString(),
              currentValue = waterMeterState.lastWaterReading?.current ?: 0.0,
              // ИСПРАВЛЕНО: Приведение типов к сквозному КМР Long-стандарту взамен Int
              newValue = waterMeterState.newWaterReading.toLongOrNull() ?: 0L,
              vodomerId = waterMeterState.selectedWaterMeter.vodomerId
            )
          },
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.WATER_READINGS)
          },
          deleteReading = {
            println("[$className.Water]: Request atomical deletion of last water reading")
            viewModel.deleteLastWaterReading(
              uid = baseUIState.uid.toString(),
              vodomerId = waterMeterState.lastWaterReading?.vodomerId ?: 0L,
              // ИСПРАВЛЕНО: Идентификатор записи pokId извлечен в формате Long под SQLDelight
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
          isWorking = heatMeterState.selectedHeatMeter.spisan != 1 &&
            heatMeterState.selectedHeatMeter.out_ != 1,
          navigateToReadings = {
            viewModel.setContentDetail(ContentDetail.HEAT_READINGS)
          },
          newHeatReading = heatMeterState.newHeatReading,
          onNewReadingChange = { newValue ->
            // Для тепла Одесской обл. разрешаем дробный разделитель точка/запятая
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
              // ИСПРАВЛЕНО: Ключ записи pokId переведен на сквозной Long стандарт
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
