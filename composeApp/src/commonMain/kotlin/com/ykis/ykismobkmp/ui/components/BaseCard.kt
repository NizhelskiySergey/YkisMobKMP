package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val className = "BaseCard"

/**
 * [BaseCard] — Универсальный кроссплатформенный контейнер информационных блоков ЮКИС.
 * Использует Outlined-стиль Material 3 для визуального единообразия на Mac, Android и iOS.
 */
@Composable
fun BaseCard(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Внутренние отступы колонки контента вынесены в чистый Modifier
  contentModifier: Modifier = Modifier.fillMaxWidth().padding(16.dp),
  labelModifier: Modifier = Modifier,
  label: String? = null,
  actionButton: @Composable (() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  // ИСПРАВЛЕНО: Нативный Log.d заменен на универсальный println() внутри LaunchedEffect
  LaunchedEffect(label) {
    if (label != null) {
      println("[$className.BaseCard]: Rendering block -> $label")
    }
  }

  OutlinedCard(
    // Базовые внешние отступы теперь задаются на месте вызова, а карточка принимает чистый modifier
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp), // Скругление приведено к стандартному Material 3 Medium/Large
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border = BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
  ) {
    Column(
      modifier = contentModifier,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (label != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            modifier = labelModifier
              .weight(1f)
              .padding(bottom = 2.dp),
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          )
          actionButton?.invoke()
        }
      }
      // Рендерим вложенную ЖКХ-верстку БТИ или истории
      content()
    }
  }
}

