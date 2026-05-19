package com.ykis.ykismobkmp.ui.screens.meter.water
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "WaterMeterItem"

/**
 * [WaterMeterItem] — Кроссплатформенная карточка водомера г. Южный.
 */
@Composable
fun WaterMeterItem(
  modifier: Modifier = Modifier,
  waterMeter: WaterMeterEntity
) {
  val statusText: String
  val alphaValue: Float

  // ИСПРАВЛЕНО: Прямое КМР-сравнение Int-флагов биллинга расчетного центра Южного (1 - Да, 0 - Нет)
  when {
    waterMeter.spisan == 1 -> {
      statusText = "Списаний"
      alphaValue = 0.4f
    }
    waterMeter.out_ == 1 -> { // ИСПРАВЛЕНО: Имя поля приведено к стандарту СУБД SQLDelight
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
        .alpha(alphaValue) // ИСПРАВЛЕНО: Теперь нативно распознается благодаря добавленному импорту draw.alpha
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
          color = if (waterMeter.spisan == 1 || waterMeter.out_ == 1)
            MaterialTheme.colorScheme.error
          else
            MaterialTheme.colorScheme.primary
        )
      }

      // ИСПРАВЛЕНО: Заменена отсутствующая ChevronRight на легитимную КМР AutoMirrored KeyboardArrowRight
      Icon(
        modifier = Modifier.padding(end = 12.dp),
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}


