package com.ykis.ykismobkmp.domain.repository.meter

import com.ykis.ykismobkmp.domain.repository.meter.useCase.AddHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.AddWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.DeleteLastHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.DeleteLastWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetHeatMeterList
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetHeatReadings
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetLastHeatReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetLastWaterReading
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetWaterMeterList
import com.ykis.ykismobkmp.domain.repository.meter.useCase.GetWaterReadings

/**
 * [MeterService] — Монолитный доменный сервис-комбайн учета коммунальных ресурсов ЮКИС.
 */
class MeterService(
  val getWaterMeterList: GetWaterMeterList,
  val getWaterReadings: GetWaterReadings,
  val getLastWaterReading: GetLastWaterReading,
  val addWaterReading: AddWaterReading,
  val deleteLastWaterReading: DeleteLastWaterReading,

  val getHeatMeterList: GetHeatMeterList,
  val getHeatReadings: GetHeatReadings,
  val getLastHeatReading: GetLastHeatReading,
  val addHeatReading: AddHeatReading,
  val deleteLastHeatReading: DeleteLastHeatReading
)
