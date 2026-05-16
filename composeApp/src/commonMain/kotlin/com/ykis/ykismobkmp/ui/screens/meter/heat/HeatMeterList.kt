package com.ykis.ykismobkmp.ui.screens.meter.heat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity


private const val className = "HeatMeterList"

/**
 * [HeatMeterList] — Кроссплатформенный Stateless-список счетчиков тепла ЮКИС.
 * Полностью очищен от Android-ресурсов R.string и готов к рендерингу на Mac Desktop и iOS.
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
      // ИСПРАВЛЕНО: Вместо stringResource(R.string) передаем чистые строки под Mac JVM
      EmptyListState(
        title = "Лічильники не знайдені",
        subtitle = "За вашою адресою у місті Южне не зафіксовано приладів обліку тепла"
      )
    } else {
      // Отрисовываем оптимизированную вертикальную ленту счетчиков
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(
          items = heatMeterState.heatMeterList,
          key = { it.teplomerId } // Проставляем КМР-ключ для оптимизации рекомпозиции LazyColumn
        ) { heatMeter ->
          HeatMeterItem(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 12.dp)
              .clip(CardDefaults.outlinedShape)
              .clickable {
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

