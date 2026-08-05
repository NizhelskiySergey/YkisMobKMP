package com.ykis.ykismobkmp.ui.screens.meter.water


import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.*

@Composable
private fun CenteredProgressIndicator(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(strokeWidth = 3.dp)
  }
}

@Composable
fun WaterReadings(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  meterUIState: BaseUIState,
  getWaterReadings: () -> Unit
) {
  LaunchedEffect(key1 = baseUIState.addressId, key2 = meterUIState.selectedWaterMeter) {
    if (baseUIState.addressId != 0L && meterUIState.selectedWaterMeter.vodomerId != 0L) {
      println("[WaterReadings.LaunchedEffect]: Запит історії водопостачання для о/р Long: ${baseUIState.addressId}")
      getWaterReadings()
    }
  }

  Crossfade(
    targetState = meterUIState.isReadingsLoading,
    label = "WaterReadingsLoadingFade",
    animationSpec = tween(durationMillis = 300, delayMillis = 100)
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else if (meterUIState.waterReadings.isEmpty()) {
      EmptyListState(
        title = stringResource(Res.string.history_empty_title),
        subtitle = stringResource(Res.string.history_empty_water_subtitle)
      )
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(
          items = meterUIState.waterReadings,
          key = { it.pokId }
        ) { waterReading ->
          WaterReadingItem(reading = waterReading)
        }
      }
    }
  }
}

@Composable
fun WaterReadingItem(
  modifier: Modifier = Modifier,
  reading: WaterReadingEntity
) {
  val cardLabel = if (reading.avg == 1L) stringResource(Res.string.calculation_by_average) else null
  BaseCard(
    modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
    label = cardLabel
  ) {
    WaterReadingItemContent(reading = reading)
  }
}

@Composable
fun WaterReadingItemContent(
  modifier: Modifier = Modifier,
  reading: WaterReadingEntity
) {
  Column(modifier = modifier.fillMaxWidth()) {
    if (reading.avg == 1L) {
      LabelTextWithText(
        labelText = stringResource(Res.string.initial_reading) + " ", 
        valueText = reading.pokOt.toString()
      )
      LabelTextWithText(
        labelText = stringResource(Res.string.final_reading) + " ",
        valueText = reading.pokDo.toString()
      )
      LabelTextWithText(
        labelText = stringResource(Res.string.cubic_meters_count) + " ",
        valueText = reading.qtyKub.toString()
      )
      LabelTextWithText(
        labelText = stringResource(Res.string.calculation_days) + " ",
        valueText = reading.rday.toString()
      )
      LabelTextWithText(
        labelText = stringResource(Res.string.daily_consumption) + " ",
        valueText = "${reading.kubDay} м³"
      )
    } else {
      LabelTextWithText(
        labelText = stringResource(Res.string.billing_period) + " ",
        valueText = "${reading.dateOt} — ${reading.dateDo}"
      )
      LabelTextWithText(
        labelText = stringResource(Res.string.days_count) + " ",
        valueText = reading.days.toString()
      )
      
      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = stringResource(Res.string.previous),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = reading.last.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = stringResource(Res.string.current),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = reading.current.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = stringResource(Res.string.cubs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = reading.kub.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    LabelTextWithText(
      modifier = Modifier.padding(vertical = 4.dp),
      labelText = stringResource(Res.string.date_entry) + " ",
      valueText = reading.dateIn
    )
  }
}
