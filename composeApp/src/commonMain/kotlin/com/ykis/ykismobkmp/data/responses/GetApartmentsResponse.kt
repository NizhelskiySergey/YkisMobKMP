package com.ykis.ykismobkmp.data.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity

@Serializable
class GetApartmentsResponse(
  @SerialName("success")
  override val success: Int,

  @SerialName("message")
  override val message: String,

  // ИСПРАВЛЕНО НАМЕРТВО: Тип изменен на нуллабельный Flow-список с дефолтным null.
  // Это позволяет Ktor десериализовать пустые ответы PHP-бэкенда без вылетов в SourceByteReadChannel!
  @SerialName("apartments")
  val apartments: List<ApartmentEntity>? = null
) : BaseResponse
