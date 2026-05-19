package com.ykis.ykismobkmp.ui.screens.service.list
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.uah

private const val className = "ServiceListStateless"

/**
 * [KmpAnimatedCircle] — Полностью кроссплатформенный, аппаратно ускоренный холст круговой диаграммы ЖЭК/ОСМД.
 * РЕШЕНИЕ ОШИБКИ: Реализован нативно через Canvas drawArc, стабилен на Mac Desktop (JVM), Android и iOS.
 */
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

/**
 * [ServiceListStateless] — Кроссплатформенный генерик-компонент круговой диаграммы баланса коммунальных начислений ЮКИС.
 * ИСПРАВЛЕНО: Несовместимый AnimatedCircle заменен на стабильный KmpAnimatedCircle, убран Unresolved reference.
 */
@Composable
fun <T> ServiceListStateless(
  modifier: Modifier = Modifier,
  items: List<T>,
  colors: (T) -> Color,
  debts: (T) -> Double,
  total: Double,
  circleLabel: String,
  rows: @Composable (T) -> Unit
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val height = maxHeight

    // Создаем чистый, локальный изолированный Modifier для Box графика
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
      // Контейнер круговой интерактивной диаграммы ЮКИС г. Южного
      Box(
        modifier = chartBoxModifier
          .padding(16.dp)
          .fillMaxWidth()
      ) {
        // Извлекаем пропорциональные доли долгов для секторов графика через КМР-хелпер
        val accountsProportion = remember(items) { items.extractProportionsKmp { debts(it) } }
        val circleColors = remember(items) { items.map { colors(it) } }

        // ИСПРАВЛЕНО: Заменен ложный компонент на наш легитимный KmpAnimatedCircle
        KmpAnimatedCircle(
          proportions = accountsProportion,
          colors = circleColors,
          modifier = Modifier
            .align(Alignment.Center)
            .fillMaxSize(0.75f) // Ограничиваем масштаб, чтобы график не прилипал к краям Mac-окна
        )

        // Текстовый блок суммы задолженности по центру круга
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
            // ИСПРАВЛЕНО: Заменен Android R.string.uah на КМР Res.string.uah, форматирование копеек выровнено
            text = "${formatDebtKmp(total)} ${stringResource(Res.string.uah)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Карточка-контейнер списка ЖКХ-предприятий города Южный
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          items.forEach { item ->
            // rows(item) больше не получает неявных ссылок на деструктивный внешний modifier
            rows(item)
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * [formatDebtKmp] — Кроссплатформенное форматирование вывода копеек общего долга ГИОЦ.
 */
private fun formatDebtKmp(debt: Double): String {
  val rounded = (debt * 100.0).toLong()
  val mainPart = rounded / 100
  val kopecks = rounded % 100
  val kopecksStr = if (kopecks < 10) "0$kopecks" else "$kopecks"
  return "$mainPart.$kopecksStr"
}

/**
 * [extractProportionsKmp] — КМР-расширение для безопасного вычисления пропорций секторов диаграммы оплат.
 */
private fun <T> List<T>.extractProportionsKmp(selector: (T) -> Double): List<Float> {
  val total = this.sumOf { selector(it) }
  if (total <= 0.0) return this.map { 1f / this.size }
  return this.map { (selector(it) / total).toFloat() }
}
