package com.ykis.ykismobkmp.ui.screens.meter.water.reading


import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState

private const val tag = "WaterReadings"
@Composable
private fun CenteredProgressIndicator(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(strokeWidth = 3.dp)
  }
}

/**
 * [WaterReadings] — Кроссплатформенный Stateless-компонент истории показаний водомера г. Южный.
 */
@Composable
fun WaterReadings(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  waterMeterState: WaterMeterState,
  getWaterReadings: () -> Unit
) {
  // Каскадный триггер загрузки истории начислений биллинга ГИОЦ г. Южный
  LaunchedEffect(key1 = baseUIState.addressId, key2 = waterMeterState.selectedWaterMeter) {
    if (baseUIState.addressId != 0L && waterMeterState.selectedWaterMeter.vodomerId != 0L) {
      println("[WaterReadings.LaunchedEffect]: Запит історії водопостачання для о/р Long: ${baseUIState.addressId}")
      getWaterReadings()
    }
  }

  Crossfade(
    targetState = waterMeterState.isReadingsLoading,
    label = "WaterReadingsLoadingFade",
    animationSpec = tween(durationMillis = 300, delayMillis = 100)
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        // Передаем коллекцию позиционным аргументом и внедряем Long-ключ pokId под SQLDelight
        items(
          items = waterMeterState.waterReadings,
          key = { it.pokId } // Наш сквозной Long ID первичного ключа таблицы waterReadingEntity
        ) { waterReading ->
          WaterReadingItem(reading = waterReading)
        }
      }
    }
  }
}

/**
 * [WaterReadingItem] — Дописанный компонент карточки для одной записи из истории водопостачання.
 */
@Composable
fun WaterReadingItem(
  modifier: Modifier = Modifier,
  reading: WaterReadingEntity
) {
  // Если флаг avg == 1, биллинг ЮКИС рассчитал месяц по среднему тарифу абонента Одесской обл.
  val cardLabel = if (reading.avg == 1) "Розрахунок за середнім нормативом" else null

  BaseCard(
    modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
    label = cardLabel
  ) {
    // Внутренний контент строки истории начислений (разница показаний, кубы, даты)
    WaterReadingItemContent(reading = reading)
  }
}

