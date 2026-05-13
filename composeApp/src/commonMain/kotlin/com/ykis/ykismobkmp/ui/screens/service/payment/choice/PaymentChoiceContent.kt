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
import com.ykis.mob.domain.payment.request.InsertPaymentParams
import com.ykis.mob.domain.service.request.ServiceParams
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.mob.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.service.ServiceViewModel
import com.ykis.ykismobkmp.ui.screens.service.list.TotalDebtState
import com.ykis.ykismobkmp.ui.screens.service.list.assembleServiceList


@Composable
fun PaymentChoiceStateful(
    modifier: Modifier = Modifier,
    baseUIState: BaseUIState,
    totalDebtState: TotalDebtState,
    viewModel: ServiceViewModel,
    navigateToWebView: (String) -> Unit
) {
    val loading by viewModel.insertPaymentLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun intent(uri:String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
//    val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/")) }

    LaunchedEffect(key1 = baseUIState.addressId) {
        viewModel.getTotalServiceDebt(
            ServiceParams(
                uid = baseUIState.uid!!,
                addressId = baseUIState.addressId,
                houseId = baseUIState.houseId,
                service = 0,
                total = 1,
                year = "2023"
            )
        )
    }
    val serviceList =
        assembleServiceList(totalDebtState = totalDebtState, baseUIState = baseUIState)
    var osbbField by rememberSaveable {
        mutableStateOf("0.00")
    }
    var waterField by rememberSaveable {
        mutableStateOf("0.00")
    }
    var heatField by rememberSaveable {
        mutableStateOf("0.00")
    }
    var tboField by rememberSaveable {
        mutableStateOf("0.00")
    }
    LazyColumn {
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
            onCheckedTrue = { service, debt ->
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
            userInput = currentField,
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
            Button(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    viewModel.insertPayment(
                        params = InsertPaymentParams(
                            uid = baseUIState.uid.toString(),
                            addressId = baseUIState.addressId,
                            kvartplata = osbbField.toDoubleOrNull() ?: 0.0,
                            rfond = 0.0,
                            teplo = heatField.toDoubleOrNull() ?: 0.0,
                            voda = waterField.toDoubleOrNull() ?: 0.0,
                            tbo = tboField.toDoubleOrNull() ?: 0.0
                        ),
                        onSuccess = {
                            navigateToWebView(it)
                        }
                    )
                    // TODO: make for empty user fields 
                }
            ) {
                AnimatedVisibility(visible = loading) {
                    CircularProgressIndicator(
                        modifier = modifier.size(
                            ButtonDefaults.IconSize
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                }
                AnimatedVisibility(visible = !loading) {
                    Text(
                        "Перейти до сплати"
                    )
                }
            }
        }
    }
}

@Composable
fun WebView(uri:String){
    val formattedUri = uri.replace("*" , "/")
    AndroidView(factory = {
        android.webkit.WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
        }
    }, update = {
        it.loadUrl(formattedUri)
    })
}
