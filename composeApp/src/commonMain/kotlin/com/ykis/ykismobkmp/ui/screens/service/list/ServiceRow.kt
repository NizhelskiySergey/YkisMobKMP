package com.ykis.ykismobkmp.ui.screens.service.list

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.uah

private const val className = "ServiceRow"

/**
 * [ServiceRow] — Кроссплатформенный элемент строки отображения баланса и долга по конкретной ЖКХ-службе г. Южный.
 * Полностью стабилен на Mac Desktop (JVM), Android и iOS без побочных эффектов дублирования модификаторов.
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
  // Используем наш кроссплатформенный хелпер форматирования копеек биллинга
  val formattedDebt = formatDebtKmp(debt)

  // ИСПРАВЛЕНО: Кликабельность перенесена на корневой Box, semantics очищена от платформозависимых вызовов
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

      // Цветовой индикатор состояния задолженности
      ServiceIndicator(
        color = color,
        modifier = Modifier
      )

      // ИСПРАВЛЕНО: Внутренние отступы иконки изолированы от входящего modifier
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
          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
          color = if (debt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
          // ИСПРАВЛЕНО: Заменен Android R.string.uah на КМР Res.string.uah
          text = stringResource(Res.string.uah),
          style = typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = null,
        modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(20.dp),
        tint = MaterialTheme.colorScheme.outline
      )
    }
  }

  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
}

/**
 * [formatDebtKmp] — Кроссплатформенное форматирование вывода копеек долга/переплаты.
 */
private fun formatDebtKmp(debt: Double): String {
  val rounded = (debt * 100.0).toLong()
  val mainPart = rounded / 100
  val kopecks = rounded % 100
  val kopecksStr = if (kopecks < 10) "0$kopecks" else "$kopecks"
  return "$mainPart.$kopecksStr"
}

/**
 * [ServiceIndicator] — Кроссплатформенный цветовой маркер состояния задолженности ЖКХ-службы.
 * Полностью автономен, изолирован по модификаторам и готов к сборке на Mac Desktop.
 */
@Composable
fun ServiceIndicator(
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    // ИСПРАВЛЕНО: Цепочка расширений начинается с входящего modifier, но внутренние свойства изолированы
    modifier = modifier
      .size(width = 6.dp, height = 36.dp)
      .clip(MaterialTheme.shapes.small)
      .background(color = color)
  )
}
