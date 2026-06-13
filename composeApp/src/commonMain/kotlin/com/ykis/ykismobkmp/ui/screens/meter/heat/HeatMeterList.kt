package com.ykis.ykismobkmp.ui.screens.meter.heat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.no_heat_meters
import ykismobkmp.composeapp.generated.resources.no_meters
import ykismobkmp.composeapp.generated.resources.no_water_meters

private const val className = "HeatMeterList"

@Composable
fun HeatMeterList(
  modifier: Modifier = Modifier,
  meterUIState: BaseUIState,
  onHeatMeterClick: (HeatMeterEntity) -> Unit
) {
  Crossfade(
    targetState = meterUIState.isMetersLoading,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "HeatMeterListFade"
  ) { isLoading ->
    if (isLoading) {
      com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator()
    } else if (meterUIState.heatMeterList.isEmpty()) {
      // ИСПРАВЛЕНО: Добавлен аватар в состояние пустого списка
      EmptyListState(
        title = stringResource(Res.string.no_meters),
        subtitle = stringResource(Res.string.no_heat_meters),
        photoUrl = meterUIState.photoUrl
      )
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(
          items = meterUIState.heatMeterList,
          key = { it.teplomerId }
        ) { heatMeter ->
          HeatMeterItem(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 12.dp)
              .clip(CardDefaults.shape)
              .clickable {
                println("[$className.Content]: Выбран тепломер ID Long: ${heatMeter.teplomerId}")
                onHeatMeterClick(heatMeter)
              },
            heatMeter = heatMeter
          )
        }
      }
    }
  }
}

@Composable
fun HeatMeterItem(
  modifier: Modifier = Modifier,
  heatMeter: HeatMeterEntity
) {
  val statusText: String
  val componentAlpha: Float
  when {
    heatMeter.spisan == 1L -> {
      statusText = "Списаний"
      componentAlpha = 0.5f
    }
    heatMeter.isOut == 1L -> {
      statusText = "На повірці"
      componentAlpha = 0.5f
    }
    else -> {
      statusText = "Працює"
      componentAlpha = 1f
    }
  }

  OutlinedCard(
    modifier = modifier.graphicsLayer { alpha = componentAlpha },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ИСПРАВЛЕНО НАМЕРТВО: Вырезан painterResource забагованной XML-иконки теплосети!
      // Подключен стабильный кроссплатформенный вектор Icons.Default.LocalFireDepartment (Тепло ЮКІС).
      // Любые NullPointerException и сбои парсинга цвета полностью уничтожены!
      Icon(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(48.dp),
        imageVector = Icons.Default.LocalFireDepartment,
        contentDescription = null,
        tint = if (heatMeter.work == 1L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
      )

      Column(
        modifier = Modifier.weight(1f).padding(start = 4.dp)
      ) {
        Text(
          text = heatMeter.model,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        LabelTextWithText(
          labelText = "Номер: ",
          valueText = heatMeter.number
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodyMedium,
          color = if (heatMeter.spisan == 1L || heatMeter.isOut == 1L)
            MaterialTheme.colorScheme.error
          else
            MaterialTheme.colorScheme.primary
        )
      }

      Icon(
        modifier = Modifier.padding(end = 12.dp),
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

