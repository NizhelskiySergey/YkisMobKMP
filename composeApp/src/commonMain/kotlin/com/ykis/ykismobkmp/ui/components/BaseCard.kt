package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res
import android.util.Log
import com.ykis.mob.ui.components.LabelTextWithCheckBox
import com.ykis.mob.ui.components.LabelTextWithText

private const val className = "BaseCardKt"

/**
 * [BaseCard] — универсальный контейнер для блоков информации (БТИ, счетчики).
 * Использует Outlined стиль для единообразия на Mac и Android.
 */
@Composable
fun BaseCard(
  cardModifier: Modifier = Modifier
    .fillMaxWidth()
    .padding(vertical = 6.dp, horizontal = 12.dp),
  columnModifier: Modifier = Modifier
    .fillMaxWidth()
    .padding(16.dp),
  labelModifier: Modifier = Modifier,
  label: String? = null,
  actionButton: @Composable (() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  // Логируем отрисовку только если есть заголовок (для идентификации блока)
  if (label != null) {
    Log.d("YkisLog", "[$className.BaseCard]: Rendering block -> $label")
  }

  OutlinedCard(
    modifier = cardModifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border = BorderStroke(
      width = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
  ) {
    Column(
      modifier = columnModifier,
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
      content()
    }
  }
}

@Preview
@Composable
private fun PreviewBaseCard() {
  YkisPAMTheme {
    BaseCard(
      label = "Останні показання" // В превью используем строки напрямую
    ) {
      LabelTextWithText(
        labelText = "Модель:",
        valueText = "GLS 3 ULTRA"
      )
      LabelTextWithCheckBox(
        labelText = "Стоки:",
        checked = true
      )
    }
  }
}
