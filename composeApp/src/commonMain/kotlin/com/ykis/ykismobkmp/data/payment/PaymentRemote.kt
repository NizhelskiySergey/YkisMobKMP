package com.ykis.ykismobkmp.data.payment


import com.ykis.ykismobkmp.data.responses.GetPaymentResponse
import com.ykis.ykismobkmp.data.responses.InsertPaymentResponse

data class InsertPaymentParams(
  val uid: String,
  val addressId: Int,
  val kvartplata: Double,
  val rfond: Double,
  val teplo: Double,
  val voda: Double,
  val tbo: Double,
)
interface PaymentRemote {
   suspend fun getPaymentList(addressId:Int , year:String ,uid:String) : GetPaymentResponse
   suspend fun insertPayment(params:InsertPaymentParams): InsertPaymentResponse
}
