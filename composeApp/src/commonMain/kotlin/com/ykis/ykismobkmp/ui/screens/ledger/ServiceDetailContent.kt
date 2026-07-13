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
  val totalServiceDebt = remember(contentDetail, ledgerUIState.totalDebt) {
    when (contentDetail) {
      ContentDetail.WATER_SERVICE   -> ledgerUIState.totalDebt.dolg1
      ContentDetail.WARM_SERVICE    -> ledgerUIState.totalDebt.dolg2
      ContentDetail.GARBAGE_SERVICE -> ledgerUIState.totalDebt.dolg3
      ContentDetail.OSBB            -> ledgerUIState.totalDebt.dolg4
      else -> 0.0
    }
  }

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
                  try { uriHandler.openUri(url) } catch (e: Exception) { }
              }
            },
            enabled = !fastpayToken.isNullOrBlank(),
            modifier = Modifier.height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (fastpayToken.isNullOrBlank()) Color.Gray else Color(0xFF7CB342))
          ) {
            Icon(painter = painterResource(Res.drawable.privatbank), contentDescription = null, modifier = Modifier.size(54.dp).padding(top=4.dp), tint = Color.Unspecified)
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
  Column(modifier = modifier.fillMaxSize()) {
    DefaultAppBar(
      canNavigateBack = true,
      onBackClick = { screenModel.closeContentDetail() },
      title = when (contentDetail) {
        ContentDetail.OSBB -> "ОСББ"
        ContentDetail.WATER_SERVICE -> stringResource(Res.string.vodokanal)
        ContentDetail.WARM_SERVICE -> stringResource(Res.string.ytke)
        ContentDetail.GARBAGE_SERVICE -> stringResource(Res.string.yzhtrans)
        else -> "Коммунальные услуги"
      },
      subtitle = baseUIState.address
    )
    FastPayPaymentRow(contentDetail = contentDetail, baseUIState = baseUIState, ledgerUIState = ledgerUIState)
    Box(modifier = Modifier.weight(1f)) {
      ServiceDetailContentContainer(modifier = Modifier.fillMaxSize(), contentDetail = contentDetail, baseUIState = baseUIState, ledgerUIState = ledgerUIState, screenModel = screenModel)
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
    contentDetail = contentDetail, // Передаємо сюди
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
  contentDetail: ContentDetail, // Додано параметр
  onSelectedChanged: (String) -> Unit
) {
  val yearsList = remember(year) {
    val baseYear = year.toIntOrNull() ?: 2026
    List(20) { index -> (baseYear - index).toString() }
  }

  Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
    GroupFilterChip(list = yearsList, selectedChip = selectedChip, onSelectedChanged = onSelectedChanged)
    Crossfade(targetState = isLoading, animationSpec = tween(300), label = "ServiceDetailCrossfade") { isCurrentlyLoading ->
      if (isCurrentlyLoading) { CenteredProgressIndicator() } 
      else { ListServiceDetails(listServiceEntity = serviceEntities, contentDetail = contentDetail) }
    }
  }
}

@Composable
fun GroupFilterChip(list: List<String>, selectedChip: String, onSelectedChanged: (String) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    list.forEach { year -> FilterChip(selected = selectedChip == year, onClick = { onSelectedChanged(year) }, label = { Text(year) }) }
  }
}

@Composable
fun ListServiceDetails(listServiceEntity: List<ServiceEntity>, contentDetail: ContentDetail) {
  if (listServiceEntity.isEmpty()) {
    EmptyListState(title = stringResource(Res.string.no_payment), subtitle = stringResource(Res.string.no_payment_year))
  } else {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
      itemsIndexed(items = listServiceEntity, key = { index, item -> "item_${index}_${item.data}" }) { index, item ->
        ServiceDetailItem(serviceEntity = item, contentDetail = contentDetail)
      }
    }
  }
}

private fun formatUkMonth(dateString: String?): String {
  if (dateString.isNullOrBlank() || !dateString.contains("-")) return "Звітний місяць"
  val parts = dateString.split("-")
  if (parts.size < 2) return "Звітний місяць"
  val monthInt = parts[1].toIntOrNull() ?: return "Звітний місяць"
  val monthName = when (monthInt) {
    1 -> "Січень" 2 -> "Лютий" 3 -> "Березень" 4 -> "Квітень" 5 -> "Травень" 6 -> "Червень"
    7 -> "Липень" 8 -> "Серпень" 9 -> "Вересень" 10 -> "Жовтень" 11 -> "Листопад" 12 -> "Грудень"
    else -> "Місяць"
  }
  return "$monthName ${parts[0]}"
}

@Composable
fun ServiceDetailItem(
  modifier: Modifier = Modifier,
  serviceEntity: ServiceEntity = ServiceEntity(),
  contentDetail: ContentDetail
) {
  val contentType = LocalContentType.current
  val isDualPane = contentType == ContentType.DUAL_PANE
  val formattedMonthHeader = remember(serviceEntity.data) { formatUkMonth(serviceEntity.data) }

  val cleanStr: (Any?) -> String = { valStr ->
    val s = valStr?.toString() ?: ""
    val cleaned = if (s.equals("none", ignoreCase = true) || s.equals("null", ignoreCase = true)) "" else s
    if (cleaned.length > 9) cleaned.take(8) + "…" else cleaned
  }
  val cleanNum: (Double?) -> String = { num -> if (num == null || num == 0.0) "0.00" else num.toString() }

  BaseCard(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)) {
    Text(text = formattedMonthHeader, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 0.dp)) {
      if (contentDetail == ContentDetail.OSBB) {
        // --- СТАРИЙ МАКЕТ ДЛЯ ОСББ ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          TableCell(text = stringResource(Res.string.services), weight = 1.3f, isHeader = true, textAlign = TextAlign.Start)
          TableCell(text = stringResource(if (isDualPane) Res.string.start_debt_full else Res.string.start_debt), isHeader = true, textAlign = TextAlign.End, weight = 1f)
          TableCell(text = stringResource(if (isDualPane) Res.string.accrued_text_full else Res.string.accrued_text), isHeader = true, textAlign = TextAlign.End)
          TableCell(text = stringResource(if (isDualPane) Res.string.paid_full else Res.string.paid), isHeader = true, textAlign = TextAlign.End)
          TableCell(text = stringResource(if (isDualPane) Res.string.end_debt_full else Res.string.end_debt), isHeader = true, textAlign = TextAlign.End)
        }
        TableDivider()

        val rows = listOf(
            Triple(serviceEntity.service1, serviceEntity.zadol1, listOf(serviceEntity.nachisleno1, serviceEntity.oplacheno1, serviceEntity.dolg1)),
            Triple(serviceEntity.service2, serviceEntity.zadol2, listOf(serviceEntity.nachisleno2, serviceEntity.oplacheno2, serviceEntity.dolg2)),
            Triple(serviceEntity.service3, serviceEntity.zadol3, listOf(serviceEntity.nachisleno3, serviceEntity.oplacheno3, serviceEntity.dolg3)),
            Triple(serviceEntity.service4, serviceEntity.zadol4, listOf(serviceEntity.nachisleno4, serviceEntity.oplacheno4, serviceEntity.dolg4))
        )
        rows.forEach { (name, zadol, vals) ->
          if (!name.isNullOrBlank() && name != "none") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              TableCell(text = cleanStr(name), weight = 1.3f, textAlign = TextAlign.Start)
              TableCell(text = cleanNum(zadol), textAlign = TextAlign.End, weight = 1f)
              vals.forEach { TableCell(text = cleanNum(it), textAlign = TextAlign.End) }
            }
            TableDivider()
          }
        }
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)), verticalAlignment = Alignment.CenterVertically) {
          TableCell(text = stringResource(Res.string.summary), weight = 1.3f, isSummary = true, textAlign = TextAlign.Start)
          TableCell(text = cleanNum(serviceEntity.zadol), isSummary = true, textAlign = TextAlign.End)
          TableCell(text = cleanNum(serviceEntity.nachisleno), isSummary = true, textAlign = TextAlign.End)
          TableCell(text = cleanNum(serviceEntity.oplacheno), isSummary = true, textAlign = TextAlign.End)
          TableCell(text = cleanNum(serviceEntity.dolg), isSummary = true, textAlign = TextAlign.End)
        }
      } else {
        // --- НОВИЙ МАКЕТ ДЛЯ ІНШИХ ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          TableCell(text = stringResource(if (isDualPane) Res.string.start_debt_full else Res.string.start_debt), isHeader = true, textAlign = TextAlign.Start, weight = 1f)
          TableCell(text = stringResource(Res.string.services), weight = 1.3f, isHeader = true, textAlign = TextAlign.Start) // Вліво
          TableCell(text = stringResource(if (isDualPane) Res.string.accrued_text_full else Res.string.accrued_text), isHeader = true, textAlign = TextAlign.End, weight = 1f)
          TableCell(text = stringResource(if (isDualPane) Res.string.paid_full else Res.string.paid), isHeader = true, textAlign = TextAlign.End, weight = 1f)
          TableCell(text = stringResource(if (isDualPane) Res.string.end_debt_full else Res.string.end_debt), isHeader = true, textAlign = TextAlign.End, weight = 1f)
        }
        TableDivider()

        val activeServices = remember(serviceEntity, contentDetail) {
            val list = mutableListOf<Pair<String, Double>>()
            fun addIf(n: String?, v: Double?) { if (!n.isNullOrBlank() && n.lowercase() != "none") list.add(n to (v ?: 0.0)) }
            addIf(serviceEntity.service1, serviceEntity.nachisleno1)
            addIf(serviceEntity.service2, serviceEntity.nachisleno2)
            addIf(serviceEntity.service3, serviceEntity.nachisleno3)
            addIf(serviceEntity.service4, serviceEntity.nachisleno4)
            if (list.isEmpty() && (serviceEntity.nachisleno ?: 0.0) != 0.0) {
                list.add((if(contentDetail==ContentDetail.WARM_SERVICE) "Опалення" else "Послуги") to (serviceEntity.nachisleno ?: 0.0))
            }
            list
        }

        if (activeServices.isNotEmpty()) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell(text = cleanNum(serviceEntity.zadol), weight = 1f, textAlign = TextAlign.Start, isSummary = true)
            Column(modifier = Modifier.weight(2.3f)) {
              activeServices.forEachIndexed { i, (n, a) ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  TableCell(text = cleanStr(n), weight = 1.3f, textAlign = TextAlign.Start)
                  TableCell(text = cleanNum(a), weight = 1f, textAlign = TextAlign.End)
                }
                if (i < activeServices.size - 1) TableDivider()
              }
            }
            TableCell(text = cleanNum(serviceEntity.oplacheno), weight = 1f, textAlign = TextAlign.End, isSummary = true)
            TableCell(text = cleanNum(serviceEntity.dolg), weight = 1f, textAlign = TextAlign.End, isSummary = true)
          }
          TableDivider()
        }
      }
    }
  }
}
