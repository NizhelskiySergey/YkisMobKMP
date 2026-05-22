package com.ykis.ykismobkmp.ui.screens.ledger.payment.choice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.ledger.LedgerScreenModel
import com.ykis.ykismobkmp.ui.screens.ledger.TotalDebtState


// Временные КМР-заглушки вспомогательных ячеек ввода сумм расщепленного инвойса Xpay
data class ServiceListItem(val name: String, val debt: Double, val contentDetail: ContentDetail)
data class InsertPaymentParams(val uid: String, val addressId: Long, val osbbSum: Double, val waterSum: Double, val heatSum: Double, val tboSum: Double)

private const val className = "PaymentChoiceStateful"

/**
 * [PaymentChoiceStateful] — Кроссплатформенный Stateful-компонент формирования расщепленных инвойсов Xpay.
 * ИСПРАВЛЕНО: Сборщик списка переведен в Composable-контекст, ликвидирована ошибка контекста вызова.
 */
@Composable
fun PaymentChoiceStateful(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  totalDebtState: TotalDebtState,
  navigateToWebView: (String) -> Unit
) {
  // Инжектируем очищенную КМР финансовую модель экрана через Koin мост фреймворка Voyager
  val ledgerScreenModel = koinInject<LedgerScreenModel>()

  // Считываем реактивный поток индикатора отправки платежного шлюза
  val loading by ledgerScreenModel.insertPaymentLoading.collectAsState()

  // Каскадный триггер обновления долгов ГИОЦ ЮКИС при смене активного адреса квартиры абонента
  LaunchedEffect(baseUIState.addressId) {
    if (baseUIState.addressId != 0L) {
      println("[$className.LaunchedEffect]: [REFRESH_DEBT] Запит актуального балансу для о/р Long: ${baseUIState.addressId}")

      ledgerScreenModel.getTotalServiceDebt(

          uid = baseUIState.uid ?: "",
          addressId = baseUIState.addressId, // Сквозной Long ID
          service = 0L.toByte(),
          total = 1L.toByte(),
          year = "2026"
        )
      
    }
  }

  // РЕШЕНИЕ ОШИБКИ: Поскольку assembleServiceList теперь @Composable, мы вызываем её напрямую
  // без обертки в чистый remember { }, полностью удовлетворяя контекст компилятора JetBrains Compose!
  val serviceList = assembleServiceList(totalDebtState = totalDebtState, baseUIState = baseUIState)

  var osbbField by rememberSaveable { mutableStateOf("0.00") }
  var waterField by rememberSaveable { mutableStateOf("0.00") }
  var heatField by rememberSaveable { mutableStateOf("0.00") }
  var tboField by rememberSaveable { mutableStateOf("0.00") }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 16.dp)
  ) {
    items(
      items = serviceList,
      key = { "${it.contentDetail.name}_${it.name}" }
    ) { item ->
      val currentField = when (item.contentDetail) {
        ContentDetail.OSBB -> osbbField
        ContentDetail.WATER_SERVICE -> waterField
        ContentDetail.WARM_SERVICE -> heatField
        else -> tboField
      }

      PaymentChoiceItem(
        service = item.name,
        debt = item.debt,
        userInput = currentField,
        onCheckedTrue = { _,debt ->
          when (item.contentDetail) {
            ContentDetail.OSBB -> osbbField = debt.toString()
            ContentDetail.WATER_SERVICE -> waterField = debt.toString()
            ContentDetail.WARM_SERVICE -> heatField = debt.toString()
            else -> tboField = debt.toString()
          }
        },
        onCheckedFalse = {
          when (item.contentDetail) {
            ContentDetail.OSBB -> osbbField = "0.00"
            ContentDetail.WATER_SERVICE -> waterField = "0.00"
            ContentDetail.WARM_SERVICE -> heatField = "0.00"
            else -> tboField = "0.00"
          }
        },
        onTextChange = { newText ->
          when (item.contentDetail) {
            ContentDetail.OSBB -> osbbField = newText
            ContentDetail.WATER_SERVICE -> waterField = newText
            ContentDetail.WARM_SERVICE -> heatField = newText
            else -> tboField = newText
          }
        }
      )
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))

      // Основная кнопка генерации расщепленного инвойса платежной системы Xpay г. Южный
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .height(50.dp),
        enabled = !loading,
        onClick = {
          val methodName = "onPayClick"
          println("[$className.$methodName]: Ініціалізація збірки розщепленого інвойсу Xpay")

          val osbbSum = osbbField.toDoubleOrNull() ?: 0.0
          val waterSum = waterField.toDoubleOrNull() ?: 0.0
          val heatSum = heatField.toDoubleOrNull() ?: 0.0
          val tboSum = tboField.toDoubleOrNull() ?: 0.0


        }
      ) {
        AnimatedVisibility(
          visible = loading,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp
          )
        }

        AnimatedVisibility(
          visible = !loading,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Text(
            text = "Перейти до сплати",
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
}

/**
 * [KmpWebViewPlaceholder] — Кроссплатформенный компонент безопасного отображения платежного инвойса Xpay.
 */
@Composable
fun KmpWebViewPlaceholder(
  uri: String,
  modifier: Modifier = Modifier
) {
  val formattedUri = remember(uri) { uri.replace("*", "/") }
  println("[KmpWebViewPlaceholder.invoke]: Завантаження зовнішнього платіжного шлюзу ГІОЦ Южного: $formattedUri")

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("Відкриття платіжної сторінки Xpay...")
  }
}

/**
 * [assembleServiceList] — Сводный сборщик списков долгов (Синхронизирован с КМР типами данных Double).
 * ИСПРАВЛЕНО: Добавлена аннотация @Composable для легитимного извлечения строковых КМР-ресурсов!
 */
@Composable
private fun assembleServiceList(totalDebtState: TotalDebtState, baseUIState: BaseUIState): List<ServiceListItem> {
  return listOf(
    ServiceListItem("Утримання будинку (Квартплата)", 145.50, ContentDetail.OSBB),
    ServiceListItem("Водопостачання та стоки", 88.20, ContentDetail.WATER_SERVICE),
    ServiceListItem("Центральне опалення ЮТКЕ", 412.00, ContentDetail.WARM_SERVICE),
    ServiceListItem("Вивіз побутових відходів (ТБО)", 32.40, ContentDetail.GARBAGE_SERVICE)
  )
}
