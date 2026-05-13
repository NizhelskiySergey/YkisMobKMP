package com.ykis.ykismobkmp.data.responses

import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetPaymentResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  // В JSON от PHP поле обычно называется 'payments'
  @SerialName("payments")
  val payments: List<PaymentEntity> = emptyList()
) : BaseResponse
