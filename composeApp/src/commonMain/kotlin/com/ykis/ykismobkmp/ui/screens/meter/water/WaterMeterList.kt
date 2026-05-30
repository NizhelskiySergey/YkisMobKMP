package com.ykis.ykismobkmp.ui.screens.meter.water

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_water_meter9_24px
private const val className = "WaterMeterList"

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
fun WaterMeterList(
  modifier: Modifier = Modifier,
  waterMeterState: WaterMeterState,
  onWaterMeterClick: (WaterMeterEntity) -> Unit
) {
  Crossfade(
    targetState = waterMeterState.isMetersLoading,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "WaterMeterListFade"
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
      ) {
        items(
          items = waterMeterState.waterMeterList,
          key = { it.vodomerId }
        ) { waterMeter ->
          WaterMeterItem(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 12.dp)
              .clip(CardDefaults.shape)
              .clickable {
                println("[$className.Content]: Выбран водомер ID Long: ${waterMeter.vodomerId}")
                onWaterMeterClick(waterMeter)
              },
            waterMeter = waterMeter
          )
        }
      }
    }
  }
}

@Composable
fun WaterMeterItem(
  modifier: Modifier = Modifier,
  waterMeter: WaterMeterEntity
) {
  val statusText: String
  val alphaValue: Float
  when {
    waterMeter.spisan == 1L -> {
      statusText = "Списаний"
      alphaValue = 0.4f
    }
    waterMeter.isOut == 1L -> {
      statusText = "На повірці"
      alphaValue = 0.4f
    }
    else -> {
      statusText = "Працює"
      alphaValue = 1f
    }
  }

  OutlinedCard(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
  ) {
    Row(
      modifier = Modifier
        .graphicsLayer { alpha = alphaValue }
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ИСПРАВЛЕНО НАМЕРТВО: Вырезан painterResource() забагованного XML-файла ic_water_meter9_24px!
      // Подключен стабильный кроссплатформенный вектор Icons.Default.WaterDrop (Капля воды ЮКІС).
      // Краш Invalid color value @android:color/white уничтожен полностью!
      Icon(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(48.dp),
        imageVector = Icons.Default.WaterDrop,
        contentDescription = null,
        tint = if (waterMeter.voda?.contains("гар", ignoreCase = true) == true)
          MaterialTheme.colorScheme.error
        else
          MaterialTheme.colorScheme.primary
      )

      Column(
        modifier = Modifier.weight(1f).padding(start = 4.dp)
      ) {
        Text(
          text = waterMeter.model,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        LabelTextWithText(
          labelText = "Номер: ",
          valueText = waterMeter.nomer
        )
        Spacer(modifier = Modifier.height(2.dp))
        LabelTextWithText(
          labelText = "Місце: ",
          valueText = waterMeter.place
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodyMedium,
          color = if (waterMeter.spisan == 1L || waterMeter.isOut == 1L)
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



