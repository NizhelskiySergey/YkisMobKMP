package com.ykis.ykismobkmp.ui.screens.service

// Импорты общих компонентов, стейтов и моделей ЮКИС

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.service.list.TotalDebtState
import com.ykis.ykismobkmp.ui.screens.service.payment.choice.PaymentChoiceStateful
import com.ykis.ykismobkmp.ui.screens.service.payment.list.PaymentListStateful
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ServiceDetailScreen"

/**
 * [ServiceDetailScreen] — Кроссплатформенный Stateless-экран детализации начислений, оплат и тарифов ГИОЦ г. Южный.
 * Полностью очищен от Android SDK и готов к нативной сборке под Mac Desktop и iOS.
 */
@Composable
fun ServiceDetailScreen(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  totalDebtState: TotalDebtState,
  contentDetail: ContentDetail,
  navigateToWebView: (String) -> Unit
) {
  // Инжектируем очищенную КМР финансовую модель экрана через Koin
  val screenModel = koinInject<ServiceScreenModel>()

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    // ИСПРАВЛЕНО: Заменен Android R.string на КМР Res.string для бесшовной локализации служб г. Южное
    DefaultAppBar(
      title = when (contentDetail) {
        ContentDetail.OSBB -> baseUIState.osbb.takeIf { it.isNotEmpty() } ?: "Мій ОСББ"
        ContentDetail.WATER_SERVICE -> stringResource(Res.string.vodokanal)
        ContentDetail.WARM_SERVICE -> stringResource(Res.string.ytke)
        ContentDetail.GARBAGE_SERVICE -> stringResource(Res.string.yzhtrans)
        else -> stringResource(Res.string.payment_list)
      },
      subtitle = baseUIState.address,
      canNavigateBack = true,
      onBackClick = {
        println("[$className]: Клик назад, закрытие панели детализации ЖКХ")
        screenModel.closeContentDetail()
      }
    )

    // Каскадный КМР-выбор отображаемого финансового контента
    when (contentDetail) {
      ContentDetail.PAYMENT_LIST -> {
        PaymentListStateful(
          modifier = Modifier.weight(1f),
          baseUIState = baseUIState
        )
      }
      ContentDetail.PAYMENT_CHOICE -> {
        PaymentChoiceStateful(
          modifier = Modifier.weight(1f),
          baseUIState = baseUIState,
          totalDebtState = totalDebtState,
          navigateToWebView = navigateToWebView
        )
      }
      else -> {
        ServiceDetailContent(
          modifier = Modifier.weight(1f),
          contentDetail = contentDetail,
          baseUIState = baseUIState
        )
      }
    }
  }
}

// Временная заглушка детальной инфо-панели тарифов БТИ (пришли её оригинальный файл для рефакторинга)
@Composable
fun ServiceDetailContent(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState
) {
  Box(modifier = modifier.fillMaxSize()) {
    Text("Деталізація тарифів та нарахувань служби: ${contentDetail.name}")
  }
}

