package com.ykis.ykismobkmp.ui.screens.meter.heat

// КРИТИЧЕСКИЙ ФИКС: Импортируем наши очищенные и типизированные под Long КМР-сущности счетчиков тепла
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity

/**
 * [HeatMeterState] — Кроссплатформенная модель состояния слоя отображения (UI State) для счетчиков тепла г. Южный.
 * Полностью синхронизирована со сквозной Long-типизацией Use Cases и `MeterScreenModel`.
 */
data class HeatMeterState(
  val heatMeterList: List<HeatMeterEntity> = emptyList(),
  val selectedHeatMeter: HeatMeterEntity = HeatMeterEntity(),
  val heatReadings: List<HeatReadingEntity> = emptyList(),
  val lastHeatReading: HeatReadingEntity = HeatReadingEntity(),
  val isMetersLoading: Boolean = true,
  val isLastReadingLoading: Boolean = false,
  val isReadingsLoading: Boolean = true,
  val newHeatReading: String = "",
  // ИСПРАВЛЕНО: Приведено к типу String? для удобного сброса ошибок наката биллинга
  val error: String? = null
)
