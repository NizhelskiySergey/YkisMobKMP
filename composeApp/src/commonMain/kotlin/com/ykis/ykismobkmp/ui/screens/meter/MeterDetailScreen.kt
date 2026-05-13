package com.ykis.ykismobkmp.ui.screens.meter.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.apartment.BaseUIState
import com.ykis.ykismobkmp.ui.screens.meter.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.MeterViewModel
import com.ykis.ykismobkmp.ui.screens.meter.WaterMeterState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res
import android.util.Log
import com.ykis.ykismobkmp.ui.screens.meter.MeterDetailContent

private const val className = "MeterDetailScreen"

@Composable
fun MeterDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  viewModel: MeterViewModel,
  baseUIState: BaseUIState
) {
  Log.d("YkisLog", "[$className.Content]: Rendering detail for $contentDetail")

  Column(modifier = modifier) {
    // 1. НАСТРОЙКА ТУЛБАРА
    DefaultAppBar(
      title = when (contentDetail) {
        ContentDetail.WATER_METER -> waterMeterState.selectedWaterMeter.model
        ContentDetail.HEAT_METER -> heatMeterState.selectedHeatMeter.model
        else -> stringResource(Res.string.reading_history)
      },
      onBackClick = {
        Log.d("YkisLog", "[$className.onBackClick]: Navigating back from $contentDetail")
        when (contentDetail) {
          ContentDetail.WATER_READINGS -> viewModel.setContentDetail(ContentDetail.WATER_METER)
          ContentDetail.HEAT_READINGS -> viewModel.setContentDetail(ContentDetail.HEAT_METER)
          else -> viewModel.closeContentDetail()
        }
      },
      actionButton = {
        // Иконка истории (только если мы на экране информации о счетчике)
        if (contentDetail == ContentDetail.HEAT_METER || contentDetail == ContentDetail.WATER_METER) {
          IconButton(
            onClick = {
              val nextDetail = if (contentDetail == ContentDetail.WATER_METER)
                ContentDetail.WATER_READINGS else ContentDetail.HEAT_READINGS

              Log.d("YkisLog", "[$className.Action]: Open history -> $nextDetail")
              viewModel.setContentDetail(nextDetail)
            },
          ) {
            Icon(
              painter = painterResource(Res.drawable.ic_history),
              contentDescription = stringResource(Res.string.reading_history),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    )

    // 2. ОСНОВНОЙ КОНТЕНТ (Форма ввода или История)
    MeterDetailContent(
      baseUIState = baseUIState,
      contentDetail = contentDetail,
      waterMeterState = waterMeterState,
      viewModel = viewModel,
      heatMeterState = heatMeterState
    )
  }
}
