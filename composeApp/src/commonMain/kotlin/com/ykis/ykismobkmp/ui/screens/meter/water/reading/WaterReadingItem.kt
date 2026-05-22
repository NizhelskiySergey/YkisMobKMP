package com.ykis.ykismobkmp.ui.screens.meter.water.reading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.average
import ykismobkmp.composeapp.generated.resources.last_colon

/**
 * [WaterReadingItemContent] — Кроссплатформенный Stateless-компонент отрисовки полей строки истории водопостачання.
 * ИСПРАВЛЕНО: Ликвидирована синтаксическая ошибка закрытия скобок, мешавшая Gradle сборке.
 */
@Composable
fun WaterReadingItemContent(
  modifier: Modifier = Modifier,
  reading: WaterReadingEntity
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Прямое КМР-сравнение Int-флага (1 - Расчет по среднему нормативу биллинга ГИОЦ г. Южного, 0 - По прибору учета)
    if (reading.avg == 1L) {
      // Отображение полей начисления по среднему нормативу
      LabelTextWithText(
        labelText = stringResource(Res.string.average),
        valueText = reading.pokOt.toString()
      )
      LabelTextWithText(
        labelText = stringResource(Res.string.last_colon),
        valueText = reading.pokDo.toString()
      )
      LabelTextWithText(
        labelText = "Кількість кубів: ",
        valueText = reading.qtyKub.toString()
      )
      LabelTextWithText(
        labelText = "Розрахункові дні: ",
        valueText = reading.rday.toString()
      )
      LabelTextWithText(
        labelText = "Споживання на день: ",
        valueText = "${reading.kubDay} м³" // Бесшовная интерполяция строк Котлина
      )
    } else {
      // Стандартное отображение физического съема показаний водомера водоканала г. Южный
      LabelTextWithText(
        labelText = "Період нарахування: ",
        valueText = "${reading.dateOt} — ${reading.dateDo}"
      )
      LabelTextWithText(
        labelText = "Кількість днів: ",
        valueText = reading.days.toString()
      )

      // Внутренняя лента фиксации разницы кубометров
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        LabelTextWithText(
          modifier = Modifier.weight(0.5f),
          labelText = "Попередні: ",
          valueText = reading.last.toString()
        )
        LabelTextWithText(
          modifier = Modifier.weight(0.5f),
          labelText = "Поточні: ",
          valueText = reading.current.toString()
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      LabelTextWithText(
        labelText = "Використано кубів: ",
        valueText = "${reading.kub} м³"
      )
    }
  }
}

