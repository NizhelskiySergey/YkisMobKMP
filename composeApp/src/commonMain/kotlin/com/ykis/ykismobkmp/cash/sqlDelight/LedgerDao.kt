package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.mapper.toDbPayment
import com.ykis.ykismobkmp.domain.mapper.toDbService
import com.ykis.ykismobkmp.domain.mapper.toDomainService
import com.ykis.ykismobkmp.domain.mapper.toDomainPayment

/**
 * [LedgerDao] — КМР-класс доступа к сгенерированному интерфейсу запросов SQLDelight 2.x для начислений и оплат.
 * ИСПРАВЛЕНО: Префикс логирования изменен на YkisLogKMP.
 */
class LedgerDao(
  private val dbQueries: YkisDatabasesQueries
) {
  private val className = "LedgerDao"

  suspend fun insertService(services: List<ServiceEntity>) {
    dbQueries.transaction {
      services.forEach { service ->
        dbQueries.insertService(service.toDbService())
      }
    }
  }

  suspend fun getServiceDetail(addressId: Long, service: String, year: String): List<ServiceEntity> {
    return dbQueries.getServiceDetail(addressId, service, year).executeAsList().map { it.toDomainService() }
  }

  suspend fun deleteAllService() {
    dbQueries.deleteAllService()
  }

  suspend fun getTotalDebt(addressId: Long): ServiceEntity? {
    return dbQueries.getTotalDebt(addressId).executeAsOneOrNull()?.toDomainService()
  }

  suspend fun deleteServiceByApartment(addressId: Long) {
    dbQueries.deleteServiceByAddressIds(addressId)
    println("[YkisLogKMP.$className.deleteServiceByApartment]: Успешно зачищен кэш начислений для ID: $addressId")
  }

  suspend fun insertPayment(payments: List<PaymentEntity>) {
    dbQueries.transaction {
      payments.forEach { payment ->
        dbQueries.insertPayment(payment.toDbPayment())
      }
    }
  }

  suspend fun getPaymentFromFlat(addressId: Long): List<PaymentEntity> {
    return dbQueries.getPaymentFromFlat(addressId).executeAsList().map { it.toDomainPayment() }
  }

  suspend fun deleteAllPayment() {
    dbQueries.deleteAllPayment()
  }

  suspend fun deletePaymentByApartment(addressId: Long) {
    dbQueries.deletePaymentByAddressIds(addressId)
    println("[YkisLogKMP.$className.deletePaymentByApartment]: Успешно зачищен кэш оплат для ID: $addressId")
  }
}
