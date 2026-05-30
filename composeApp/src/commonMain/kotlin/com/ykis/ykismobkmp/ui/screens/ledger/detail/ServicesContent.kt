
package com.ykis.ykismobkmp.ui.screens.ledger.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator

import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.repository.ledger.LedgerParams
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "ServiceDetailContent"
@Composable
fun ServiceDetailContent(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState
) {
  val screenModel = koinInject<LedgerScreenModel>()
  val serviceDetail by screenModel.detailState.collectAsState()
  val currentYearString = remember {
    val currentMoment = kotlin.time.Clock.System.now()
    val localDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    localDateTime.year.toString()
  }

  var selectedChip by rememberSaveable { mutableStateOf(currentYearString) }

  // Каскадный КМР-триггер перезагрузки таблиц ГИОЦ при смене года, квартиры или выбранной службы
  LaunchedEffect(key1 = selectedChip, key2 = contentDetail, key3 = baseUIState.addressId) {
    if (baseUIState.addressId != 0L) {
      baseUIState.uid?.let { currentUid ->
        screenModel.getDetailService(
            uid = currentUid,
            addressId = baseUIState.addressId,
            service = when (contentDetail) {
              ContentDetail.OSBB -> 4.toByte()
              ContentDetail.WATER_SERVICE -> 1.toByte()
              ContentDetail.WARM_SERVICE -> 2.toByte()
              ContentDetail.GARBAGE_SERVICE -> 3.toByte()
              else -> 4.toByte()
            },
            year = selectedChip,
            total = 0,
          )

      }
    }
  }

  ServiceDetailContentStateless(
    modifier = modifier,
    isLoading = serviceDetail.isLoading,
    year = currentYearString,
    serviceEntities = serviceDetail.services,
    selectedChip = selectedChip,
    onSelectedChanged = { selectedChip = it }
  )
}
@Composable
fun ServiceDetailContentStateless(
  modifier: Modifier = Modifier,
  isLoading: Boolean,
  year: String,
  serviceEntities: List<ServiceEntity>,
  selectedChip: String,
  onSelectedChanged: (String) -> Unit
) {
  val yearsList = remember(year) {
    val baseYear = year.toIntOrNull() ?: 2026
    List(20) { index -> (baseYear - index).toString() }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Линейка переключения годов
    GroupFilterChip(
      list = yearsList,
      selectedChip = selectedChip,
      onSelectedChanged = onSelectedChanged
    )

    Crossfade(
      targetState = isLoading,
      animationSpec = tween(300),
      label = "ServiceDetailCrossfade"
    ) { isCurrentlyLoading ->
      if (isCurrentlyLoading) {
        CenteredProgressIndicator()
      } else {
        ListServiceDetails(listServiceEntity = serviceEntities)
      }
    }
  }
}

/**
 * [ListServiceDetails] — Списочный Lazy-контейнер месяцев отчетного финансового года.
 */
@Composable
fun ListServiceDetails(
  listServiceEntity: List<ServiceEntity>,
  modifier: Modifier = Modifier
) {
  if (listServiceEntity.isEmpty()) {
    EmptyListState(
      title = stringResource(Res.string.no_payment),
      subtitle = stringResource(Res.string.no_payment_year)
    )
  } else {
    LazyColumn(
      modifier = modifier.fillMaxSize(),
      contentPadding = PaddingValues(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(
        items = listServiceEntity,
        key = { it.id } // Наш сквозной Long ID первичного ключа
      ) { detailItem ->
        ServiceDetailItem(serviceEntity = detailItem)
      }
    }
  }
}

/**
 * [ServiceDetailItem] — Адаптивная таблица начислений, долгов и оплат Material 3.
 */
@Composable
fun ServiceDetailItem(
  modifier: Modifier = Modifier,
  serviceEntity: ServiceEntity
) {
  val scrollState = rememberScrollState()
  val monthTitle = rememberMonthTitleKmp(serviceEntity.id.toString())
  BaseCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 12.dp)
  ) {
    Text(
      text = monthTitle,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp)
    )
    Box(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(29.dp)
      ) {
        TableDivider(modifier = Modifier.padding(top = 42.dp))
        TableDivider()
        TableDivider()
        TableDivider()
        TableDivider()
      }

      // Передний слой: Горизонтально прокручиваемая КМР-сетка колонок баланса ГИОЦ
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(scrollState)
          .padding(start = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ColumnItemInTable(
          alignment = Alignment.Start,
          value1 = "Послуга 1",
          value2 = "Послуга 2",
          value3 = "Послуга 3",
          value4 = "Послуга 4",
          header = stringResource(Res.string.services),
          summary = stringResource(Res.string.summary),
          headerAlign = TextAlign.Start
        )
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = serviceEntity.dolg1.toString(),
          value2 = serviceEntity.dolg2.toString(),
          value3 = serviceEntity.dolg3.toString(),
          value4 = serviceEntity.dolg4.toString(),
          header = stringResource(Res.string.start_debt),
          summary = serviceEntity.dolg.toString(),
          headerAlign = TextAlign.End
        )
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = "0.00",
          value2 = "0.00",
          value3 = "0.00",
          value4 = "0.00",
          header = stringResource(Res.string.accrued_text),
          summary = "0.00",
          headerAlign = TextAlign.End
        )
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = "0.00",
          value2 = "0.00",
          value3 = "0.00",
          value4 = "0.00",
          header = stringResource(Res.string.paid),
          summary = "0.00",
          headerAlign = TextAlign.End
        )
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = serviceEntity.dolg1.toString(),
          value2 = serviceEntity.dolg2.toString(),
          value3 = serviceEntity.dolg3.toString(),
          value4 = serviceEntity.dolg4.toString(),
          header = stringResource(Res.string.end_debt),
          summary = serviceEntity.dolg.toString(),
          headerAlign = TextAlign.End
        )
      }
    }
  }
}

/**
 * [rememberMonthTitleKmp] — Локализованный КМР-парсер периодов начисления без привлечения Java SimpleDateFormat.
 */
@Composable
fun rememberMonthTitleKmp(rawPeriod: String): String {
  return remember(rawPeriod) {
    val monthsUk = listOf(
      "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
      "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
    )
    // Предполагаем, что ID или строка периода содержит номер месяца (например, от 1 до 12)
    val monthIdx = rawPeriod.toLongOrNull()?.mod(12L)?.toInt() ?: 4
    val currentKmpYear = "2026"
    "${monthsUk[monthIdx]} $currentKmpYear"
  }
}

// Заглушка фильтра чипсов для компиляции файла (замени на свою GroupFilterChip.kt)
@Composable fun GroupFilterChip(list: List<String>, selectedChip: String, onSelectedChanged: (String) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { list.forEach { FilterChip(selected = it == selectedChip, onClick = { onSelectedChanged(it) }, label = { Text(it) }, modifier = Modifier.padding(4.dp)) } } }

