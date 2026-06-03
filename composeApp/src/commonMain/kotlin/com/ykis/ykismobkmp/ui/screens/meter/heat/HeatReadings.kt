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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.components.LabelTextWithText

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
        title = "Історія тепла порожня",
        subtitle = "Дані про споживання Гкал за даним приладом відсутні в біллінгу ЮТКЕ"
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
  // ИСПРАВЛЕНО: Заменено на каноничную BaseCard для соблюдения дизайн-системы ЮКІС
  BaseCard(
    modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
    label = if (reading.avg == 1L) "Розрахунок за середнім нормативом" else null
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
      labelText = "Період нарахування: ",
      valueText = "${reading.dateOt} — ${reading.dateDo}"
    )
    if (isAverage) {
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Розрахункові дні (середнє): ",
        valueText = reading.dayAvg.toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Розрахунковий Гкал: ",
        valueText = reading.gkalRasch.toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Споживання Гкал/день: ",
        valueText = reading.gkalDay.toString()
      )
    } else {
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Кількість днів: ",
        valueText = reading.days.toString()
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        LabelTextWithText(
          modifier = Modifier.weight(0.5f),
          labelText = "Попередні: ",
          valueText = reading.last.toString()
        )
        LabelTextWithText(
          modifier = Modifier.weight(0.5f),
          labelText = "Поточні: ",
          valueText = reading.current.toString()
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Об'єм (qty): ",
        valueText = reading.qty.toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Спожито теплоенергії: ",
        valueText = "${reading.gkal} Гкал"
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Діючий тариф: ",
        valueText = "${reading.tarif} грн/Гкал"
      )
    }
  }
}


