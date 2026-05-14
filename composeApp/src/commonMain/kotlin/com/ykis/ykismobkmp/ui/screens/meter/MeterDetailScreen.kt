package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.meter.heat.HeatMeterState
import com.ykis.ykismobkmp.ui.screens.meter.water.WaterMeterState
import org.jetbrains.compose.resources.painterResource
import ykismobkmp.composeapp.generated.resources.*
import com.ykis.ykismobkmp.ui.navigation.ContentDetail

private const val className = "MeterDetailScreen"

/**
 * [MeterDetailScreen] — Кроссплатформенный Stateless-экран детализации и съема показаний ЮКИС.
 * Полностью изолирован от моделей и готов к рендерингу на Mac Desktop, Android и iOS.
 */
@Composable
fun MeterDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState,
  baseUIState: BaseUIState,
  onSetContentDetail: (ContentDetail) -> Unit,
  onCloseDetail: () -> Unit
) {
  // ИСПРАВЛЕНО: Платформозависимый Log.d заменен на универсальный println() под Mac JVM
  LaunchedEffect(contentDetail) {
    println("[$className.Content]: Rendering detail for $contentDetail")
  }

  Column(modifier = modifier.fillMaxSize()) {
    // 1. НАСТРОЙКА КРОСС ПЛАТФОРМЕННОГО ТУЛБАРА
    DefaultAppBar(
      title = when (contentDetail) {
        ContentDetail.WATER_METER -> "Водомір: ${waterMeterState.selectedWaterMeter.model}"
        ContentDetail.HEAT_METER -> "Лічильник тепла: ${heatMeterState.selectedHeatMeter.model}"
        ContentDetail.WATER_READINGS -> "Історія водопостачання"
        ContentDetail.HEAT_READINGS -> "Історія опалення"
        // РЕШЕНИЕ: Ветка else закрывает требования компилятора к исчерпываемости (exhaustiveness)
        else -> "Прилади обліку ЮКІС"
      },
      onBackClick = {
        println("[$className.onBackClick]: Navigating back from $contentDetail")
        // ИСПРАВЛЕНО: Логика навигации переведена на чистые лямбда-коллбэки вместо вызова viewModel
        when (contentDetail) {
          ContentDetail.WATER_READINGS -> onSetContentDetail(ContentDetail.WATER_METER)
          ContentDetail.HEAT_READINGS -> onSetContentDetail(ContentDetail.HEAT_METER)
          else -> onCloseDetail()
        }
      },
      actionButton = {
        // Иконка истории (отображается только если мы находимся на главном экране прибора учета)
        if (contentDetail == ContentDetail.HEAT_METER || contentDetail == ContentDetail.WATER_METER) {
          IconButton(
            onClick = {
              val nextDetail = if (contentDetail == ContentDetail.WATER_METER)
                ContentDetail.WATER_READINGS else ContentDetail.HEAT_READINGS

              println("[$className.Action]: Open history -> $nextDetail")
              onSetContentDetail(nextDetail)
            },
          ) {
            Icon(
              painter = painterResource(Res.drawable.ic_history),
              contentDescription = "Історія показань",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    )

    // 2. ОСНОВНОЙ КОНТЕНТ (Форма ввода или Лента Истории)
    // Убедись, что твой MeterDetailContent также отрефакторен под Stateless (не принимает саму viewModel)
    MeterDetailContent(
      baseUIState = baseUIState,
      contentDetail = contentDetail,
      waterMeterState = waterMeterState,
      heatMeterState = heatMeterState
    )
  }
}

/**
 * [MeterDetailContent] — Временная заглушка контента для успешной компиляции файла.
 * Сюда будут встраиваться формы ввода показаний жителей Южного.
 */
@Composable
fun MeterDetailContent(
  baseUIState: BaseUIState,
  contentDetail: ContentDetail,
  waterMeterState: WaterMeterState,
  heatMeterState: HeatMeterState
) {
  Box(Modifier.fillMaxSize()) {
    // Логика отрисовки форм ввода или списков показаний на основе контента
  }
}
