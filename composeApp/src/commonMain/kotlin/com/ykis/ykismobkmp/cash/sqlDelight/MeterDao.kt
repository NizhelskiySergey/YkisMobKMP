package com.ykis.ykismobkmp.cash.sqlDelight

import com.ykis.ykismobkmp.db.YkisDatabasesQueries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
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

/**
 * [MeterDao] — КМР-класс доступа к запросам SQLDelight.
 */
class MeterDao(
  private val dbQueries: YkisDatabasesQueries
) {
  suspend fun insertHeatMeter(heatMeters: List<HeatMeterEntity>) {
    dbQueries.transaction {
      heatMeters.forEach { meter ->
        dbQueries.insertHeatMeter(meter.toDbHeatMeter())
      }
    }
  }

  suspend fun getHeatMetersByApartment(addressId: Long): List<HeatMeterEntity> {
    return dbQueries.getHeatMetersByApartment(addressId)
      .awaitAsList()
      .map { it.toDomainHeatMeter() }
  }

  suspend fun deleteHeatMetersByApartment(addressId: Long) {
    dbQueries.transaction {
      dbQueries.deleteHeatMetersByApartment(addressId)
    }
  }

  suspend fun deleteAllHeatMeters() {
    dbQueries.deleteAllHeatMeters()
  }

  suspend fun insertHeatReadings(heatReadings: List<HeatReadingEntity>) {
    dbQueries.transaction {
      heatReadings.forEach { reading ->
        dbQueries.insertHeatReading(reading.toDbHeatReading())
      }
    }
  }

  suspend fun getHeatReadingsByMeter(teplomerId: Long): List<HeatReadingEntity> {
    return dbQueries.getHeatReadingsByMeter(teplomerId)
      .awaitAsList()
      .map { it.toDomainHeatReading() }
  }

  suspend fun getLastHeatReadingByMeter(pokId: Long): HeatReadingEntity? {
    return dbQueries.getLastHeatReadingByMeter(pokId)
      .awaitAsOneOrNull()
      ?.toDomainHeatReading()
  }

  suspend fun deleteHeatReadingsByMeter(teplomerId: Long) {
    dbQueries.deleteHeatReadingsByMeter(teplomerId)
  }

  suspend fun deleteHeatReadingByPokId(pokId: Long) {
    dbQueries.deleteHeatReadingByPokId(pokId)
  }

  suspend fun deleteAllHeatReadings() {
    dbQueries.deleteAllHeatReadings()
  }

  suspend fun insertWaterMeter(waterMeters: List<WaterMeterEntity>) {
    dbQueries.transaction {
      waterMeters.forEach { meter ->
        dbQueries.insertWaterMeter(meter.toDbWaterMeter())
      }
    }
  }

  suspend fun getWaterMetersByApartment(addressId: Long): List<WaterMeterEntity> {
    return dbQueries.getWaterMetersByApartment(addressId)
      .awaitAsList()
      .map { it.toDomainWaterMeter() }
  }

  suspend fun deleteWaterMetersByApartment(addressId: Long) {
    dbQueries.transaction {
      dbQueries.deleteWaterMetersByApartment(addressId)
    }
  }

  suspend fun deleteAllWaterMeters() {
    dbQueries.deleteAllWaterMeters()
  }

  suspend fun insertWaterReadings(waterReadings: List<WaterReadingEntity>) {
    dbQueries.transaction {
      waterReadings.forEach { reading ->
        dbQueries.insertWaterReading(reading.toDbWaterReading())
      }
    }
  }

  suspend fun getWaterReadingsByMeter(vodomerId: Long): List<WaterReadingEntity> {
    return dbQueries.getWaterReadingsByMeter(vodomerId)
      .awaitAsList()
      .map { it.toDomainWaterReading() }
  }

  suspend fun getLastWaterReadingByMeter(pokId: Long): WaterReadingEntity? {
    return dbQueries.getLastWaterReadingByMeter(pokId)
      .awaitAsOneOrNull()
      ?.toDomainWaterReading()
  }

  suspend fun deleteWaterReadingsByMeter(vodomerId: Long) {
    dbQueries.deleteWaterReadingsByMeter(vodomerId)
  }

  suspend fun deleteWaterReadingByPokId(pokId: Long) {
    dbQueries.deleteWaterReadingByPokId(pokId)
  }

  suspend fun deleteAllWaterReadings() {
    dbQueries.deleteAllWaterReadings()
  }

  suspend fun deleteWaterReadingByApartment(addressId: Long) {
    dbQueries.transaction {
      dbQueries.deleteWaterMetersByApartment(addressId)
    }
  }

  suspend fun deleteWaterReadingById(readingId: Long) {
    dbQueries.deleteWaterReadingByPokId(readingId)
  }
}
