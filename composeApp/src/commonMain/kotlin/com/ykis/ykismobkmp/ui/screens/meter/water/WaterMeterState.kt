package com.ykis.ykismobkmp.ui.screens.meter.water
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "WaterMeterState"

/**
 * [WaterMeterState] — Кроссплатформенная модель состояния слоя отображения (UI State) для водомеров.
 * ИСПРАВЛЕНО: Добавлены аннотации @Serializable, поле lastWaterReading переведено в nullable формат
 * для ликвидации Type mismatch конфликтов в MeterDetailContent.kt.
 */
@Serializable
data class WaterMeterState(
  @SerialName("waterMeterList")
  val waterMeterList: List<WaterMeterEntity> = emptyList(),

  @SerialName("waterReadings")
  val waterReadings: List<WaterReadingEntity> = emptyList(),

  @SerialName("selectedWaterMeter")
  val selectedWaterMeter: WaterMeterEntity = WaterMeterEntity(),

  // ИСПРАВЛЕНО: Приведено к Nullable типу под архитектурный стандарт safeLastReading в MeterDetailContent
  @SerialName("lastWaterReading")
  val lastWaterReading: WaterReadingEntity? = null,

  @SerialName("newWaterReading")
  val newWaterReading: String = "",

  @SerialName("isReadingError")
  val isReadingError: Boolean = false,

  @SerialName("isMetersLoading")
  val isMetersLoading: Boolean = false,

  @SerialName("isReadingsLoading")
  val isReadingsLoading: Boolean = false,

  @SerialName("isLastReadingLoading")
  val isLastReadingLoading: Boolean = false,

  @SerialName("error")
  val error: String? = null
) {
  init {
    // Логирование создания снимка состояния по правилу [Класс.Метод] через КМР-команду println()
    println("[$className.init]: Снімок стану водопостачання оновлено. Знайдено приладів: ${waterMeterList.size}, Введення кубів: $newWaterReading")
  }
}
