package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.mapper.toDbService
import com.ykis.ykismobkmp.domain.mapper.toDomainService

/**
 * [LedgerDao] — КМР-класс доступа к запросам SQLDelight для начислений и оплат.
 */
class LedgerDao(
  private val dbQueries: YkisDatabasesQueries
) {
  suspend fun insertService(services: List<ServiceEntity>) {
    dbQueries.transaction {
      services.forEach { service ->
        dbQueries.insertService(service.toDbService())
      }
    }
  }

  suspend fun getServiceDetail(
    addressId: Long,
    service: String,
    year: String
  ): List<ServiceEntity> {
    return dbQueries.getServiceDetail(addressId, service, year)
      .awaitAsList()
      .map { it.toDomainService() }
  }

  suspend fun deleteAllService() {
    dbQueries.deleteAllService()
  }

  suspend fun getTotalDebt(addressId: Long): ServiceEntity? {
    return dbQueries.getTotalDebt(addressId)
      .awaitAsOneOrNull()
      ?.toDomainService()
  }

  suspend fun deleteServiceByApartment(addressId: Long) {
    dbQueries.transaction {
      dbQueries.deleteServiceByAddressIds(addressId)
    }
  }
}
