package com.ykis.ykismobkmp.ui.screens.ledger

import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import org.jetbrains.compose.resources.painterResource
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
import com.ykis.ykismobkmp.ui.navigation.ContentType
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.*
import kotlin.time.Clock
private const val className = "ServiceDetailContent"

@Composable
fun FastPayPaymentRow(
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  ledgerUIState: BaseUIState
) {
  val uriHandler = LocalUriHandler.current
  
  // Визначаємо ЗАГАЛЬНИЙ борг по службі
  val totalServiceDebt = remember(contentDetail, ledgerUIState.totalDebt) {
    when (contentDetail) {
      ContentDetail.WATER_SERVICE   -> ledgerUIState.totalDebt.dolg1
      ContentDetail.WARM_SERVICE    -> ledgerUIState.totalDebt.dolg2
      ContentDetail.GARBAGE_SERVICE -> ledgerUIState.totalDebt.dolg3
      ContentDetail.OSBB            -> ledgerUIState.totalDebt.dolg4
      else -> 0.0
    }
  }

  // Стейт суми
  var paymentSum by remember(totalServiceDebt) { 
      mutableStateOf(if (totalServiceDebt > 0) totalServiceDebt.toString() else "") 
  }

  val targetOsbbId = when (contentDetail) {
      ContentDetail.WATER_SERVICE   -> Constants.WATER_SERVICE_ID
      ContentDetail.WARM_SERVICE    -> Constants.WARM_SERVICE_ID
      ContentDetail.GARBAGE_SERVICE -> Constants.GARBAGE_SERVICE_ID
      else -> baseUIState.osmdId
  }
  
  val fastpayToken = remember(targetOsbbId, ledgerUIState.fastpayTokens) {
      ledgerUIState.fastpayTokens.find { it.osbbId == targetOsbbId }?.token
  }

  // Показуємо тільки мешканцю
  if (baseUIState.userRole != com.ykis.ykismobkmp.domain.services.UserRole.StandardUser) return

  Column(modifier = Modifier.fillMaxWidth()) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 1.dp
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = paymentSum,
            onValueChange = { },
            modifier = Modifier.weight(1f),
            label = { Text("Сума до сплати", fontSize = 11.sp) },
            suffix = { Text("грн") },
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
          )

          Button(
            onClick = {
              if (!fastpayToken.isNullOrBlank()) {
                  val personalAccount = baseUIState.addressId.toString()
                  val jsonParams = "{\"token\":\"$fastpayToken\",\"personalAccount\":\"$personalAccount\"}"
                  val encodedParams = jsonParams.replace("{", "%7B").replace("}", "%7D").replace("\"", "%22")
                  val url = "https://next.privat24.ua/payments/form/$encodedParams"

                  println("[YkisLogKMP.FastPay]: >>> ВІДКРИВАЄМО ОПЛАТУ (ID: $personalAccount) >>>")
                  try { uriHandler.openUri(url) } catch (e: Exception) { }
              }
            },
            enabled = !fastpayToken.isNullOrBlank(),
            modifier = Modifier.height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (fastpayToken.isNullOrBlank()) Color.Gray else Color(0xFF7CB342))
          ) {
            Icon(
              painter = painterResource(Res.drawable.privatbank), 
              contentDescription = null, 
              modifier = Modifier.size(54.dp).padding(top=4.dp),
              tint = Color.Unspecified
            )
            Spacer(Modifier.width(8.dp))
            Text(if (fastpayToken.isNullOrBlank()) "Немає токена" else "Сплатити")
          }
        }
      }
      HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
  }
}


@Composable
fun ServiceDetailScreen(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  ledgerUIState: BaseUIState,
  screenModel: LedgerScreenModel,
  navigateToWebView: (String) -> Unit
) {
  Column(
    modifier = modifier.fillMaxSize()
  ) {
    DefaultAppBar(
      canNavigateBack = true,
      onBackClick = {
        println("[YkisLogKMP.$className.onBackClick]: Возврат к списку. Служба: $contentDetail")
        screenModel.closeContentDetail()
      },
      title = when (contentDetail) {
        ContentDetail.OSBB -> "ОСББ"
        ContentDetail.WATER_SERVICE -> stringResource(Res.string.vodokanal)
        ContentDetail.WARM_SERVICE -> stringResource(Res.string.ytke)
        ContentDetail.GARBAGE_SERVICE -> stringResource(Res.string.yzhtrans)
        else -> "Коммунальные услуги"
      },
      subtitle = baseUIState.address
    )

    // БЛОК ШВИДКОЇ ОПЛАТИ (FastPay)
    FastPayPaymentRow(
      contentDetail = contentDetail,
      baseUIState = baseUIState,
      ledgerUIState = ledgerUIState
    )

    Box(modifier = Modifier.weight(1f)) {
      ServiceDetailContentContainer(
        modifier = Modifier.fillMaxSize(),
        contentDetail = contentDetail,
        baseUIState = baseUIState,
        ledgerUIState = ledgerUIState,
        screenModel = screenModel
      )
    }
  }
}

@Composable
fun ServiceDetailContentContainer(
  modifier: Modifier = Modifier,
  contentDetail: ContentDetail,
  baseUIState: BaseUIState,
  ledgerUIState: BaseUIState,
  screenModel: LedgerScreenModel
) {
  val currentYearString = remember {
    val currentMoment = Clock.System.now()
    val localDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    localDateTime.year.toString()
  }

  var selectedChip by rememberSaveable { mutableStateOf(currentYearString) }

  // 1. Каскадний КМР-триггер перезавантаження таблиць (при зміні року або квартири)
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
          year = selectedChip
        )
      }
    }
  }

  ServiceDetailContentStateless(
    modifier = modifier,
    isLoading = ledgerUIState.isLoading,
    year = currentYearString,
    serviceEntities = ledgerUIState.monthlyServices,
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
        key = { index, item -> "item_${index}_${item.data}" }
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
  val contentType = LocalContentType.current
  val isDualPane = contentType == ContentType.DUAL_PANE

  val scrollState = rememberScrollState()
  val formattedMonthHeader = remember(serviceEntity.data) { formatUkMonth(serviceEntity.data) }

  // Хелперы очистки и сокращения данных
  val cleanStr: (Any?) -> String = { valStr ->
    val s = valStr?.toString() ?: ""
    val cleaned = if (s.equals("none", ignoreCase = true) || s.equals("null", ignoreCase = true)) "" else s
    // ИСПРАВЛЕНО: Сокращаем название до 9 символов для экстремальной компактности таблицы
    if (cleaned.length > 9) cleaned.take(8) + "…" else cleaned
  }
  val cleanNum: (Double?) -> String = { num ->
    if (num == null || num == 0.0) "0.00" else num.toString()
  }

  BaseCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 12.dp)
  ) {
    Text(
      text = formattedMonthHeader,
      style = MaterialTheme.typography.bodyLarge, // Уменьшили шрифт месяца
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 8.dp)
    )

    // КОМПАКТНЫЙ КОНТЕЙНЕР ТАБЛИЦЫ
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
      // 1. ЗАГОЛОВКИ ТАБЛИЦЫ (В центре, 11.sp, жирный)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TableCell(
          text = stringResource(Res.string.services), 
          weight = 1.3f, 
          isHeader = true, 
          textAlign = TextAlign.Center // Заголовок "Послуги" тепер по центру
        )
        TableCell(
          text = stringResource(if (isDualPane) Res.string.start_debt_full else Res.string.start_debt),
          isHeader = true,
          textAlign = TextAlign.End,
//          weight = if (isDualPane) 1.2f else 1f // Даем чуть больше места длинным заголовкам
        )
        TableCell(
          text = stringResource(if (isDualPane) Res.string.accrued_text_full else Res.string.accrued_text),
          isHeader = true,
          textAlign = TextAlign.End,
//          weight = if (isDualPane) 1.2f else 1f
        )
        TableCell(
          text = stringResource(if (isDualPane) Res.string.paid_full else Res.string.paid),
          isHeader = true,
          textAlign = TextAlign.End,
//          weight = if (isDualPane) 1.2f else 1f
        )
        TableCell(
          text = stringResource(if (isDualPane) Res.string.end_debt_full else Res.string.end_debt),
          isHeader = true,
          textAlign = TextAlign.End,
//          weight = if (isDualPane) 1.2f else 1f
        )
      }
      TableDivider()

      // 2. СТРОКИ ДАННЫХ (Цифры справа, 12.sp, не жирный)
      val servicesData = listOf(
        cleanStr(serviceEntity.service1) to listOf(cleanNum(serviceEntity.zadol1), cleanNum(serviceEntity.nachisleno1), cleanNum(serviceEntity.oplacheno1), cleanNum(serviceEntity.dolg1)),
        cleanStr(serviceEntity.service2) to listOf(cleanNum(serviceEntity.zadol2), cleanNum(serviceEntity.nachisleno2), cleanNum(serviceEntity.oplacheno2), cleanNum(serviceEntity.dolg2)),
        cleanStr(serviceEntity.service3) to listOf(cleanNum(serviceEntity.zadol3), cleanNum(serviceEntity.nachisleno3), cleanNum(serviceEntity.oplacheno3), cleanNum(serviceEntity.dolg3)),
        cleanStr(serviceEntity.service4) to listOf(cleanNum(serviceEntity.zadol4), cleanNum(serviceEntity.nachisleno4), cleanNum(serviceEntity.oplacheno4), cleanNum(serviceEntity.dolg4))
      )

      servicesData.forEach { (name, values) ->
        if (name.isNotBlank()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            TableCell(text = name, weight = 1.3f, textAlign = TextAlign.Start) // Название слева
            values.forEach { valText -> TableCell(text = valText, textAlign = TextAlign.End) } // Цифры справа
          }
          TableDivider()
        }
      }

      // 3. ИТОГОВАЯ СТРОКА (Жирный, 12.sp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TableCell(text = stringResource(Res.string.summary), weight = 1.3f, isSummary = true, textAlign = TextAlign.Start)
        TableCell(text = cleanNum(serviceEntity.zadol), isSummary = true, textAlign = TextAlign.End)
        TableCell(text = cleanNum(serviceEntity.nachisleno), isSummary = true, textAlign = TextAlign.End)
        TableCell(text = cleanNum(serviceEntity.oplacheno), isSummary = true, textAlign = TextAlign.End)
        TableCell(text = cleanNum(serviceEntity.dolg), isSummary = true, textAlign = TextAlign.End)
      }
    }
  }
}





