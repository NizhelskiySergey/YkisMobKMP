package com.ykis.ykismobkmp.ui.screens.ledger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * [TableCell] — Компактная ячейка финансовой таблицы биллинга ЮКИС.
 * ИСПРАВЛЕНО: Убрана жесткая ширина, добавлена поддержка весов (Modifier.weight).
 */
@Composable
fun RowScope.TableCell(
  text: String,
  modifier: Modifier = Modifier,
  textAlign: TextAlign = TextAlign.End, // Стандарт для цифр
  isHeader: Boolean = false,
  isSummary: Boolean = false,
  weight: Float = 1f
) {
  Text(
    text = text,
    modifier = modifier
      .weight(weight)
      .fillMaxWidth() // ДОДАНО: Гарантуємо, що текст займає всю ширину комірки для коректного TextAlign
      .padding(horizontal = 2.dp, vertical = 6.dp),
    style = when {
      isHeader -> MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold, 
        fontSize = 11.sp
      )
      isSummary -> MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Bold, 
        fontSize = 12.sp
      )
      else -> MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Normal, 
        fontSize = 12.sp
      )
    },
    textAlign = textAlign, // Пряме застосування вирівнювання
    color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    maxLines = 2,
    softWrap = true,
    overflow = TextOverflow.Ellipsis
  )
}

/**
 * [TableDivider] — Горизонтальный разделитель строк финансовой таблицы.
 */
@Composable
fun TableDivider(
  modifier: Modifier = Modifier
) {
  HorizontalDivider(
    modifier = modifier
      .fillMaxWidth()
      .alpha(0.2f),
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.outline
  )
}

/**
 * [isBillNullOrNone] — Безопасное КМР-расширение фильтрации пустых текстовых ответов PHP-бэкенда.
 */
fun String?.isBillNullOrNone(): Boolean {
  if (this == null || this.isBlank()) return true
  val s = this.trim()
  return s.equals("null", ignoreCase = true) || 
         s.equals("none", ignoreCase = true) || 
         s == "0" || s == "0.0" || s == "0.00"
}
