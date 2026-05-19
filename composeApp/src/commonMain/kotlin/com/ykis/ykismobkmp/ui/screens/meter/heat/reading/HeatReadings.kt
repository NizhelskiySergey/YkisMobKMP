package com.ykis.ykismobkmp.ui.screens.meter.heat.reading
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState

// Переиспользуем твой локальный легковесный центрированный лоадер
@Composable
private fun CenteredProgressIndicator(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(strokeWidth = 3.dp)
  }
}

private const val className = "HeatReadings"

/**
 * [HeatReadings] — Кроссплатформенный Stateless-компонент истории показаний счетчика тепла г. Южный.
 * добавлен LaunchedEffect каскадного опроса сетевого Ktor API теплосети.
 */
@Composable
fun HeatReadings(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,       // ИСПРАВЛЕНО: Возвращен обязательный сквозной КМР-параметр
  heatMeterState: HeatMeterState,
  getHeatReadings: () -> Unit     // ИСПРАВЛЕНО: Возвращен обязательный сквозной КМР-параметр
) {
  // Каскадный триггер загрузки истории начислений биллинга теплосети г. Южный
  LaunchedEffect(baseUIState.addressId, heatMeterState.selectedHeatMeter.teplomerId) {
    if (baseUIState.addressId != 0L && heatMeterState.selectedHeatMeter.teplomerId != 0L) {
      println("[$className.LaunchedEffect]: Оновлення історії опалення для рахунку Long: ${baseUIState.addressId}")
      getHeatReadings()
    }
  }

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
        // Передаем коллекцию позиционным аргументом и внедряем Long-ключ pokId под SQLDelight
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
  reading: HeatReadingEntity
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
        isAverage = reading.avg == 1 // Прямое КМР Int сравнение флага
      )
    }
  }
}


