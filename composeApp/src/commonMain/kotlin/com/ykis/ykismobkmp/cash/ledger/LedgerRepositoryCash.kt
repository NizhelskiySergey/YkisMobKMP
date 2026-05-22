package com.ykis.ykismobkmp.cash.ledger

import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.entity.ServiceEntity

/**
 * [LedgerRepositoryCash] — Главный доменный КМР-контракт локального кэша начислений и платежей ЮКИС.
 */
interface LedgerRepositoryCash {
  suspend fun addPayments(payments: List<PaymentEntity>)
  suspend fun getPaymentsFromFlat(addressId: Long): List<PaymentEntity>
  suspend fun deleteAllPayment()
  suspend fun deletePaymentByApartment(addressId: Long)

  suspend fun addService(service: List<ServiceEntity>)
  suspend fun getServiceDetail(addressId: Long, service: String, year: String): List<ServiceEntity>
  suspend fun deleteAllService()
  suspend fun getTotalDebt(addressId: Long): ServiceEntity?
  suspend fun deleteServiceByApartment(addressId: Long)
}
