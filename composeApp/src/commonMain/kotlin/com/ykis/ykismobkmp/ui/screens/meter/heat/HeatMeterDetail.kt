package com.ykis.ykismobkmp.ui.screens.meter.heat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.mob.core.ext.isTrue
import com.ykis.mob.domain.meter.heat.meter.HeatMeterEntity
import com.ykis.mob.domain.meter.heat.reading.HeatReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.mob.ui.components.LabelTextWithCheckBox
import com.ykis.mob.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.screens.meter.AddReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.DeleteReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.LastReadingCardButtons
import com.ykis.ykismobkmp.ui.screens.meter.heat.reading.HeatReadingItemContent
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HeatMeterDetail(
    modifier: Modifier = Modifier,
    heatMeterEntity: HeatMeterEntity,
    baseUIState: BaseUIState,
    getLastHeatReading:()->Unit,
    lastHeatReading: HeatReadingEntity,
    onNewReadingChange: (String) -> Unit,
    newHeatReading: String,
    addReading: () -> Unit,
    deleteReading : ()->Unit,
    navigateToReadings: () -> Unit,
    isWorking : Boolean
) {
    var showAddReadingDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showDeleteReadingDialog by rememberSaveable {
        mutableStateOf(false)
    }
    val enabledButton by remember(key1 =newHeatReading ,key2 = lastHeatReading.current  ) {
        derivedStateOf {
            (newHeatReading.takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: -1.0) > lastHeatReading.current
        }
    }
    LaunchedEffect(key1 = baseUIState.addressId , key2 = heatMeterEntity.teplomerId ) {
        if(isWorking){
            getLastHeatReading()
        }
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        if(isWorking){
            BaseCard(
                cardModifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
                    .clip(CardDefaults.shape)
                    .clickable {
                        navigateToReadings()
                    },
                label = stringResource(id = R.string.last_reading)
            ) {
                HeatReadingItemContent(
                    reading = lastHeatReading,
                    isAverage = lastHeatReading.avg.isTrue()
                )
            }
            LastReadingCardButtons(
                onAddButtonClick = {
                    showAddReadingDialog=true
                },
                onDeleteButtonClick = {
                    showDeleteReadingDialog = true
                },
                showDeleteButton = (lastHeatReading.dateDo == SimpleDateFormat("dd.MM.yyyy").format(
                    Date()
                ))
            )
        }
        BaseCard(label = stringResource(id = R.string.meter_detail_text) ) {
            LabelTextWithText(
                labelText = stringResource(id = R.string.model_colon),
                valueText = heatMeterEntity.model
            )
            LabelTextWithText(
                labelText = stringResource(id = R.string.number_colon),
                valueText = heatMeterEntity.number
            )
            LabelTextWithText(
                labelText = stringResource(id = R.string.edizm_colon),
                valueText = heatMeterEntity.edizm
            )
            LabelTextWithText(
                labelText = stringResource(id = R.string.koef_colon),
                valueText = heatMeterEntity.koef
            )
            LabelTextWithText(
                labelText = stringResource(id = R.string.area_colon),
                valueText = heatMeterEntity.area.toString()
            )
            LabelTextWithCheckBox(
                labelText = stringResource(id = R.string.out_colon),
                checked = heatMeterEntity.out.isTrue()
            )
            LabelTextWithCheckBox(
                labelText = stringResource(id = R.string.spisan_colon),
                checked = heatMeterEntity.spisan.isTrue()
            )
        }
        if(isWorking){
            BaseCard(label = stringResource(id = R.string.check_water_meter)) {
                LabelTextWithText(
                    labelText = stringResource(id = R.string.pdate_colon),
                    valueText = heatMeterEntity.pdate
                )
                LabelTextWithText(
                    labelText = stringResource(id = R.string.fdate_colon),
                    valueText = heatMeterEntity.fpdate
                )
                LabelTextWithCheckBox(
                    labelText = stringResource(id = R.string.stop_colon),
                    checked = heatMeterEntity.spisan.isTrue()
                )
            }
        }

    }
    if (showAddReadingDialog) {
        AddReadingDialog(
            onDismissRequest = {
                showAddReadingDialog = false
                onNewReadingChange("")
            },
            onAddClick = addReading,
            currentReading = lastHeatReading.current.toString(),
            newReading = newHeatReading,
            onReadingChange = onNewReadingChange,
            enabledButton = enabledButton,
            isInteger = false
        )
    }
    if (showDeleteReadingDialog) {
        DeleteReadingDialog(
            onDismissRequest = { showDeleteReadingDialog = false },
            onDeleteClick = { deleteReading() }
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
private fun PreviewHeatMeterDetail() {
    YkisPAMTheme {
        HeatMeterDetail(
            heatMeterEntity = HeatMeterEntity(
            ),
            baseUIState = BaseUIState(),
            getLastHeatReading = {},
            lastHeatReading = HeatReadingEntity(
                avg = 1
            ),
            isWorking = true,
            navigateToReadings = {},
            onNewReadingChange = {},
            newHeatReading = "",
            addReading = {},
            deleteReading = {}
        )
    }
}
