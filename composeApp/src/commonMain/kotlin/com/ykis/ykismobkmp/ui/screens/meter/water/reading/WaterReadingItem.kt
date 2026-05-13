package com.ykis.ykismobkmp.ui.screens.meter.water.reading

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ykis.mob.R
import com.ykis.mob.core.ext.isTrue
import com.ykis.mob.domain.meter.water.reading.WaterReadingEntity
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.mob.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme


@Composable
fun WaterReadingItem(reading : WaterReadingEntity) {
    BaseCard(
        label = if(reading.avg.isTrue()) stringResource(id = R.string.average) else null
    ) {
        WaterReadingItemContent(reading = reading)
    }
}

@Composable
fun WaterReadingItemContent(
    modifier: Modifier = Modifier,
    reading: WaterReadingEntity
) {
    if (reading.avg.isTrue()) {
                LabelTextWithText(
                    labelText = stringResource(id = R.string.last_colon),
                    valueText = reading.pokOt.toString()
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.current_colon),
                    valueText = reading.pokDo.toString()
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.qty_cube_colon),
                    valueText = reading.qtyKub.toString()
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.days_colon),
                    valueText = reading.rday.toString()
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.cube_day_colon),
                    valueText = reading.kubDay.toString()
                )
    }else {
        LabelTextWithText(
            labelText = stringResource(id = R.string.period_colon),
            valueText = stringResource(
                R.string.date_format,
                reading.dateOt,
                reading.dateDo
            )
        )
        LabelTextWithText(
            labelText = stringResource(id = R.string.days_colon),
            valueText = reading.days.toString()
        )
        Row {
            LabelTextWithText(
                modifier = modifier.weight(0.5f),
                labelText = stringResource(id = R.string.last_colon),
                valueText = reading.last.toString()
            )
            LabelTextWithText(
                modifier = modifier.weight(0.5f),
                labelText = stringResource(id = R.string.current_colon),
                valueText = reading.current.toString()
            )
        }

        LabelTextWithText(
            labelText = stringResource(id = R.string.cubs_colon),
            valueText = reading.kub.toString()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWaterReadingItem() {
    YkisPAMTheme {
        WaterReadingItem(
            reading = WaterReadingEntity(
                avg = 0
            )
        )
    }
}
