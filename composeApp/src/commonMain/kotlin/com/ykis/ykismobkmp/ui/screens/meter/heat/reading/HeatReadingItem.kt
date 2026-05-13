package com.ykis.ykismobkmp.ui.screens.meter.heat.reading

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ykis.mob.R
import com.ykis.mob.domain.meter.heat.reading.HeatReadingEntity
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.mob.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme


@Composable
fun HeatReadingItem(
    reading: HeatReadingEntity,
    isAverage : Boolean
) {
    BaseCard(
        label = if(isAverage) stringResource(id = R.string.average) else null
    ) {
        HeatReadingItemContent(
            reading = reading,
            isAverage = isAverage
        )
    }
}


@Composable
fun HeatReadingItemContent(
    modifier: Modifier = Modifier,
    reading: HeatReadingEntity,
    isAverage: Boolean
) {
    if (isAverage) {
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
            valueText = reading.dayAvg
        )
        LabelTextWithText(
            labelText = stringResource(id = R.string.gkal_rasch_colon),
            valueText = reading.gkalRasch
        )
        LabelTextWithText(
            labelText = stringResource(id = R.string.gkal_day_colon),
            valueText = reading.gkalDay
        )
    } else {
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
            labelText = stringResource(id = R.string.qty_colon),
            valueText = reading.qty.toString()
        )
        LabelTextWithText(
            labelText = stringResource(id = R.string.gkal_colon),
            valueText = reading.gkal.toString()
        )
        LabelTextWithText(
            labelText = stringResource(id = R.string.rate_colon),
            valueText = reading.tarif.toString()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHeatReadingItem() {
    YkisPAMTheme {
            HeatReadingItem(
                reading = HeatReadingEntity(),
                isAverage = true
            )
    }
}
