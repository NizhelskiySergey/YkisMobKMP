package com.ykis.ykismobkmp.ui.screens.meter.heat
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "HeatMeterState"

/**
 * [HeatMeterState] — Кроссплатформенная модель состояния слоя отображения (UI State) для счетчиков тепла г. Южный.
 * для стопроцентной синхронизации с валидатором AddReadingDialog в HeatMeterDetail.kt.
 */
@Serializable
data class HeatMeterState(
  @SerialName("heatMeterList")
  val heatMeterList: List<HeatMeterEntity> = emptyList(),

  @SerialName("selectedHeatMeter")
  val selectedHeatMeter: HeatMeterEntity = HeatMeterEntity(),

  @SerialName("heatReadings")
  val heatReadings: List<HeatReadingEntity> = emptyList(),

  // ИСПРАВЛЕНО: Приведено к Nullable типу под архитектурный стандарт safeLastReading
  @SerialName("lastHeatReading")
  val lastHeatReading: HeatReadingEntity? = null,

  @SerialName("isMetersLoading")
  val isMetersLoading: Boolean = true,

  @SerialName("isLastReadingLoading")
  val isLastReadingLoading: Boolean = false,

  @SerialName("isReadingsLoading")
  val isReadingsLoading: Boolean = true,

  @SerialName("newHeatReading")
  val newHeatReading: String = "",

  @SerialName("error")
  val error: String? = null
) {
  init {
    // Логирование создания снимка состояния по правилу [Класс.Метод] через КМР-команду println()
    println("[$className.init]: Снімок стану опалення оновлено. Знайдено лічильників: ${heatMeterList.size}, Поточне введення: $newHeatReading")
  }
}

