package com.ykis.ykismobkmp.data.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class InsertPaymentResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  @SerialName("payment_id") // Заменили @Json на @SerialName
  val paymentId: Int = 0,

  @SerialName("uri")
  val uri: String = ""
) : BaseResponse
