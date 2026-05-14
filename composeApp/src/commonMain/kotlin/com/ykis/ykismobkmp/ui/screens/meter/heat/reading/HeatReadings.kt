package com.ykis.ykismobkmp.ui.screens.meter.heat.reading

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator

import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState

private const val className = "HeatReadings"

/**
 * [HeatReadings] — Кроссплатформенный Stateless-компонент истории показаний счетчика тепла г. Южный.
 * Полностью автономен, избавлен от внутренней логики и готов к рендерингу на любой ОС.
 */
@Composable
fun HeatReadings(
  modifier: Modifier = Modifier,
  heatMeterState: HeatMeterState
) {
  Crossfade(
    targetState = heatMeterState.isReadingsLoading,
    label = "HeatReadingsLoadingFade",
    animationSpec = tween(durationMillis = 300, delayMillis = 100)
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        // ИСПРАВЛЕНО: Передаем коллекцию позиционным аргументом и внедряем Long-ключ pokId
        items(
          items = heatMeterState.heatReadings,
          key = { it.pokId } // Наш сквозной Long ID первичного ключа таблицы heatReadingEntity
        ) { heatReading ->
          HeatReadingItem(reading = heatReading)
        }
      }
    }
  }
}

/**
 * [HeatReadingItem] — Контейнер карточки для одной записи из истории опалення.
 */
@Composable
fun HeatReadingItem(
  modifier: Modifier = Modifier,
  reading: com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Внедряем твой кастомный КМР-компонент отрисовки полей строки истории тепла
      HeatReadingItemContent(
        reading = reading,
        isAverage = reading.avg == 1 // ИСПРАВЛЕНО: Прямое КМР Int сравнение флага
      )
    }
  }
}

