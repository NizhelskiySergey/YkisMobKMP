package com.ykis.ykismobkmp.ui.screens.service.payment.choice

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ykis.ykismobkmp.domain.repository.payment.request.PaymentParams
import com.ykis.mob.domain.service.request.ServiceParams
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.service.ServiceViewModel
import com.ykis.ykismobkmp.ui.screens.service.list.TotalDebtState
import com.ykis.ykismobkmp.ui.screens.service.list.assembleServiceList

package com.ykis.ykismobkmp.ui.screens.finance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННОЙ БИБЛИОТЕКИ MULTIPLATFORM WEBVIEW:
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState

// Импорты общих компонентов, стейтов и моделей ЮКИС
import com.ykis.ykismobkmp.ui.screens.meter.BaseUIState
import com.ykis.ykismobkmp.ui.screens.finance.ServiceScreenModel
import com.ykis.ykismobkmp.ui.screens.finance.TotalDebtState
import com.ykis.ykismobkmp.ui.screens.meter.ContentDetail
import com.ykis.ykismobkmp.ui.screens.finance.ServiceParams
import com.ykis.ykismobkmp.domain.repository.finance.InsertPaymentParams
import com.ykis.ykismobkmp.ui.screens.finance.components.PaymentChoiceItem

private const val className = "PaymentChoiceStateful"

// Временная КМР структура элемента списка услуг для корректной компиляции assembleServiceList
data class ServiceListItem(val name: String, val debt: Double, val contentDetail: ContentDetail)

/**
 * [PaymentChoiceStateful] — Кроссплатформенный Stateful-компонент формирования расщепленных инвойсов Xpay.
 */
@Composable
fun PaymentChoiceStateful(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  totalDebtState: TotalDebtState,
  navigateToWebView: (String) -> Unit
) {
  // Инжектируем очищенную КМР финансовую модель экрана через Koin
  val screenModel = koinInject<ServiceScreenModel>()

  // ИСПРАВЛЕНО: collectAsStateWithLifecycle заменен универсальным КМР collectAsState()
  val loading by screenModel.insertPaymentLoading.collectAsState()

  // Каскадный триггер обновления долгов ГИОЦ при смене активного адреса квартиры
  LaunchedEffect(key1 = baseUIState.addressId) {
    if (baseUIState.addressId != 0L) {
      screenModel.getTotalServiceDebt(
        ServiceParams(
          uid = baseUIState.uid.toString(),
          addressId = baseUIState.addressId
        )
      )
    }
  }

  val serviceList = assembleServiceList(totalDebtState = totalDebtState, baseUIState = baseUIState)

  var osbbField by rememberSaveable { mutableStateOf("0.00") }
  var waterField by rememberSaveable { mutableStateOf("0.00") }
  var heatField by rememberSaveable { mutableStateOf("0.00") }
  var tboField by rememberSaveable { mutableStateOf("0.00") }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 16.dp)
  ) {
    items(serviceList) { item ->
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
        onCheckedTrue = { _, debt ->
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

      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .height(50.dp),
        onClick = {
          // ИСПРАВЛЕНО: Создаем чистый КМР-класс InsertPaymentParams со сквозными типами Long и Double
          screenModel.insertPayment(
            params = InsertPaymentParams(
              uid = baseUIState.uid.toString(),
              addressId = baseUIState.addressId,
              kvartplata = osbbField.toDoubleOrNull() ?: 0.0,
              rfond = 0.0,
              teplo = heatField.toDoubleOrNull() ?: 0.0,
              voda = waterField.toDoubleOrNull() ?: 0.0,
              tbo = tboField.toDoubleOrNull() ?: 0.0
            ),
            onSuccess = { securedUrl ->
              navigateToWebView(securedUrl)
            }
          )
        }
      ) {
        AnimatedVisibility(visible = loading) {
          // ИСПРАВЛЕНО: Внутренний модификатор изолирован от внешнего modifier для защиты геометрии кнопок на Mac
          CircularProgressIndicator(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp
          )
        }
        AnimatedVisibility(visible = !loading) {
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
 * [KmpWebView] — Полностью кроссплатформенный компонент безопасного отображения платежного инвойса Xpay.
 * ИСПРАВЛЕНО: Заменен нативный AndroidView WebView на КМР Multiplatform WebView.
 */
@Composable
fun KmpWebView(uri: String, modifier: Modifier = Modifier) {
  val formattedUri = remember(uri) { uri.replace("*", "/") }
  val webViewState = rememberWebViewState(formattedUri)

  WebView(
    state = webViewState,
    modifier = modifier.fillMaxSize()
  )
}

// Вспомогательный хелпер сборки списков долгов (замени на свою реальную доменную функцию сборщика)
private fun assembleServiceList(totalDebtState: TotalDebtState, baseUIState: BaseUIState): List<ServiceListItem> {
  return listOf(
    ServiceListItem("Утримання будинку (Квартплата)", 145.50, ContentDetail.OSBB),
    ServiceListItem("Водопостачання та стоки", 88.20, ContentDetail.WATER_SERVICE),
    ServiceListItem("Центральне опалення ЮТКЕ", 412.00, ContentDetail.WARM_SERVICE),
    ServiceListItem("Вивіз побутових відходів (ТБО)", 32.40, ContentDetail.GARBAGE_SERVICE)
  )
}

