package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.mapper.toDbHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDbHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDbWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDbWaterReading
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainHeatReading
import com.ykis.ykismobkmp.domain.mapper.toDomainWaterMeter
import com.ykis.ykismobkmp.domain.mapper.toDomainWaterReading
import com.ykis.ykismobkmp.db.DatabaseSchemaInitializer

/**
 * [MeterDao] — КМР-класс доступа к запросам SQLDelight.
 */
class MeterDao(
  private val dbQueries: YkisDatabasesQueries,
  private val driver: SqlDriver,
  private val schemaInitializer: DatabaseSchemaInitializer
) {
  private suspend fun ensureSchema() {
    schemaInitializer.ensureSchema(driver)
  }

  suspend fun insertHeatMeter(heatMeters: List<HeatMeterEntity>) {
    ensureSchema()
    dbQueries.transaction {
      heatMeters.forEach { meter ->
        dbQueries.insertHeatMeter(meter.toDbHeatMeter())
      }
    }
  }

  suspend fun getHeatMetersByApartment(addressId: Long): List<HeatMeterEntity> {
    ensureSchema()
    return dbQueries.getHeatMetersByApartment(addressId)
      .awaitAsList()
      .map { it.toDomainHeatMeter() }
  }

  suspend fun deleteHeatMetersByApartment(addressId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteHeatMetersByApartment(addressId)
    }
  }

  suspend fun deleteAllHeatMeters() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllHeatMeters()
    }
  }

  suspend fun insertHeatReadings(heatReadings: List<HeatReadingEntity>) {
    ensureSchema()
    dbQueries.transaction {
      heatReadings.forEach { reading ->
        dbQueries.insertHeatReading(reading.toDbHeatReading())
      }
    }
  }

  suspend fun getHeatReadingsByMeter(teplomerId: Long): List<HeatReadingEntity> {
    ensureSchema()
    return dbQueries.getHeatReadingsByMeter(teplomerId)
      .awaitAsList()
      .map { it.toDomainHeatReading() }
  }

  suspend fun getLastHeatReadingByMeter(pokId: Long): HeatReadingEntity? {
    ensureSchema()
    return dbQueries.getLastHeatReadingByMeter(pokId)
      .awaitAsOneOrNull()
      ?.toDomainHeatReading()
  }

  suspend fun deleteHeatReadingsByMeter(teplomerId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteHeatReadingsByMeter(teplomerId)
    }
  }

  suspend fun deleteHeatReadingByPokId(pokId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteHeatReadingByPokId(pokId)
    }
  }

  suspend fun deleteAllHeatReadings() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllHeatReadings()
    }
  }

  suspend fun insertWaterMeter(waterMeters: List<WaterMeterEntity>) {
    ensureSchema()
    dbQueries.transaction {
      waterMeters.forEach { meter ->
        dbQueries.insertWaterMeter(meter.toDbWaterMeter())
      }
    }
  }

  suspend fun getWaterMetersByApartment(addressId: Long): List<WaterMeterEntity> {
    ensureSchema()
    return dbQueries.getWaterMetersByApartment(addressId)
      .awaitAsList()
      .map { it.toDomainWaterMeter() }
  }

  suspend fun deleteWaterMetersByApartment(addressId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteWaterMetersByApartment(addressId)
    }
  }

  suspend fun deleteAllWaterMeters() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllWaterMeters()
    }
  }

  suspend fun insertWaterReadings(waterReadings: List<WaterReadingEntity>) {
    ensureSchema()
    dbQueries.transaction {
      waterReadings.forEach { reading ->
        dbQueries.insertWaterReading(reading.toDbWaterReading())
      }
    }
  }

  suspend fun getWaterReadingsByMeter(vodomerId: Long): List<WaterReadingEntity> {
    ensureSchema()
    return dbQueries.getWaterReadingsByMeter(vodomerId)
      .awaitAsList()
      .map { it.toDomainWaterReading() }
  }

  suspend fun getLastWaterReadingByMeter(pokId: Long): WaterReadingEntity? {
    ensureSchema()
    return dbQueries.getLastWaterReadingByMeter(pokId)
      .awaitAsOneOrNull()
      ?.toDomainWaterReading()
  }

  suspend fun deleteWaterReadingsByMeter(vodomerId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteWaterReadingsByMeter(vodomerId)
    }
  }

  suspend fun deleteWaterReadingByPokId(pokId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteWaterReadingByPokId(pokId)
    }
  }

  suspend fun deleteAllWaterReadings() {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteAllWaterReadings()
    }
  }

  suspend fun deleteWaterReadingByApartment(addressId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteWaterMetersByApartment(addressId)
    }
  }

  suspend fun deleteWaterReadingById(readingId: Long) {
    ensureSchema()
    dbQueries.transaction {
      dbQueries.deleteWaterReadingByPokId(readingId)
    }
  }
}
