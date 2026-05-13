package com.ykis.ykismobkmp.ui.screens.meter.water

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.mob.core.ext.isTrue
import com.ykis.mob.domain.meter.water.meter.WaterMeterEntity
import com.ykis.mob.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

@Composable
fun WaterMeterItem(
    modifier: Modifier = Modifier,
    waterMeter : WaterMeterEntity
) {
    val statusText :String
    val alpha: Float
    when {
        waterMeter.spisan.isTrue() ->{
            statusText = stringResource(R.string.written_off)
            alpha =0.4f
        }
        waterMeter.out.isTrue() -> {
            statusText = stringResource(R.string.on_the_test)
            alpha =0.4f
        }
        waterMeter.paused.isTrue() ->{
            statusText = stringResource(R.string.suspended)
            alpha =0.4f
        }
        else -> {
            statusText = stringResource(R.string.works)
            alpha = 1f
        }
    }
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(48.dp)),
    ) {
        Row(
            modifier = Modifier
                .alpha(alpha)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(64.dp),
                painter = painterResource(id = R.drawable.ic_water_meter9_24px),
                contentDescription = null
            )
            Column(modifier =  Modifier.weight(1f)) {
                Text(
                    text = waterMeter.model,
                    style = MaterialTheme.typography.titleLarge
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.number_colon),
                    valueText = waterMeter.nomer
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.place_colon),
                    valueText = waterMeter.place
                )
                Text(
                    text = statusText
                )

            }
            Icon(
                modifier = Modifier.padding(end = 24.dp),
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun PreviewWaterMeter() {
    YkisPAMTheme {
        WaterMeterItem(
            waterMeter = WaterMeterEntity(
                model = "GSD 8 METERS",
                nomer = "133932453245",
                place = "санвузол",
                work = 1,
            )
        )
    }

}
