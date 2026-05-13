package com.ykis.ykismobkmp.ui.screens.meter.heat

import com.ykis.mob.domain.meter.heat.meter.HeatMeterEntity
import com.ykis.mob.domain.meter.heat.reading.HeatReadingEntity

data class HeatMeterState(
    val heatMeterList: List<HeatMeterEntity> = emptyList(),
    val selectedHeatMeter : HeatMeterEntity = HeatMeterEntity(),
    val heatReadings : List<HeatReadingEntity> = emptyList(),
    val lastHeatReading : HeatReadingEntity = HeatReadingEntity(),
    val isMetersLoading:Boolean = true,
    val isLastReadingLoading : Boolean = false,
    val isReadingsLoading:Boolean = true,
    val newHeatReading : String = "" ,
    val error : String = ""
)
