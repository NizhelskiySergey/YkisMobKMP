package com.ykis.ykismobkmp.ui.screens.service.payment.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ykis.mob.core.CenteredProgressIndicator
import com.ykis.mob.domain.payment.PaymentEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.mob.ui.components.GroupFilterChip
import com.ykis.ykismobkmp.ui.screens.service.ServiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun PaymentListStateful(
    serviceViewModel: ServiceViewModel,
    baseUIState: BaseUIState
) {
    val date = Date()
    val year = SimpleDateFormat("yyyy", Locale("uk")).format(Date(date.time))
    val paymentState by serviceViewModel.paymentState.collectAsStateWithLifecycle()

    var selectedChip by rememberSaveable { mutableStateOf(year) }


    LaunchedEffect(
        key1 = selectedChip,
        key2 = baseUIState.addressId
    ) {
        serviceViewModel.getPaymentList(
            baseUIState.addressId,
            selectedChip,
            baseUIState.uid.toString()  
        )
    }

    PaymentContentStateless(
        isLoading = paymentState.isLoading ,
        year = year,
        paymentList = paymentState.paymentList,
        selectedChip = selectedChip,
        onSelectedChanged = {
            selectedChip = it
        },
        osbb = baseUIState.osbb
    )
}

@Composable
fun PaymentContentStateless(
    isLoading:Boolean,
    year:String,
    paymentList : List<PaymentEntity>,
    selectedChip : String,
    onSelectedChanged: (String) -> Unit,
    osbb:String
) {

    val years = mutableListOf<String>()

    for (i in 0 until 20) {
        years.add((year.toInt() + (-i)).toString())
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        GroupFilterChip(
            list = years, selectedChip = selectedChip,
            onSelectedChanged = onSelectedChanged
        )
        Crossfade(
            targetState = isLoading,
            animationSpec = tween(delayMillis = 500), label = ""
        ) {
                targetState ->
            if(targetState){
                CenteredProgressIndicator()
            }else PaymentList(
              paymentList = paymentList,
              osbb = osbb
            )
        }
    }
}
