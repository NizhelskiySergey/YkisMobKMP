package com.ykis.ykismobkmp.ui.screens.meter.water

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ykis.mob.core.CenteredProgressIndicator
import com.ykis.mob.domain.meter.water.meter.WaterMeterEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.meter.MeterViewModel

@Composable
fun WaterMeterList(
  modifier: Modifier = Modifier,
  viewModel : MeterViewModel,
  baseUIState: BaseUIState,
  waterMeterState: WaterMeterState,
  onWaterMeterClick : (WaterMeterEntity) ->Unit
) {

    LaunchedEffect(key1 = baseUIState.addressId) {
        viewModel.getWaterMeterList(baseUIState.uid!!,baseUIState.addressId)
    }
    Crossfade(
        targetState = waterMeterState.isMetersLoading, label = "",
        animationSpec = tween(delayMillis = 500)
    ) {
        isLoading->
            if(isLoading){
             CenteredProgressIndicator()
            }else LazyColumn(
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(
                    waterMeterState.waterMeterList
                ) { waterMeter ->
                    WaterMeterItem(
                        modifier = modifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .clip(CardDefaults.outlinedShape)
                            .clickable {
                                onWaterMeterClick(waterMeter)
                            },
                        waterMeter = waterMeter
                    )
                }
            }
    }
}
