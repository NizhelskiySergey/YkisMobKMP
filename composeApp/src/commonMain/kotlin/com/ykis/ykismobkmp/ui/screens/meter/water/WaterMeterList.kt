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
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.no_meters
import ykismobkmp.composeapp.generated.resources.no_water_meters
import ykismobkmp.composeapp.generated.resources.*

private const val className = "WaterMeterList"

@Composable
fun WaterMeterList(
  modifier: Modifier = Modifier,
  meterUIState: BaseUIState,
  onWaterMeterClick: (WaterMeterEntity) -> Unit
) {
  Crossfade(
    targetState = meterUIState.isMetersLoading,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "WaterMeterListFade"
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else if (meterUIState.waterMeterList.isEmpty()) {
      EmptyListState(
        title = stringResource(Res.string.no_meters),
        subtitle = stringResource(Res.string.no_water_meters),
        photoUrl = meterUIState.photoUrl
      )
    } else {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
      ) {
        items(
          items = meterUIState.waterMeterList,
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
      statusText = stringResource(Res.string.written_off)
      alphaValue = 0.4f
    }
    waterMeter.isOut == 1L -> {
      statusText = stringResource(Res.string.on_the_test)
      alphaValue = 0.4f
    }
    else -> {
      statusText = stringResource(Res.string.works)
      alphaValue = 1f
    }
  }

  OutlinedCard(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer
    ),
  ) {
    Row(
      modifier = Modifier
        .graphicsLayer { alpha = alphaValue }
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(48.dp),
        imageVector = Icons.Default.WaterDrop,
        contentDescription = null,
        tint = if (waterMeter.voda.contains("гар", ignoreCase = true))
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
          labelText = stringResource(Res.string.number),
          valueText = waterMeter.nomer
        )
        Spacer(modifier = Modifier.height(2.dp))
        LabelTextWithText(
          labelText = stringResource(Res.string.place),
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
