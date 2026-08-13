package com.ykis.ykismobkmp.ui.screens.ledger.list
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.*

private const val className = "ServiceListStateless"

@Composable
fun KmpAnimatedCircle(
  proportions: List<Float>,
  colors: List<Color>,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    var startAngle = -90f // Начинаем отрисовку секторов биллинга с верхней точки круга (12 часов)

    proportions.forEachIndexed { index, proportion ->
      val sweepAngle = proportion * 360f
      val color = colors.getOrNull(index) ?: Color.Gray

      drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false, // Рисуем полый круг (Donut Chart) в стиле Material 3
        style = Stroke(width = 24.dp.toPx()) // Толщина кольца диаграммы задолженностей ГИОЦ
      )
      startAngle += sweepAngle
    }
  }
}

@Composable
fun <T> ServiceListStateless(
  modifier: Modifier = Modifier,
  items: List<T>,
  colors: (T) -> Color,
  debts: (T) -> Double,
  total: Double,
  circleLabel: String,
  onPayClick: (() -> Unit)? = null, // Новое действие для оплаты
  isPayEnabled: Boolean = true,      // Состояние активности кнопки
  rows: @Composable (T) -> Unit
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val height = maxHeight
    val chartBoxModifier = if (height > 600.dp) {
      Modifier.height(height - 224.dp)
    } else {
      Modifier.height(300.dp)
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = chartBoxModifier
          .padding(16.dp)
          .fillMaxWidth()
      ) {
        val accountsProportion = remember(items) { items.extractProportionsKmp { debts(it) } }
        val circleColors = remember(items) { items.map { colors(it) } }

        KmpAnimatedCircle(
          proportions = accountsProportion,
          colors = circleColors,
          modifier = Modifier
            .align(Alignment.Center)
            .fillMaxSize(0.75f) // Ограничиваем масштаб, чтобы график не прилипал к краям Mac-окна
        )
        Column(
          modifier = Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = circleLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            // Вызовы ресурсов строк переведены под управление JetBrains Res.string для кроссплатформы
            text = "${formatDebtKmp(total)} ${stringResource(Res.string.uah)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
          )

          if (onPayClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onPayClick,
              enabled = isPayEnabled,
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7CB342),
                contentColor = Color.White,
                disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
              ),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
              modifier = Modifier.height(48.dp)
            ) {
              Icon(
                painter = painterResource(Res.drawable.privatbank),
                contentDescription = null,
                modifier = Modifier.size(40.dp).padding(end = 8.dp),
                tint = Color.Unspecified
              )
              Text(
                text = stringResource(Res.string.pay),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          // Внутри Card -> Column файла ServiceListStateless.kt обновите цикл:
          items.forEachIndexed { index, item ->
            rows(item)
            if (index < items.lastIndex) {
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 12.dp)
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * [formatDebtKmp] — Кроссплатформенное форматирование вывода копеек общего долга ГИОЦ.
 * ИСПРАВЛЕНО: Корректная обработка отрицательных чисел (переплат).
 */
private fun formatDebtKmp(debt: Double): String {
  val isNegative = debt < 0
  val absDebt = kotlin.math.abs(debt)
  val rounded = (absDebt * 100.0 + 0.5).toLong() // Добавляем 0.5 для правильного округления Double
  val mainPart = rounded / 100
  val kopecks = rounded % 100
  val kopecksStr = if (kopecks < 10) "0$kopecks" else "$kopecks"
  val sign = if (isNegative) "-" else ""
  return "$sign$mainPart.$kopecksStr"
}

/**
 * [extractProportionsKmp] — КМР-расширение для безопасного вычисления пропорций секторов диаграммы оплат.
 */
private fun <T> List<T>.extractProportionsKmp(selector: (T) -> Double): List<Float> {
  val total = this.fold(0.0) { acc, item -> acc + selector(item) }
  if (total <= 0.0) return this.map { 1f / this.size }
  return this.map { (selector(it) / total).toFloat() }
}
