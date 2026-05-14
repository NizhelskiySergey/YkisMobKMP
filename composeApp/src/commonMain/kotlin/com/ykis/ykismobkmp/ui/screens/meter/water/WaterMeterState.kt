package com.ykis.ykismobkmp.ui.screens.meter.water

// КРИТИЧЕСКИЙ ФИКС: Импортируем наши очищенные и типизированные под Long КМР-сущности
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity

/**
 * [WaterMeterState] — Кроссплатформенная модель состояния слоя отображения (UI State) для водомеров.
 * Полностью синхронизирована с реактивными потоками StateFlow в MeterScreenModel.
 */
data class WaterMeterState(
  val waterMeterList: List<WaterMeterEntity> = emptyList(),
  val waterReadings: List<WaterReadingEntity> = emptyList(),
  val selectedWaterMeter: WaterMeterEntity = WaterMeterEntity(),
  val lastWaterReading: WaterReadingEntity = WaterReadingEntity(),
  val newWaterReading: String = "",
  val isReadingError: Boolean = false,
  val isMetersLoading: Boolean = false,
  val isReadingsLoading: Boolean = false,
  val isLastReadingLoading: Boolean = false,
  val error: String? = null
)
