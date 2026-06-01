package com.ykis.ykismobkmp.ui.screens.ledger

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.navigation.LocalNavigationType
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.accrued_text
import ykismobkmp.composeapp.generated.resources.end_debt
import ykismobkmp.composeapp.generated.resources.no_payment
import ykismobkmp.composeapp.generated.resources.no_payment_year
import ykismobkmp.composeapp.generated.resources.paid
import ykismobkmp.composeapp.generated.resources.payment_list
import ykismobkmp.composeapp.generated.resources.services
import ykismobkmp.composeapp.generated.resources.start_debt
import ykismobkmp.composeapp.generated.resources.summary
import ykismobkmp.composeapp.generated.resources.vodokanal
import ykismobkmp.composeapp.generated.resources.ytke
import ykismobkmp.composeapp.generated.resources.yzhtrans
import kotlin.time.Clock
private const val className = "ServiceDetailContent"

@Composable
fun ServiceDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  totalDebtState: TotalDebtState,
  screenModel: LedgerScreenModel, // Принимаем сквозную родительскую модель Хаба ЮКІС
  navigateToWebView: (String) -> Unit
) {
  val adaptiveNavigationType = LocalNavigationType.current

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    // Нативный тулбар DefaultAppBar расчетного центра ЮКІС
    DefaultAppBar(
      navigationType = adaptiveNavigationType,
      canNavigateBack = true,
      onBackClick = {
        // Исправлено: Команда сброса уходит в единую общую ScreenModel
        println("[YkisLogKMP.$className.onBackClick.Mobile]: Натиснуто стрілку назад. Повернення на список служб.")
        screenModel.closeContentDetail()
        screenModel.setContentDetail(ContentDetail.UNKNOWN)
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

    Box(modifier = Modifier.weight(1f)) {
      // Исправлено: Вызываем переименованный контейнер, убирая конфликт перегрузок КМР
      ServiceDetailContentContainer(
        modifier = Modifier.fillMaxSize(),
        contentDetail = contentDetail,
        baseUIState = baseUIState,
        screenModel = screenModel
      )
    }
  }
}

/**
 * [ServiceDetailContentContainer] — Изолированный контейнер сбора истории начислений ГИОЦ.
 * Исправлено: Функция переименована для устранения ошибки "never used".
 */
@Composable
fun ServiceDetailContentContainer(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  screenModel: LedgerScreenModel
) {
  val serviceDetail by screenModel.detailState.collectAsState()
  val currentYearString = remember {
    val currentMoment = Clock.System.now()
    val localDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    localDateTime.year.toString()
  }

  var selectedChip by rememberSaveable { mutableStateOf(currentYearString) }

  LaunchedEffect(serviceDetail.services.size, selectedChip) {
    println("[YkisLogKMP.ServiceDetailContentContainer]: [STATE_CHANGE] Перерисовано. Элементов: ${serviceDetail.services.size}")
  }

  // Каскадный КМР-триггер перезагрузки таблиц при смене года или квартиры
  LaunchedEffect(key1 = selectedChip, key2 = contentDetail, key3 = baseUIState.addressId) {
    if (baseUIState.addressId != 0L) {
      baseUIState.uid?.let { currentUid ->
        screenModel.getDetailService(
          uid = currentUid,
          addressId = baseUIState.addressId,
          houseId = baseUIState.houseId,
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

@Composable
fun ListServiceDetails(listServiceEntity: List<ServiceEntity>) {

  // Трассировка входящего списка по нашему железному правилу [Класс.Метод]
  LaunchedEffect(listServiceEntity) {
    println("[YkisLogKMP.ServiceDetailScreen.ListServiceDetails]: [UI_RECEIVE] Входящая коллекция месяцев на рендер. Размер: ${listServiceEntity.size} шт.")

    listServiceEntity.forEachIndexed { index, item ->
      println("[YkisLogKMP.ServiceDetailScreen.ListServiceDetails]: Идентификация строки [$index] -> Л/С: ${item.addressId}, Служба: ${item.service}, Дата: ${item.data}, Начислено: ${item.nachisleno}, Долг: ${item.dolg}")
    }
  }

  if (listServiceEntity.isEmpty()) {
    // Если в логах выведется Размер: 0 шт., значит, Use Case выдает пустой стейт из СУБД.
    // Если в логах выведется Размер: 6 шт., но экран пустой, значит, LazyColumn спотыкается о дубликаты ключей key!
    EmptyListState(
      title = stringResource(Res.string.no_payment),
      subtitle = stringResource(Res.string.no_payment_year)
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(vertical = 8.dp)
    ) {
      itemsIndexed(
        items = listServiceEntity,
        // Безопасный, отказоустойчивый ключ на базе индекса строки для предотвращения коллизий
        key = { index, item -> "item_${index}_${item.data ?: "0"}" }
      ) { index, item ->
        ServiceDetailItem(serviceEntity = item)
      }
    }
  }
}


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
@Composable
fun ServiceDetailItem(
  modifier: Modifier = Modifier,
  serviceEntity: ServiceEntity = ServiceEntity()
) {
  val scrollState = rememberScrollState()

  // ИСПРАВЛЕНО НАМЕРТВО: Явно передаем поле .data из сетевой модели ServiceEntity!
  val formattedMonthHeader = remember(serviceEntity.data) { formatUkMonth(serviceEntity.data) }

  // Хелпер очистки текстовых полей от серверных none/null значений
  val cleanStr: (Any?) -> String = { valStr ->
    val s = valStr?.toString() ?: ""
    if (s.equals("none", ignoreCase = true) || s.equals("null", ignoreCase = true)) "" else s
  }

  // Хелпер очистки числовых полей биллинга ГИОЦ
  val cleanNum: (Double?) -> String = { num ->
    if (num == null || num == 0.0) "0.00" else num.toString()
  }

  BaseCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 12.dp)
  ) {
    // Красивый жирный заголовок месяца под брендинг ЮКІС
    Text(
      text = formattedMonthHeader,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Black,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp)
    )

    Box(modifier = Modifier.fillMaxWidth()) {
      // Задний слой: Горизонтальные разделительные линии таблицы
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(29.dp)
      ) {
        TableDivider(modifier = Modifier.padding(top = 42.dp))
        TableDivider(cleanNum(serviceEntity.zadol1))
        TableDivider(cleanNum(serviceEntity.zadol2))
        TableDivider(cleanNum(serviceEntity.zadol3))
        TableDivider(cleanNum(serviceEntity.zadol4))
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
        // Колонка №1: Перечень наименований тарифов (Взносы, Ремонтный фонд)
        ColumnItemInTable(
          alignment = Alignment.Start,
          value1 = cleanStr(serviceEntity.service1),
          value2 = cleanStr(serviceEntity.service2),
          value3 = cleanStr(serviceEntity.service3),
          value4 = cleanStr(serviceEntity.service4),
          header = stringResource(Res.string.services),
          summary = stringResource(Res.string.summary),
          headerAlign = TextAlign.Start
        )

        // Колонка №2: Входящий долг на начало месяца
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = cleanNum(serviceEntity.zadol1),
          value2 = cleanNum(serviceEntity.zadol2),
          value3 = cleanNum(serviceEntity.zadol3),
          value4 = cleanNum(serviceEntity.zadol4),
          header = stringResource(Res.string.start_debt),
          summary = cleanNum(serviceEntity.zadol),
          headerAlign = TextAlign.End
        )

        // Колонка №3: Начислено по тарифу за текущий период
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = cleanNum(serviceEntity.nachisleno1),
          value2 = cleanNum(serviceEntity.nachisleno2),
          value3 = cleanNum(serviceEntity.nachisleno3),
          value4 = cleanNum(serviceEntity.nachisleno4),
          header = stringResource(Res.string.accrued_text),
          summary = cleanNum(serviceEntity.nachisleno),
          headerAlign = TextAlign.End
        )

        // Колонка №4: Фактически оплачено жильцом
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = cleanNum(serviceEntity.oplacheno1),
          value2 = cleanNum(serviceEntity.oplacheno2),
          value3 = cleanNum(serviceEntity.oplacheno3),
          value4 = cleanNum(serviceEntity.oplacheno4),
          header = stringResource(Res.string.paid),
          summary = cleanNum(serviceEntity.oplacheno),
          headerAlign = TextAlign.End
        )

        // Колонка №5: Итоговая задолженность на конец месяца
        ColumnItemInTable(
          alignment = Alignment.End,
          value1 = cleanNum(serviceEntity.dolg1),
          value2 = cleanNum(serviceEntity.dolg2),
          value3 = cleanNum(serviceEntity.dolg3),
          value4 = cleanNum(serviceEntity.dolg4),
          header = stringResource(Res.string.end_debt),
          summary = cleanNum(serviceEntity.dolg),
          headerAlign = TextAlign.End
        )
      }
    }
  }
}





