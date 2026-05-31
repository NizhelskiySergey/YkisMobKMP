package com.ykis.ykismobkmp.ui.screens.ledger.payment

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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.detail.ColumnItemInTable
import com.ykis.ykismobkmp.ui.screens.ledger.detail.TableDivider
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.accrued_text
import ykismobkmp.composeapp.generated.resources.end_debt
import ykismobkmp.composeapp.generated.resources.no_payment
import ykismobkmp.composeapp.generated.resources.no_payment_year
import ykismobkmp.composeapp.generated.resources.paid
import ykismobkmp.composeapp.generated.resources.services
import ykismobkmp.composeapp.generated.resources.start_debt
import ykismobkmp.composeapp.generated.resources.summary
import kotlin.time.Clock

private const val className = "ServiceDetailContent"

/**
 * КМР-хелпер форматирования месяцев биллинга на украинском языке (замена SimpleDateFormat).
 */
private fun formatUkMonth(dateString: String?): String {
  if (dateString.isNullOrBlank() || !dateString.contains("-")) return "Звітний місяць"
  val parts = dateString.split("-")
  if (parts.size < 2) return "Звітний місяць"
  val yearStr = parts[0]
  val monthInt = parts[1].toIntOrNull() ?: return "Звітний місяць"
  val monthName = when (monthInt) {
    1 -> "Січень" 2 -> "Лютий" 3 -> "Березень" 4 -> "Квітень"
    5 -> "Травень" 6 -> "Червень" 7 -> "Липень" 8 -> "Серпень"
    9 -> "Вересень" 10 -> "Жовтень" 11 -> "Листопад" 12 -> "Грудень"
    else -> "Місяць"
  }
  return "$monthName $yearStr"
}

/**
 * [GroupFilterChip] — Локальный компонент горизонтальной ленты чип-фильтров выбора лет.
 */
@Composable
fun GroupFilterChip(
  list: List<String>,
  selectedChip: String,
  onSelectedChanged: (String) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    list.forEach { year ->
      FilterChip(
        selected = selectedChip == year,
        onClick = { onSelectedChanged(year) },
        label = { Text(year) }
      )
    }
  }
}

@Composable fun CenteredProgressIndicator() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 3.dp) } }
@Composable fun EmptyListState(title: String, subtitle: String) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) } }

/**
 * [ServiceDetailContentWrapper] — Кроссплатформенный Stateful-контейнер детализации и инвойсов начислений ЮКИС.
 */
@Composable
fun ServiceDetailContentWrapper(
  contentDetail: ContentDetail,
  baseUIState: BaseUIState
) {
  val viewModel = koinInject<LedgerScreenModel>()

  // Извлекаем текущий календарный год кроссплатформенным способом kotlinx-datetime
  val currentMoment = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
  val currentYearString = remember { currentMoment.year.toString() }

  val serviceDetailState by viewModel.totalDebtState.collectAsState()
  var selectedChip by rememberSaveable { mutableStateOf(currentYearString) }

  // Атомарный триггер запроса ведомостей ГИОЦ при переключении фильтров, служб или лицевых счетов
  LaunchedEffect(selectedChip, contentDetail, baseUIState.addressId) {
    baseUIState.uid?.let { currentUid ->
      println("[$className.LaunchedEffect]: [FETCH_DETAIL] Запит відомості для о/р Long: ${baseUIState.addressId} за рік: $selectedChip")

      viewModel.getDetailService(
          uid = currentUid,
          addressId = baseUIState.addressId, // Сквозной Long ID СУБД
        houseId = baseUIState.houseId,
          total = 0L.toByte(),
          year = selectedChip,
          service = when (contentDetail) {
            ContentDetail.OSBB -> 4L.toByte()
            ContentDetail.WATER_SERVICE -> 1L.toByte()
            ContentDetail.WARM_SERVICE -> 2L.toByte()
            ContentDetail.GARBAGE_SERVICE -> 3L.toByte()
            else -> 4L.toByte()
          }
        )

    }
  }

  // Безопасно кастим тип Any к ожидаемой коллекции List<ServiceEntity>
  val safeServiceList = remember(serviceDetailState.serviceDetail) {
    (serviceDetailState.serviceDetail as? List<ServiceEntity>) ?: emptyList<ServiceEntity>()
  }

  ServiceDetailContent(
    isLoading = baseUIState.isLoading,
    year = currentYearString,
    serviceEntities = safeServiceList,
    onSelectedChanged = { nextYear ->
      println("[$className.onSelectedChanged]: Перемикання відомості на рік: $nextYear")
      selectedChip = nextYear
    },
    selectedChip = selectedChip
  )
}

/**
 * [ServiceDetailContent] — Компоновщик структуры холста финансовой детализации и чип-фильтров лет.
 */
@Composable
fun ServiceDetailContent(
  isLoading: Boolean,
  year: String,
  serviceEntities: List<ServiceEntity>,
  selectedChip: String,
  onSelectedChanged: (String) -> Unit
) {
  val years = remember(year) {
    mutableListOf<String>().apply {
      val baseYear = year.toIntOrNull() ?: 2026
      for (i in 0 until 20) {
        add((baseYear - i).toString())
      }
    }
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    GroupFilterChip(
      list = years,
      selectedChip = selectedChip,
      onSelectedChanged = onSelectedChanged
    )

    Crossfade(
      targetState = isLoading,
      animationSpec = tween(600),
      label = "ServiceDetailLoadingFade"
    ) { loadingState ->
      if (loadingState) {
        CenteredProgressIndicator()
      } else {
        ListServiceDetails(listServiceEntity = serviceEntities)
      }
    }
  }
}

/**
 * [ListServiceDetails] — Списочная Lazy-лента квитанций Xpay коммунального биллинга г. Южного.
 */
@Composable
fun ListServiceDetails(listServiceEntity: List<ServiceEntity>) {
  if (listServiceEntity.isEmpty()) {
    EmptyListState(
      title = stringResource(Res.string.no_payment),
      subtitle = stringResource(Res.string.no_payment_year)
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(vertical = 8.dp)
    ) {
      items(
        items = listServiceEntity,
        key = { it.data ?: "" }
      ) { item ->
        ServiceDetailItem(serviceEntity = item)
      }
    }
  }
}

/**
 * [ServiceDetailItem] — Визуальная карточка детализации начислений по одной квартире за отчетный месяц.
 */
@Composable
fun ServiceDetailItem(
  modifier: Modifier = Modifier,
  serviceEntity: ServiceEntity = ServiceEntity()
) {
  val scrollState = rememberScrollState()
  val formattedMonthHeader = remember(serviceEntity.data) { formatUkMonth(serviceEntity.data) }

  // ИСПРАВЛЕНО НАМЕРТВО: Ложные columnModifier и labelModifier удалены в соответствии с BaseCard КМР-контрактом!
  // Все отступы и правила растягивания таблицы ГИОЦ передаются через единый стандартный modifier
  BaseCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp, horizontal = 12.dp),
    label = formattedMonthHeader
  ) {
    Box {
      Column(
        verticalArrangement = Arrangement.spacedBy(29.dp)
      ) {
        TableDivider(modifier = Modifier.padding(top = 42.dp))
        TableDivider(serviceEntity.zadol1.toString())
        TableDivider(serviceEntity.zadol2.toString())
        TableDivider(serviceEntity.zadol3.toString())
        TableDivider(serviceEntity.zadol4.toString())
      }

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
          value1 = serviceEntity.service1.toString(),
          value2 = serviceEntity.service2.toString(),
          value3 = serviceEntity.service3.toString(),
          value4 = serviceEntity.service4.toString(),
          header = stringResource(Res.string.services),
          summary = stringResource(Res.string.summary),
          headerAlign = TextAlign.Start
        )
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = serviceEntity.zadol1.toString(),
          value2 = serviceEntity.zadol2.toString(),
          value3 = serviceEntity.zadol3.toString(),
          value4 = serviceEntity.zadol4.toString(),
          header = stringResource(Res.string.start_debt),
          summary = serviceEntity.zadol.toString(),
          headerAlign = TextAlign.End
        )

        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = serviceEntity.nachisleno1.toString(),
          value2 = serviceEntity.nachisleno2.toString(),
          value3 = serviceEntity.nachisleno3.toString(),
          value4 = serviceEntity.nachisleno4.toString(),
          header = stringResource(Res.string.accrued_text),
          summary = serviceEntity.nachisleno.toString(),
          headerAlign = TextAlign.End
        )
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = serviceEntity.oplacheno1.toString(),
          value2 = serviceEntity.oplacheno2.toString(),
          value3 = serviceEntity.oplacheno3.toString(),
          value4 = serviceEntity.oplacheno4.toString(),
          header = stringResource(Res.string.paid),
          summary = serviceEntity.oplacheno.toString(),
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
