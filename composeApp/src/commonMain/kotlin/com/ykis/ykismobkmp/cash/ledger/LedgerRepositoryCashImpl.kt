package com.ykis.ykismobkmp.cash.ledger

import com.ykis.ykismobkmp.cash.sqlDelight.LedgerDao
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.entity.ServiceEntity

/**
 * [LedgerRepositoryCashImpl] — Реализация кэша Ledger на одиночных идентификаторах Long.
 */
class LedgerRepositoryCashImpl(
  private val ledgerDao: LedgerDao
) : LedgerRepositoryCash {

  override suspend fun addService(service: List<ServiceEntity>) {
    ledgerDao.insertService(service)
  }

  override suspend fun getServiceDetail(addressId: Long, service: String, year: String): List<ServiceEntity> {
    return ledgerDao.getServiceDetail(addressId, service, year)
  }

  override suspend fun deleteAllService() {
    ledgerDao.deleteAllService()
  }

  override suspend fun getTotalDebt(addressId: Long): ServiceEntity? {
    return ledgerDao.getTotalDebt(addressId)
  }

  override suspend fun deleteServiceByApartment(addressId: Long) {
    ledgerDao.deleteServiceByApartment(addressId)
  }

  override suspend fun addPayments(payments: List<PaymentEntity>) {
    ledgerDao.insertPayment(payments)
  }

  override suspend fun getPaymentsFromFlat(addressId: Long): List<PaymentEntity> {
    return ledgerDao.getPaymentFromFlat(addressId)
  }

  override suspend fun deleteAllPayment() {
    ledgerDao.deleteAllPayment()
  }

  override suspend fun deletePaymentByApartment(addressId: Long) {
    ledgerDao.deletePaymentByApartment(addressId)
  }
}
