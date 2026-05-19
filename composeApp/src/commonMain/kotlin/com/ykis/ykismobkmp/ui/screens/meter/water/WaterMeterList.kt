package com.ykis.ykismobkmp.ui.screens.meter.water

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity

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

private const val className = "WaterMeterList"

/**
 * [WaterMeterList] — Кроссплатформенный Stateless-список водомеров города Южный.
 * ИСПРАВЛЕНО: Свойство CardDefaults.outlinedShape заменено на стандартный КМР-совместимый CardDefaults.shape.
 */
@Composable
fun WaterMeterList(
  modifier: Modifier = Modifier,
  waterMeterState: WaterMeterState,
  onWaterMeterClick: (WaterMeterEntity) -> Unit
) {
  // Используем кроссплатформенный Crossfade для плавной смены состояний экрана
  Crossfade(
    targetState = waterMeterState.isMetersLoading,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "WaterMeterListFade"
  ) { isLoading ->
    if (isLoading) {
      // Показываем индикатор прогресса по центру холста
      CenteredProgressIndicator()
    } else {
      // Отрисовываем вертикальную ленту счетчиков воды водоканала г. Южного
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
      ) {
        items(
          items = waterMeterState.waterMeterList,
          // Проставляем КМР-ключ на базе Long ID для оптимизации рекомпозиции в ОЗУ
          key = { it.vodomerId }
        ) { waterMeter ->
          WaterMeterItem(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 12.dp)
              // ИСПРАВЛЕНО: Заменено на каноничное свойство CardDefaults.shape
              .clip(CardDefaults.shape)
              .clickable {
                println("[$className.Content]: Выбран водомер ID Long: ${waterMeter.vodomerId}")
                // Передаем Long-сущность водомера в callback клика родителя
                onWaterMeterClick(waterMeter)
              },
            waterMeter = waterMeter
          )
        }
      }
    }
  }
}


