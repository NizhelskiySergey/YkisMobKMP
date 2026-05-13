package com.ykis.ykismobkmp.ui.screens.service.payment.list

import com.ykis.mob.domain.payment.PaymentEntity

data class PaymentState(
    val paymentList : List<PaymentEntity> = emptyList(),
    val isLoading:Boolean = true
)
