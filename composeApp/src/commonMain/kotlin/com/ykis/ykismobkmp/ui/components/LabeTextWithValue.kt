package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log

private const val className = "LabelTextsKt"

@Composable
fun LabelTextWithText(
  modifier: Modifier = Modifier,
  labelText: String = "",
  valueText: String = ""
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelText,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
    Text(
      modifier = Modifier.padding(start = 8.dp),
      text = valueText,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light
      )
    )
  }
}

@Composable
fun LabelTextWithTextAndIcon(
  modifier: Modifier = Modifier,
  labelText: String = "",
  valueText: String = "",
  imageVector: ImageVector
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.size(18.dp), // Фиксированный размер для аккуратности
      imageVector = imageVector,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary
    )

    Text(
      modifier = Modifier.padding(start = 4.dp),
      text = labelText,
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Normal
      )
    )
    Text(
      modifier = Modifier.padding(start = 8.dp),
      text = valueText,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light
      )
    )
  }
}

@Composable
fun LabelTextWithCheckBox(
  modifier: Modifier = Modifier,
  labelText: String,
  checked: Boolean,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelText,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Normal
      )
    )
    Checkbox(
      modifier = Modifier
        .padding(start = 4.dp)
        .size(24.dp),
      checked = checked,
      onCheckedChange = null // Компонент только для чтения (инфо)
    )
  }
}

@Composable
fun ColumnLabelTextWithTextAndIcon(
  modifier: Modifier = Modifier,
  labelText: String = "",
  valueText: String = "",
  imageVector: ImageVector? = null
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (imageVector != null) {
        Icon(
          modifier = Modifier.size(20.dp),
          imageVector = imageVector,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary
        )
      }
      Text(
        text = labelText,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium
        )
      )
    }
    Text(
      text = valueText,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light // Изменил Thin на Light для Mac
      )
    )
  }
}
