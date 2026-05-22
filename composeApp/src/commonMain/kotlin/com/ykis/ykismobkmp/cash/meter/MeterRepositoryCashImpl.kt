package com.ykis.ykismobkmp.cash.meter

import com.ykis.ykismobkmp.cash.sqlDelight.MeterDao
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity

/**
 * [MeterRepositoryCashImpl] — Чистая КМР-реализация кэша приборов учета через единый монолитный MeterDao.
 */
class MeterRepositoryCashImpl(
  private val meterDao: MeterDao
) : MeterRepositoryCash {

  // ==========================================
  // ЛОГИКА СЧЕТЧИКОВ И ПОКАЗАНИЙ ВОДЫ
  // ==========================================

  override suspend fun insertWaterMeter(waterMeters: List<WaterMeterEntity>) {
    meterDao.insertWaterMeter(waterMeters)
  }

  override suspend fun getWaterMetersByApartment(addressId: Long): List<WaterMeterEntity> {
    return meterDao.getWaterMetersByApartment(addressId)
  }

  override suspend fun deleteWaterMetersByApartment(addressId: Long) {
    meterDao.deleteWaterMetersByApartment(addressId)
  }

  override suspend fun deleteAllWaterMeters() {
    meterDao.deleteAllWaterMeters()
  }

  override suspend fun insertWaterReadings(waterReadings: List<WaterReadingEntity>) {
    meterDao.insertWaterReadings(waterReadings)
  }

  override suspend fun getWaterReadingsByMeter(vodomerId: Long): List<WaterReadingEntity> {
    return meterDao.getWaterReadingsByMeter(vodomerId)
  }

  override suspend fun getLastWaterReadingByMeter(pokId: Long): WaterReadingEntity? {
    return meterDao.getLastWaterReadingByMeter(pokId)
  }

  override suspend fun deleteWaterReadingsByMeter(vodomerId: Long) {
    meterDao.deleteWaterReadingsByMeter(vodomerId)
  }

  override suspend fun deleteWaterReadingByPokId(pokId: Long) {
    meterDao.deleteWaterReadingByPokId(pokId)
  }

  override suspend fun deleteAllWaterReadings() {
    meterDao.deleteAllWaterReadings()
  }

  // ==========================================
  // ЛОГИКА СЧЕТЧИКОВ И ПОКАЗАНИЙ ТЕПЛА
  // ==========================================

  override suspend fun insertHeatMeter(heatMeters: List<HeatMeterEntity>) {
    meterDao.insertHeatMeter(heatMeters)
  }

  override suspend fun getHeatMetersByApartment(addressId: Long): List<HeatMeterEntity> {
    return meterDao.getHeatMetersByApartment(addressId)
  }

  override suspend fun deleteHeatMetersByApartment(addressId: Long) {
    meterDao.deleteHeatMetersByApartment(addressId)
  }

  override suspend fun deleteAllHeatMeters() {
    meterDao.deleteAllHeatMeters()
  }

  override suspend fun insertHeatReadings(heatReadings: List<HeatReadingEntity>) {
    meterDao.insertHeatReadings(heatReadings)
  }

  override suspend fun getHeatReadingsByMeter(teplomerId: Long): List<HeatReadingEntity> {
    return meterDao.getHeatReadingsByMeter(teplomerId)
  }

  override suspend fun getLastHeatReadingByMeter(pokId: Long): HeatReadingEntity? {
    return meterDao.getLastHeatReadingByMeter(pokId)
  }

  override suspend fun deleteHeatReadingsByMeter(teplomerId: Long) {
    meterDao.deleteHeatReadingsByMeter(teplomerId)
  }

  override suspend fun deleteHeatReadingByPokId(pokId: Long) {
    meterDao.deleteHeatReadingByPokId(pokId)
  }

  override suspend fun deleteAllHeatReadings() {
    meterDao.deleteAllHeatReadings()
  }
}
