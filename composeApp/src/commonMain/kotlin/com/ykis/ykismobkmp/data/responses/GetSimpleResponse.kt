package com.ykis.ykismobkmp.data.responses


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetSimpleResponse(
  @SerialName("success") override val success: Int = 0,
  @SerialName("message") override val message: String = "",
  @SerialName("addressId") val addressId: Int = 0,
  @SerialName("address") val address: String? = null, // ТУТ ДОБАВИЛ ?
  @SerialName("userRole") val userRole: String = "StandardUser",
  @SerialName("osbbId") val osbbId: Int = 0,
  @SerialName("osbb") val osbb: String? = null,    // ТУТ ТОЖЕ ЛУЧШЕ ДОБАВИТЬ ?
) : BaseResponse

