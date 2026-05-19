package com.ykis.ykismobkmp.ui.screens.service

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.service.payment.ServiceDetailContentWrapper
import com.ykis.ykismobkmp.ui.screens.service.payment.choice.PaymentChoiceStateful
import com.ykis.ykismobkmp.ui.screens.service.payment.list.PaymentListStateful
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ServiceDetailScreen"

/**
 * [ServiceDetailScreen] — Кроссплатформенный Stateless-экран детализации начислений, оплат и тарифов ГИОЦ г. Южный.
 */
@Composable
fun ServiceDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  totalDebtState: TotalDebtState,
  navigateToWebView: (String) -> Unit
) {
  // Нативная КМР инжекция финансовой ScreenModel фреймворка Voyager
  val serviceScreenModel = koinInject<ServiceScreenModel>()

  // Реактивно вычитываем глобальный адаптивный тип навигации из CompositionLocal
  val adaptiveNavigationType = LocalNavigationType.current

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    // Вызовы ресурсов строк переведены под управление JetBrains Res.string для Mac/iOS
    DefaultAppBar(
      navigationType = adaptiveNavigationType,
      canNavigateBack = true,
      onBackClick = {
        // Трассировка рантайма по нашему железному правилу [Класс.Метод] через КМР-команду println()
        println("[$className.onBackClick]: Скидання контексту деталізації та повернення до сводного балансу")
        serviceScreenModel.closeContentDetail()
      },
      title = when (contentDetail) {
        ContentDetail.OSBB -> baseUIState.osbb.takeIf { it.isNotEmpty() } ?: "Мій ОСББ"
        ContentDetail.WATER_SERVICE -> stringResource(Res.string.vodokanal)
        ContentDetail.WARM_SERVICE -> stringResource(Res.string.ytke)
        ContentDetail.GARBAGE_SERVICE -> stringResource(Res.string.yzhtrans)
        else -> stringResource(Res.string.payment_list)
      },
      subtitle = baseUIState.address
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
        // ИСПРАВЛЕНО НАМЕРТВО: Вызываем нашу реальную, отлаженную панель чип-фильтров лет!
        Box(modifier = Modifier.weight(1f)) {
          ServiceDetailContentWrapper(
            contentDetail = contentDetail,
            baseUIState = baseUIState
          )
        }
      }
    }
  }
}
