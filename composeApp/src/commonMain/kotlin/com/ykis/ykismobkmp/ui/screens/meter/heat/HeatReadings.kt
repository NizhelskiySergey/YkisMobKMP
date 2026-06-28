package com.ykis.ykismobkmp.ui.screens.meter.heat

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
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "HeatReadings"

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
fun HeatReadings(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,       // Обязательный сквозной КМР-параметр
  meterUIState: BaseUIState,
  getHeatReadings: () -> Unit     // Обязательный сквозной КМР-параметр
) {
  LaunchedEffect(baseUIState.addressId, meterUIState.selectedHeatMeter.teplomerId) {
    if (baseUIState.addressId != 0L && meterUIState.selectedHeatMeter.teplomerId != 0L) {
      println("[$className.LaunchedEffect]: Оновлення історії опалення для рахунку Long: ${baseUIState.addressId}")
      getHeatReadings()
    }
  }

  Crossfade(
    targetState = meterUIState.isReadingsLoading,
    label = "HeatReadingsLoadingFade",
    animationSpec = tween(durationMillis = 300, delayMillis = 100)
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else if (meterUIState.heatReadings.isEmpty()) {
      EmptyListState(
        title = stringResource(Res.string.history_empty_title),
        subtitle = stringResource(Res.string.history_empty_heat_subtitle)
      )
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(
          items = meterUIState.heatReadings,
          key = { it.pokId }
        ) { heatReading ->
          HeatReadingItem(reading = heatReading)
        }
      }
    }
  }
}

@Composable
fun HeatReadingItem(
  modifier: Modifier = Modifier,
  reading: HeatReadingEntity
) {
  BaseCard(
    modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
    label = if (reading.avg == 1L) stringResource(Res.string.calculation_by_average) else null
  ) {
    HeatReadingItemContent(
      reading = reading,
      isAverage = reading.avg == 1L
    )
  }
}

@Composable
fun HeatReadingItemContent(
  modifier: Modifier = Modifier,
  reading: HeatReadingEntity,
  isAverage: Boolean
) {
  Column(modifier = modifier.fillMaxWidth()) {
    LabelTextWithText(
      modifier = Modifier.padding(vertical = 2.dp),
      labelText = stringResource(Res.string.billing_period) + " ",
      valueText = "${reading.dateOt} — ${reading.dateDo}"
    )
    if (isAverage) {
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.calculation_days_average) + " ",
        valueText = reading.dayAvg
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.calculated_gkal) + " ",
        valueText = reading.gkalRasch
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.daily_consumption_gkal) + " ",
        valueText = reading.gkalDay
      )
    } else {
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
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
            text = stringResource(Res.string.gkal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = reading.gkal.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.volume_qty) + " ",
        valueText = reading.qty.toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.active_tariff) + " ",
        valueText = "${reading.tarif} грн/Гкал"
      )
    }

    LabelTextWithText(
      modifier = Modifier.padding(vertical = 4.dp),
      labelText = stringResource(Res.string.date_entry) + " ",
      valueText = reading.dateIn
    )
  }
}


