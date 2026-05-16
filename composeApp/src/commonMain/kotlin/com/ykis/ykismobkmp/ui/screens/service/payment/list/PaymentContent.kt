package com.ykis.ykismobkmp.ui.screens.service.payment.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.components.GroupFilterChip
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.service.ServiceScreenModel

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private const val className = "PaymentListStateful"

/**
 * [PaymentListStateful] — Кроссплатформенный Stateful-компонент архива оплат ГИОЦ г. Южный.
 * Инжектирует ServiceScreenModel и запускает реактивный каскадный сбор истории платежей по годам.
 */
@Composable
fun PaymentListStateful(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState
) {
  // ИСПРАВЛЕНО: Внедряем очищенную КМР-модель экрана через фабрику koinInject()
  val screenModel = koinInject<ServiceScreenModel>()

  // ИСПРАВЛЕНО: collectAsStateWithLifecycle заменен на кроссплатформенный collectAsState()
  val paymentState by screenModel.paymentState.collectAsState()

  // ИСПРАВЛЕНО: Платформозависимый SimpleDateFormat заменен на нативный КМР-пакет kotlinx-datetime
  val currentYearString = remember {
    val currentMoment = kotlin.time.Clock.System.now()
    val localDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    localDateTime.year.toString()
  }

  var selectedChip by rememberSaveable { mutableStateOf(currentYearString) }

  // Каскадный КМР-триггер перезагрузки финансовой истории при смене года или квартиры БТИ
  LaunchedEffect(key1 = selectedChip, key2 = baseUIState.addressId) {
    if (baseUIState.addressId != 0L) {
      // ИСПРАВЛЕНО: Передаем сквозной Long ID адреса напрямую в КМР-метод без кастингов
      screenModel.getPaymentList(
        addressId = baseUIState.addressId,
        year = selectedChip,
        uid = baseUIState.uid.toString()
      )
    }
  }

  PaymentContentStateless(
    modifier = modifier,
    isLoading = paymentState.isLoading,
    currentYear = currentYearString,
    paymentList = paymentState.paymentList,
    selectedChip = selectedChip,
    onSelectedChanged = { selectedChip = it },
    osbb = baseUIState.osbb
  )
}

/**
 * [PaymentContentStateless] — Адаптивная Stateless-верстка ленты истории оплат ЮКИС.
 */
@Composable
fun PaymentContentStateless(
  modifier: Modifier = Modifier,
  isLoading: Boolean,
  currentYear: String,
  paymentList: List<PaymentEntity>,
  selectedChip: String,
  onSelectedChanged: (String) -> Unit,
  osbb: String
) {
  // ИСПРАВЛЕНО: Динамический цикл заблокирован от утечек ОЗУ и холостых пересчетов хелпером remember
  val yearsList = remember(currentYear) {
    val baseYear = currentYear.toIntOrNull() ?: 2026
    List(20) { index -> (baseYear - index).toString() }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Линейка КМР-чипсов фильтрации архива платежей (20 лет истории ГИОЦ г. Южный)
    GroupFilterChip(
      list = yearsList,
      selectedChip = selectedChip,
      onSelectedChanged = onSelectedChanged
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Плавное кроссплатформенное переключение индикатора загрузки и контента квитанций
    Crossfade(
      targetState = isLoading,
      animationSpec = tween(durationMillis = 300, delayMillis = 100),
      label = "PaymentListCrossfade"
    ) { isCurrentlyLoading ->
      if (isCurrentlyLoading) {
        CenteredProgressIndicator()
      } else {
        PaymentList(
          paymentList = paymentList,
          osbb = osbb
        )
      }
    }
  }
}

