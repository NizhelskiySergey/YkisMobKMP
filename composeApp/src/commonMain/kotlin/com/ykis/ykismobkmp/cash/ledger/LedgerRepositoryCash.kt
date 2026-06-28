package com.ykis.ykismobkmp.cash.ledger

import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.FastpayEntity

/**
 * [LedgerRepositoryCash] — Главный доменный КМР-контракт локального кэша начислений и платежей ЮКИС.
 */
interface LedgerRepositoryCash {

  suspend fun addService(service: List<ServiceEntity>)
  suspend fun getServiceDetail(addressId: Long, service: String, year: String): List<ServiceEntity>
  suspend fun deleteAllService()
  suspend fun getTotalDebt(addressId: Long): ServiceEntity?
  suspend fun deleteServiceByApartment(addressId: Long)

  suspend fun insertFastpayTokens(tokens: List<FastpayEntity>)
  suspend fun getFastpayTokens(): List<FastpayEntity>
}
