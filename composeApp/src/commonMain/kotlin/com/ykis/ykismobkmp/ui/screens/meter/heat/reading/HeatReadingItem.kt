package com.ykis.ykismobkmp.ui.screens.meter.heat.reading
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
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.ui.components.LabelTextWithText

private const val tag = "HeatReadingItemContent"

/**
 * [HeatReadingItemContent] — Кроссплатформенный Stateless-компонент отрисовки полей строки истории опалення.
 * ИСПРАВЛЕНО: Добавлен явный вызов .toString() для числовых параметров начисления по среднему нормативу.
 */
@Composable
fun HeatReadingItemContent(
  modifier: Modifier = Modifier,
  reading: HeatReadingEntity,
  isAverage: Boolean
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Нативная интерполяция строк Котлина взамен Android R.string
    LabelTextWithText(
      modifier = Modifier.padding(vertical = 2.dp),
      labelText = "Період нарахування: ",
      valueText = "${reading.dateOt} — ${reading.dateDo}"
    )

    if (isAverage) {
      // Отображение полей начисления по среднему нормативу теплосети г. Южного
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Розрахункові дні (середнє): ",
        valueText = reading.dayAvg.toString() // ИСПРАВЛЕНО: Добавлено явное приведение типов .toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Розрахунковий Гкал: ",
        valueText = reading.gkalRasch.toString() // ИСПРАВЛЕНО: Добавлено явное приведение типов .toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Споживання Гкал/день: ",
        valueText = reading.gkalDay.toString() // ИСПРАВЛЕНО: Добавлено явное приведение типов .toString()
      )
    } else {
      // Стандартное отображение физического съема показаний тепломера
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Кількість днів: ",
        valueText = reading.days.toString()
      )

      // Внутренняя лента фиксации разницы показаний
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
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Об'єм (qty): ",
        valueText = reading.qty.toString()
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Спожито теплоенергії: ",
        valueText = "${reading.gkal} Гкал"
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Діючий тариф: ",
        valueText = "${reading.tarif} грн/Гкал"
      )
    }
  }
}

