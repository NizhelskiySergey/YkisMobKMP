package com.ykis.mob.data.remote.service

import com.ykis.ykismobkmp.data.responses.GetServiceResponse

data class ServiceParams(
  val uid:String,
  val addressId:Int,
  val houseId:Int ,
  val service:Byte,
  val total:Byte,
  val year : String ,
)
interface ServiceRemote {
    suspend fun getFlatDetailServices(params : ServiceParams): GetServiceResponse
    suspend fun getTotalDebtService(params: ServiceParams):GetServiceResponse
}

