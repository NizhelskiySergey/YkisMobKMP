package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val className = "LabelTexts"

/**
 * [LabelTextWithText] — Строка вывода параметров (например, Модель: GLS 3).
 * ИСПРАВЛЕНО: Добавлен weight(1f) для создания аккуратной табличной верстки на Mac Desktop.
 */
@Composable
fun LabelTextWithText(
  modifier: Modifier = Modifier,
  labelText: String = "",
  valueText: String = ""
) {
  Row(
    modifier = modifier.fillMaxWidth(), // Растягиваем на всю ширину карточки
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelText,
      modifier = Modifier.weight(1f), // Занимает всё свободное место, прижимая значение вправо
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
    Text(
      text = valueText,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onSurface
      )
    )
  }
}

/**
 * [LabelTextWithTextAndIcon] — Строка вывода информации с иконкой (например, Телефон).
 */
@Composable
fun LabelTextWithTextAndIcon(
  modifier: Modifier = Modifier,
  labelText: String = "",
  valueText: String = "",
  imageVector: ImageVector
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = labelText,
      modifier = Modifier.weight(1f).padding(start = 8.dp), // Выравниваем текст
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
    Text(
      text = valueText,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onSurface
      )
    )
  }
}

/**
 * [LabelTextWithCheckBox] — Строка информационного чекбокса (например, Враховувати стоки).
 * ИСПРАВЛЕНО: onCheckedChange = null оставлен для режима Read-Only, добавлен весовой отступ.
 */
@Composable
fun LabelTextWithCheckBox(
  modifier: Modifier = Modifier,
  labelText: String,
  checked: Boolean
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelText,
      modifier = Modifier.weight(1f), // Текст слева
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
      )
    )
    Checkbox(
      checked = checked,
      onCheckedChange = null, // Компонент только для чтения (инфо-флаг биллинга)
      modifier = Modifier.size(24.dp), // Чекбокс аккуратно выровнен по правому краю
      colors = CheckboxDefaults.colors(
        disabledCheckedColor = MaterialTheme.colorScheme.primary,
        disabledUncheckedColor = MaterialTheme.colorScheme.outlineVariant
      )
    )
  }
}

/**
 * [ColumnLabelTextWithTextAndIcon] — Двухстрочный блок вывода (для длинных адресов или ФИО админов).
 */
@Composable
fun ColumnLabelTextWithTextAndIcon(
  modifier: Modifier = Modifier,
  labelText: String = "",
  valueText: String = "",
  imageVector: ImageVector? = null
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (imageVector != null) {
        Icon(
          imageVector = imageVector,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(20.dp)
        )
      }
      Text(
        text = labelText,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      )
    }
    Text(
      text = valueText,
      modifier = Modifier.padding(start = if (imageVector != null) 26.dp else 0.dp), // Сдвиг текста под иконку
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light // Стабильный КМР Light-шрифт
      )
    )
  }
}

