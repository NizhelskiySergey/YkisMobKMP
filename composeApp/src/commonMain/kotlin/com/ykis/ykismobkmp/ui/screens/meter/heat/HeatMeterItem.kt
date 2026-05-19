package com.ykis.ykismobkmp.ui.screens.meter.heat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.ic_heat_meter5_24px

private const val className = "HeatMeterItem"

/**
 * [HeatMeterItem] — Кроссплатформенная карточка счетчика тепла г. Южный.
 * ИСПРАВЛЕНО: Добавлен импорт draw.alpha, ложная иконка ChevronRight заменена на КМР-совместимую KeyboardArrowRight.
 */
@Composable
fun HeatMeterItem(
  modifier: Modifier = Modifier,
  heatMeter: HeatMeterEntity
) {
  val statusText: String
  val componentAlpha: Float

  // Прямое КМР-сравнение Int-флагов биллинга расчетного центра Южного (1 - Да, 0 - Нет)
  when {
    heatMeter.spisan == 1 -> {
      statusText = "Списаний"
      componentAlpha = 0.5f
    }
    heatMeter.out_ == 1 -> {
      statusText = "На повірці"
      componentAlpha = 0.5f
    }
    else -> {
      statusText = "Працює"
      componentAlpha = 1f
    }
  }

  // Применяем прозрачность альфа-канала ко всей карточке, если прибор учета списан или на поверке
  OutlinedCard(
    modifier = modifier.alpha(componentAlpha), // ИСПРАВЛЕНО: Теперь нативно распознается компилятором
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ИСПРАВЛЕНО: Иконка тепла рендерится через кроссплатформенный генератор ресурсов Res
      Icon(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(48.dp),
        painter = painterResource(Res.drawable.ic_heat_meter5_24px),
        contentDescription = null,
        tint = if (heatMeter.work == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
          color = if (heatMeter.spisan == 1 || heatMeter.out_ == 1)
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

