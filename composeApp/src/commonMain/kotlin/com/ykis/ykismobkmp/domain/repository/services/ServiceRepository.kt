package com.ykis.ykismobkmp.domain.repository.services

import com.ykis.ykismobkmp.data.responses.GetServiceResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "ServiceParams"

/**
 * [ServiceParams] — Кроссплатформенная модель параметров сетевых запросов к API ГИОЦ г. Южный.
 */
@Serializable
data class ServiceParams(
  @SerialName("uid")
  val uid: String,

  @SerialName("addressId")
  val addressId: Long,

  @SerialName("houseId")
  val houseId: Long,

  @SerialName("service")
  val service: Byte,

  @SerialName("total")
  val total: Byte,

  @SerialName("year")
  val year: String
) {
  init {
    // Логирование создания параметров согласно правилу [Класс.Метод]
    println("[$className.init]: Сформированы КМР-параметры ГИОЦ. addressId=$addressId, service=$service, year=$year")
  }
}

interface ServiceRepository {
    suspend fun getFlatDetailService(params: ServiceParams): GetServiceResponse
    suspend fun getTotalDebtService(params: ServiceParams):GetServiceResponse
}
