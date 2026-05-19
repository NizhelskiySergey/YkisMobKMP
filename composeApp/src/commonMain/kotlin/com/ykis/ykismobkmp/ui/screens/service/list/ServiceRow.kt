package com.ykis.ykismobkmp.ui.screens.service.list
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.uah

private const val className = "ServiceRow"

/**
 * [ServiceRow] — Кроссплатформенный элемент строки отображения баланса и долга по конкретной ЖКХ-службе г. Южный.
 */
@Composable
fun ServiceRow(
  modifier: Modifier = Modifier,
  color: Color,
  title: String,
  debt: Double,
  icon: ImageVector,
  onClick: () -> Unit
) {
  // Вызов утилиты перевода копеек, запечатанной в истории проекта
  val rounded = (debt * 100.0).toLong()
  val mainPart = rounded / 100
  val kopecks = rounded % 100
  val kopecksStr = if (kopecks < 10) "0$kopecks" else "$kopecks"
  val formattedDebt = "$mainPart.$kopecksStr"

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .semantics(mergeDescendants = true) {
        contentDescription = "Ваш борг для компанії $title становить $formattedDebt гривень"
      }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 8.dp)
        .height(54.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val typography = MaterialTheme.typography

      // Цветовой индикатор состояния задолженности ЖКХ расчетного центра
      ServiceIndicator(
        color = color,
        modifier = Modifier
      )

      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(horizontal = 12.dp).size(24.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = title,
          style = typography.titleMedium,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formattedDebt,
          style = typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = if (debt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
          text = stringResource(Res.string.uah), // КМР-ресурс валюты гривны JetBrains Res
          style = typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // ИСПРАВЛЕНО: Заменена отсутствующая ChevronRight на легитимную КМР AutoMirrored KeyboardArrowRight
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(20.dp),
        tint = MaterialTheme.colorScheme.outline
      )
    }
  }

  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
}

/**
 * [ServiceIndicator] — Кроссплатформенный цветовой маркер состояния задолженности ЖКХ-службы.
 */
@Composable
fun ServiceIndicator(
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(width = 6.dp, height = 36.dp)
      .clip(MaterialTheme.shapes.small)
      .background(color = color)
  )
}
