package com.ykis.ykismobkmp.ui.screens.meter.heat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.ui.components.EmptyListState

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

// Легковесный КМР-компонент отображения пустого состояния списка
@Composable
private fun EmptyListState(title: String, subtitle: String) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
  }
}

// Временная заглушка элемента карточки тепла, пока не присланы сорцы ее верстки

private const val className = "HeatMeterList"

/**
 * [HeatMeterList] — Кроссплатформенный Stateless-список счетчиков тепла ЮКИС.
 * ИСПРАВЛЕНО: Свойство CardDefaults.outlinedShape заменено на стандартный КМР-совместимый CardDefaults.shape.
 */
@Composable
fun HeatMeterList(
  modifier: Modifier = Modifier,
  heatMeterState: HeatMeterState,
  onHeatMeterClick: (HeatMeterEntity) -> Unit
) {
  // Используем кроссплатформенный Crossfade для плавной смены состояний экрана
  Crossfade(
    targetState = heatMeterState.isMetersLoading,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "HeatMeterListFade"
  ) { isLoading ->
    if (isLoading) {
      // Показываем индикатор прогресса по центру холста
      CenteredProgressIndicator()
    } else if (heatMeterState.heatMeterList.isEmpty()) {
      // Предотвращаем падения на Mac JVM из-за легаси Android-ресурсов
      EmptyListState(
        title = "Лічильники не знайдені",
        subtitle = "За вашою адресою у місті Южне не зафіксовано приладів обліку тепла"
      )
    } else {
      // Отрисовываем оптимизированную вертикальную ленту счетчиков тепла г. Южного
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(
          items = heatMeterState.heatMeterList,
          // Проставляем КМР-ключ на базе Long ID для оптимизации рекомпозиции LazyColumn в ОЗУ
          key = { it.teplomerId }
        ) { heatMeter ->
          HeatMeterItem(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 12.dp)
              // ИСПРАВЛЕНО: Заменено на каноничное свойство CardDefaults.shape
              .clip(CardDefaults.shape)
              .clickable {
                println("[$className.Content]: Выбран тепломер ID Long: ${heatMeter.teplomerId}")
                // Безопасно передаем Long-сущность тепломера в callback клика родителя
                onHeatMeterClick(heatMeter)
              },
            heatMeter = heatMeter
          )
        }
      }
    }
  }
}


