package com.ykis.ykismobkmp.ui.screens.service.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val className = "ServiceTableComponents"

/**
 * [HeaderInTable] — Кроссплатформенный заголовок колонки финансовой таблицы начислений ЮКИС.
 */
@Composable
fun HeaderInTable(
  text: String,
  modifier: Modifier = Modifier,
  textAlign: TextAlign = TextAlign.Start
) {
  Text(
    // ИСПРАВЛЕНО: Внутренний fillMaxWidth аккуратно расширяет переданную область веса родителя
    modifier = modifier.fillMaxWidth(),
    text = text,
    style = MaterialTheme.typography.titleSmall.copy(
      fontWeight = FontWeight.Normal
    ),
    textAlign = textAlign,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}

/**
 * [ColumnItemInTable] — Адаптивная вертикальная колонка для ячеек тарифов биллинга города Южный.
 * ИСПРАВЛЕНО: Устранено дублирование modifier, исправлено логическое сравнение строковых null-маркеров.
 */
@Composable
fun ColumnItemInTable(
  modifier: Modifier = Modifier,
  alignment: Alignment.Horizontal,
  value1: String,
  value2: String,
  value3: String,
  value4: String,
  header: String,
  summary: String,
  headerAlign: TextAlign
) {
  // Вся колонка получает входящий modifier (например, вес .weight(1f) внутри Row таблицы)
  Column(
    horizontalAlignment = alignment,
    verticalArrangement = Arrangement.spacedBy(13.dp),
    modifier = modifier.padding(horizontal = 4.dp)
  ) {
    // ИСПРАВЛЕНО: Вложенные вызовы больше не наследуют тяжелый внешний modifier напрямую
    HeaderInTable(
      text = header,
      textAlign = headerAlign
    )

    // ИСПРАВЛЕНО: Оператор !== заменен на КМР-совместимое безопасное сравнение строк
    if (!value1.isBillNullOrNone()) {
      Text(
        text = value1,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    if (!value2.isBillNullOrNone()) {
      Text(
        text = value2,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    if (!value3.isBillNullOrNone()) {
      Text(
        text = value3,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    if (!value4.isBillNullOrNone()) {
      Text(
        text = value4,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    HeaderInTable(
      text = summary,
      textAlign = TextAlign.End
    )
  }
}

/**
 * [TableDivider] — Горизонтальный разделитель строк финансовой таблицы.
 */
@Composable
fun TableDivider(
  previousValue: String? = null,
  modifier: Modifier = Modifier
) {
  // ИСПРАВЛЕНО: Проверка nullable-строки приведена к КМР-стандарту во избежание пустых линий в UI
  if (!previousValue.isBillNullOrNone()) {
    HorizontalDivider(
      modifier = modifier
        .fillMaxWidth()
        .alpha(0.3f)
        .padding(horizontal = 8.dp),
      color = MaterialTheme.colorScheme.onSecondaryContainer
    )
  }
}

/**
 * [isBillNullOrNone] — Безопасное КМР-расширение фильтрации пустых текстовых ответов PHP-бэкенда.
 */
private fun String?.isBillNullOrNone(): Boolean {
  if (this == null || this.isBlank()) return true
  return this.equals("null", ignoreCase = true) || this.equals("none", ignoreCase = true)
}

