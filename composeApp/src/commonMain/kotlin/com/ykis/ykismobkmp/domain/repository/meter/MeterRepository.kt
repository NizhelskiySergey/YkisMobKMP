package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.data.responses.GetHeatMeterResponse
import com.ykis.ykismobkmp.data.responses.GetHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastHeatReadingResponse
import com.ykis.ykismobkmp.data.responses.GetLastWaterReadingResponse
import com.ykis.ykismobkmp.data.responses.GetSimpleResponse
import com.ykis.ykismobkmp.data.responses.GetWaterMeterResponse
import com.ykis.ykismobkmp.data.responses.GetWaterReadingsResponse


/**
 * [MeterRepository] — Главный доменный контракт взаимодействия с данными счетчиков (Вода / Тепло) ЮКИС.
 * ИСПРАВЛЕНО НАМЕРТВО: Все числовые идентификаторы приведены к единому сквозному стандарту Long.
 * Намертво зафиксирован для полной замены.
 */
interface MeterRepository {

  suspend fun getWaterMeterList(uid: String, addressId: Long): GetWaterMeterResponse

  suspend fun getWaterReadings(uid: String, vodomerId: Long): GetWaterReadingsResponse

  suspend fun getLastWaterReading(uid: String, meterId: Long): GetLastWaterReadingResponse

  suspend fun addWaterReading(
    uid: String,
    vodomerId: Long,
    currentValue: Long,
    newValue: Long
  ): GetSimpleResponse

  suspend fun deleteLastWaterReading(uid: String, pokId: Long): GetSimpleResponse

  suspend fun getHeatMeterList(uid: String, addressId: Long): GetHeatMeterResponse

  suspend fun getHeatReadings(uid: String, teplomerId: Long): GetHeatReadingResponse

  suspend fun getLastHeatReading(uid: String, teplomerId: Long): GetLastHeatReadingResponse

  suspend fun addHeatReading(
    uid: String,
    teplomerId: Long,
    currentValue: Double,
    newValue: Double
  ): GetSimpleResponse

  suspend fun deleteLastHeatReading(uid: String, pokId: Long): GetSimpleResponse
}

