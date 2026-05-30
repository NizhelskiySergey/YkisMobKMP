package com.ykis.ykismobkmp.ui.screens.ledger

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.ledger.payment.ServiceDetailContentWrapper
import com.ykis.ykismobkmp.ui.screens.ledger.payment.choice.PaymentChoiceStateful
import com.ykis.ykismobkmp.ui.screens.ledger.payment.list.PaymentListStateful
import ykismobkmp.composeapp.generated.resources.*

private const val className = "ServiceDetailScreen"

@Composable
fun ServiceDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  totalDebtState: TotalDebtState,
  navigateToWebView: (String) -> Unit
) {
  // Нативная КМР инжекция финансовой ScreenModel фреймворка Voyager
  val ledgerScreenModel = koinInject<LedgerScreenModel>()

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
        ledgerScreenModel.closeContentDetail()
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
