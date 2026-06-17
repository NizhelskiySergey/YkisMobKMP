package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.PaymentEntity
import com.ykis.ykismobkmp.domain.mapper.toDbService
import com.ykis.ykismobkmp.domain.mapper.toDomainService
import com.ykis.ykismobkmp.db.DatabaseSchemaInitializer

/**
 * [LedgerDao] — КМР-класс доступа к запросам SQLDelight для начислений и оплат.
 */
class LedgerDao(
  private val dbQueries: YkisDatabasesQueries,
  private val driver: SqlDriver,
  private val schemaInitializer: DatabaseSchemaInitializer
) {
  private val className = "LedgerDao"

  private suspend fun ensureSchema() {
    schemaInitializer.ensureSchema(driver)
  }

  suspend fun insertService(services: List<ServiceEntity>) {
    ensureSchema()
    try {
        dbQueries.transaction {
          services.forEach { service ->
            dbQueries.insertService(service.toDbService())
          }
        }
    } catch (e: Exception) {
        println("[${className}_ERROR]: Помилка запису послуг: ${e.message}")
    }
  }

  suspend fun getServiceDetail(
    addressId: Long,
    service: String,
    year: String
  ): List<ServiceEntity> {
    ensureSchema()
    return dbQueries.getServiceDetail(addressId, service, year)
      .awaitAsList()
      .map { it.toDomainService() }
  }

  suspend fun deleteAllService() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllService()
    }
  }

  suspend fun getTotalDebt(addressId: Long): ServiceEntity? {
    ensureSchema()
    return dbQueries.getTotalDebt(addressId)
      .awaitAsOneOrNull()
      ?.toDomainService()
  }

  suspend fun deleteServiceByApartment(addressId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteServiceByAddressIds(addressId)
    }
  }
}
