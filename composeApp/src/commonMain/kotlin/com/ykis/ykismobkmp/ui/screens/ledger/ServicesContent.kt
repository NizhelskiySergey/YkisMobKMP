package com.ykis.ykismobkmp.ui.screens.ledger

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.components.EmptyListState
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator

import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*
import kotlin.time.Clock

private const val tag = "ServiceDetailContent"
private const val className = "ServiceDetailContent"

@Composable
fun ServiceDetailContent(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState
) {
  val screenModel = koinInject<LedgerScreenModel>()
  val serviceDetail by screenModel.detailState.collectAsState()
  val currentYearString = remember {
    val currentMoment = Clock.System.now()
    val localDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    localDateTime.year.toString()
  }

  var selectedChip by rememberSaveable { mutableStateOf(currentYearString) }

  // ЛОГ №1: Трассировка рекомпозиции внешнего контейнера
  LaunchedEffect(serviceDetail.services.size, selectedChip) {
    println("[YkisLogKMP.$className]: [STATE_CHANGE] Перерисовка холста деталей. Скачано месяцев в ОЗУ: ${serviceDetail.services.size} шт. Выбран год: $selectedChip")
  }

  // Каскадный КМР-триггер перезагрузки таблиц при смене года, квартиры или выбранной службы
  LaunchedEffect(key1 = selectedChip, key2 = contentDetail, key3 = baseUIState.addressId) {
    if (baseUIState.addressId != 0L) {
      baseUIState.uid?.let { currentUid ->
        println("[YkisLogKMP.$className]: [LAUNCH_EFFECT_TRIGGER] Запуск getDetailService для о/р: ${baseUIState.addressId}, Служба: $contentDetail")
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

  // ЛОГ №2: Трассировка бесшовного переключения лоадеров
  LaunchedEffect(isLoading) {
    println("[YkisLogKMP.$className]: [STATELESS_UI] isLoading стейт изменился на: $isLoading")
  }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
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
  // ЛОГ №3: Глубокий аудит элементов LazyColumn прямо в рантайме смартфона
  LaunchedEffect(listServiceEntity.size) {
    println("[YkisLogKMP.$className.ListServiceDetails]: [UI_RENDER] Финальный список влетел на LazyColumn! Количество: ${listServiceEntity.size} шт.")
    listServiceEntity.forEachIndexed { idx, item ->
      println("[YkisLogKMP.$className.ListServiceDetails]: Строка списка [$idx] -> Дата периода: ${item.data}, Код службы: ${item.service}")
    }
  }

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
        // ИСПРАВЛЕНО НАМЕРТВО: Ключ переведен на уникальную дату отчетного периода (it.data)!
        // Дубликаты ключей key = 1326 полностью ликвидированы, Skiko мгновенно отрисует все строки!
        key = { it.data }
      ) { detailItem ->
        ServiceDetailItem(serviceEntity = detailItem)
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
    val monthIdx = rawPeriod.toLongOrNull()?.mod(12L)?.toInt() ?: 4
    val currentKmpYear = "2026"
    "${monthsUk[monthIdx]} $currentKmpYear"
  }
}


