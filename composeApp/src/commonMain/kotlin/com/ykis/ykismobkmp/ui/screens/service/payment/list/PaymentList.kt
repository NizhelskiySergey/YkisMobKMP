package com.ykis.ykismobkmp.ui.screens.service.payment.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ykis.mob.R
import com.ykis.mob.domain.payment.PaymentEntity
import com.ykis.mob.ui.components.EmptyListState

@Composable
fun PaymentList(
    paymentList : List<PaymentEntity>,
    osbb:String
) {
    if (paymentList.isEmpty()) {
        EmptyListState(
            title = stringResource(id = R.string.no_payment),
            subtitle = stringResource(id = R.string.no_payment_year)
        )
    } else LazyColumn {
        items(items = paymentList) { payment ->
          PaymentListItem(
            item = payment,
            osbb = osbb
          )
        }
    }
}
