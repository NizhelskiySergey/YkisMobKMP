package com.ykis.ykismobkmp.ui.screens.meter.heat


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.core.utils.DateTimeUtils
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.LabelTextWithCheckBox
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.screens.meter.AddReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.DeleteReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.LastReadingCardButtons
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val tag = "HeatMeterDetail"

@Composable
fun HeatMeterDetail(
  modifier: Modifier = Modifier,
  heatMeterEntity: HeatMeterEntity,
  baseUIState: BaseUIState,
  getLastHeatReading: () -> Unit,
  lastHeatReading: HeatReadingEntity?, 
  onNewReadingChange: (String) -> Unit,
  newHeatReading: String,
  addReading: () -> Unit,
  deleteReading: () -> Unit,
  navigateToReadings: () -> Unit,
  isWorking: Boolean
) {
  var showAddReadingDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteReadingDialog by rememberSaveable { mutableStateOf(false) }

  val safeLastReading = remember(lastHeatReading) {
    lastHeatReading ?: HeatReadingEntity(current = 0.0, avg = 0L)
  }

  val enabledButton by remember(newHeatReading, safeLastReading.current) {
    derivedStateOf {
      val rawInput = newHeatReading.replace(',', '.')
      val newValue = rawInput.toDoubleOrNull() ?: -1.0
      val isValid = newValue >= safeLastReading.current && rawInput.lastOrNull() != '.'
      isValid
    }
  }

  val isErrorValue = remember(newHeatReading, safeLastReading.current) {
    derivedStateOf {
      val rawInput = newHeatReading.replace(',', '.')
      val newValue = rawInput.toDoubleOrNull() ?: -1.0
      newHeatReading.isNotEmpty() && newValue < safeLastReading.current && newValue != -1.0
    }
  }

  LaunchedEffect(baseUIState.addressId, heatMeterEntity.teplomerId) {
    if (isWorking && heatMeterEntity.teplomerId != 0L) {
      println("[$tag.LaunchedEffect]: Оновлення показань тепла для лічильника ID Long: ${heatMeterEntity.teplomerId}")
      getLastHeatReading()
    }
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
      .padding(horizontal = 8.dp)
  ) {
    if (isWorking) {
      BaseCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(CardDefaults.shape)
          .clickable { navigateToReadings() },
        label = stringResource(Res.string.last_reading_title)
      ) {
        HeatReadingItemContent(
          reading = safeLastReading,
          isAverage = safeLastReading.avg == 1L
        )
      }

      val isDeleteEnabled = remember(safeLastReading.dateIn) {
        DateTimeUtils.isWithinOneHour(safeLastReading.dateIn)
      }

      LastReadingCardButtons(
        onAddButtonClick = { showAddReadingDialog = true },
        onDeleteButtonClick = { showDeleteReadingDialog = true },
        showDeleteButton = true,
        isDeleteEnabled = isDeleteEnabled
      )
    }

    BaseCard(
      modifier = Modifier.padding(vertical = 4.dp),
      label = stringResource(Res.string.water_meter_features)
    ) {
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.heat_meter_model) + " ",
        valueText = heatMeterEntity.model
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.factory_number) + " ",
        valueText = heatMeterEntity.number
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.unit_measurement) + " ",
        valueText = heatMeterEntity.edizm
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.device_coefficient) + " ",
        valueText = heatMeterEntity.koef
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.heating_area) + " ",
        valueText = "${heatMeterEntity.area} m²"
      )
      LabelTextWithCheckBox(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.device_on_test) + " ",
        checked = heatMeterEntity.isOut == 1L
      )
      LabelTextWithCheckBox(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = stringResource(Res.string.off_commercial_accounting) + " ",
        checked = heatMeterEntity.spisan == 1L
      )
    }

    if (isWorking) {
      BaseCard(
        modifier = Modifier.padding(vertical = 4.dp),
        label = stringResource(Res.string.state_verification)
      ) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = stringResource(Res.string.next_check_date) + " ",
          valueText = heatMeterEntity.pdate
        )
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = stringResource(Res.string.last_check_date) + " ",
          valueText = heatMeterEntity.fpdate
        )
        LabelTextWithCheckBox(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = stringResource(Res.string.commercial_accounting_suspended) + " ",
          checked = heatMeterEntity.spisan == 1L
        )
      }
    }
    Spacer(modifier = Modifier.height(24.dp))
  }

  if (showAddReadingDialog) {
    AddReadingDialog(
      onDismissRequest = {
        showAddReadingDialog = false
        onNewReadingChange("")
      },
      onAddClick = {
        addReading()
        showAddReadingDialog = false
      },
      currentReading = safeLastReading.current.toString(),
      newReading = newHeatReading,
      onReadingChange = onNewReadingChange,
      enabledButton = enabledButton,
      isInteger = false, 
      isError = isErrorValue.value,
      errorMessage = "Значення має бути не менше ${safeLastReading.current}"
    )
  }

  if (showDeleteReadingDialog) {
    DeleteReadingDialog(
      onDismissRequest = { showDeleteReadingDialog = false },
      onDeleteClick = {
        deleteReading()
        showDeleteReadingDialog = false
      }
    )
  }
}

