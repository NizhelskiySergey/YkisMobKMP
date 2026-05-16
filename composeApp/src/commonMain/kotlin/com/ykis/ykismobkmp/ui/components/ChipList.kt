package com.ykis.ykismobkmp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val className = "GroupFilterChip"

/**
 * [FilterChipSample] — Индивидуальный КМР-чип выбора расчетного периода или категории ЖКХ.
 * Оптимизирован по модификаторам и готов к плавному изменению размеров при активации.
 */
@Composable
fun FilterChipSample(
  modifier: Modifier = Modifier,
  text: String,
  isSelected: Boolean = false,
  onClick: () -> Unit = {}
) {
  FilterChip(
    // ИСПРАВЛЕНО: Внутренние анимации и отступы изолированы от внешнего modifier
    modifier = modifier
      .padding(horizontal = 4.dp)
      .animateContentSize(),
    selected = isSelected,
    label = {
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge
      )
    },
    onClick = onClick,
    leadingIcon = if (isSelected) {
      {
        Icon(
          imageVector = Icons.Filled.Done,
          contentDescription = "Вибрано",
          modifier = Modifier.size(FilterChipDefaults.IconSize)
        )
      }
    } else {
      null
    }
  )
}

/**
 * [GroupFilterChip] — Кроссплатформенная горизонтальная лента чипсов Material 3 для фильтрации архивов оплат и чатов.
 * ИСПРАВЛЕНО: Тип Any? изменен на String?, оптимизирован вызов лямбд.
 */
@Composable
fun GroupFilterChip(
  modifier: Modifier = Modifier,
  list: List<String>,
  selectedChip: String? = null, // ИСПРАВЛЕНО: Строгая типизация String? вместо Any?
  onSelectedChanged: (String) -> Unit = {}
) {
  LazyRow(
    modifier = modifier
  ) {
    // ИСПРАВЛЕНО:items вызван с КМР-совместимым синтаксисом и текстовым ключом для стабильности 60 FPS
    items(
      items = list,
      key = { it } // Текст года/категории используется как уникальный ключ рекомпозиции
    ) { text ->
      FilterChipSample(
        text = text,
        isSelected = text == selectedChip,
        // ИСПРАВЛЕНО: Прямая передача значения без двойного оборачивания в замыкания
        onClick = { onSelectedChanged(text) }
      )
    }
  }
}

