package com.ykis.ykismobkmp.ui.screens.meter.heat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.mob.core.CenteredProgressIndicator
import com.ykis.mob.domain.meter.heat.meter.HeatMeterEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.mob.ui.components.EmptyListState
import com.ykis.ykismobkmp.ui.screens.meter.MeterViewModel

@Composable
fun HeatMeterList(
    modifier: Modifier = Modifier,
    viewModel: MeterViewModel,
    onHeatMeterClick : (HeatMeterEntity) -> Unit,
    baseUIState: BaseUIState,
    heatMeterState: HeatMeterState
) {
    LaunchedEffect(key1 = baseUIState.addressId) {
        viewModel.getHeatMeterList(baseUIState.uid!!,baseUIState.addressId)
    }
    Crossfade(
        targetState = heatMeterState.isMetersLoading,
        animationSpec = tween(delayMillis = 500), label = ""
    ) {
        isLoading->
            if(isLoading){
                CenteredProgressIndicator()
            }else  if(heatMeterState.heatMeterList.isEmpty()){
                EmptyListState(title = stringResource(R.string.no_meters) , subtitle = stringResource(R.string.no_heat_meters))
            }else {
                LazyColumn {
                    items(
                        heatMeterState.heatMeterList
                    ) { heatMeter ->
                        HeatMeterItem(
                            modifier = modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .clip(CardDefaults.outlinedShape)
                                .clickable {
                                    onHeatMeterClick(heatMeter)
                                },
                            heatMeter = heatMeter
                        )
                    }
                }
            }

    }
}
