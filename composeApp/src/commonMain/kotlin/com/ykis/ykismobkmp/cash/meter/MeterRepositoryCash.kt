package com.ykis.ykismobkmp.cash.meter

import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity

/**
 * [MeterRepositoryCash] — Главный КМР-контракт локального хранилища приборов учета ЮКИС.
 */
interface MeterRepositoryCash {

  // ==========================================
  // ЛОГИКА СЧЕТЧИКОВ И ПОКАЗАНИЙ ВОДЫ
  // ==========================================
  suspend fun insertWaterMeter(waterMeters: List<WaterMeterEntity>)
  suspend fun getWaterMetersByApartment(addressId: Long): List<WaterMeterEntity>
  suspend fun deleteWaterMetersByApartment(addressId: Long)
  suspend fun deleteAllWaterMeters()

  suspend fun insertWaterReadings(waterReadings: List<WaterReadingEntity>)
  suspend fun getWaterReadingsByMeter(vodomerId: Long): List<WaterReadingEntity>
  suspend fun getLastWaterReadingByMeter(pokId: Long): WaterReadingEntity?
  suspend fun deleteWaterReadingsByMeter(vodomerId: Long)
  suspend fun deleteWaterReadingByPokId(pokId: Long)
  suspend fun deleteAllWaterReadings()

  // ==========================================
  // ЛОГИКА СЧЕТЧИКОВ И ПОКАЗАНИЙ ТЕПЛА
  // ==========================================
  suspend fun insertHeatMeter(heatMeters: List<HeatMeterEntity>)
  suspend fun getHeatMetersByApartment(addressId: Long): List<HeatMeterEntity>
  suspend fun deleteHeatMetersByApartment(addressId: Long)
  suspend fun deleteAllHeatMeters()

  suspend fun insertHeatReadings(heatReadings: List<HeatReadingEntity>)
  suspend fun getHeatReadingsByMeter(teplomerId: Long): List<HeatReadingEntity>
  suspend fun getLastHeatReadingByMeter(pokId: Long): HeatReadingEntity?
  suspend fun deleteHeatReadingsByMeter(teplomerId: Long)
  suspend fun deleteHeatReadingByPokId(pokId: Long)
  suspend fun deleteAllHeatReadings()
}
