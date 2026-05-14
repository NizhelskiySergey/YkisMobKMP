package com.ykis.ykismobkmp.ui.screens.meter.water

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ РЕСУРСОВ JETBRAINS COMPOSE:
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_water_meter9_24px

private const val className = "WaterMeterItem"

/**
 * [WaterMeterItem] — Кроссплатформенная карточка водомера г. Южный.
 * Полностью стабильна на Mac Desktop (JVM), Android и iOS без привязок к Android SDK.
 */
@Composable
fun WaterMeterItem(
  modifier: Modifier = Modifier,
  waterMeter: WaterMeterEntity
) {
  val statusText: String
  val alpha: Float

  // ИСПРАВЛЕНО: Прямое КМР-сравнение Int-флагов биллинга расчетного центра Южного (1 - Да, 0 - Нет)
  when {
    waterMeter.spisan == 1 -> {
      statusText = "Списаний"
      alpha = 0.4f
    }
    waterMeter.out == 1 -> {
      statusText = "На повірці"
      alpha = 0.4f
    }
    waterMeter.paused == 1 -> {
      statusText = "Призупинено"
      alpha = 0.4f
    }
    else -> {
      statusText = "Працює"
      alpha = 1f
    }
  }

  OutlinedCard(
    modifier = modifier,
    // ИСПРАВЛЕНО: surfaceColorAtElevation заменен на стабильный контейнер Material 3 Compose Multiplatform
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
  ) {
    Row(
      modifier = Modifier
        .alpha(alpha)
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ИСПРАВЛЕНО: Иконка водомера рендерится через кроссплатформенный генератор ресурсов Res
      Icon(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(48.dp),
        painter = painterResource(Res.drawable.ic_water_meter9_24px),
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
          color = if (waterMeter.spisan == 1 || waterMeter.out == 1 || waterMeter.paused == 1)
            MaterialTheme.colorScheme.error
          else
            MaterialTheme.colorScheme.primary
        )
      }

      Icon(
        modifier = Modifier.padding(end = 12.dp),
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

