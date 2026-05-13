package com.ykis.ykismobkmp.ui.screens.meter.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.ui.screens.apartment.BaseUIState
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import com.ykis.ykismobkmp.utils.isTrue
import com.ykis.ykismobkmp.utils.getCurrentDateString // Твой мультиплатформенный хелпер
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res
import android.util.Log
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.mob.ui.components.LabelTextWithCheckBox
import com.ykis.mob.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.screens.meter.AddReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.DeleteReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.LastReadingCardButtons
import com.ykis.ykismobkmp.ui.screens.meter.water.reading.WaterReadingItemContent

private const val className = "WaterMeterDetail"

@Composable
fun WaterMeterDetail(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  waterMeterEntity: WaterMeterEntity,
  lastReading: WaterReadingEntity,
  getLastReading: () -> Unit,
  onNewReadingChange: (String) -> Unit,
  newWaterReading: String,
  addReading: () -> Unit,
  deleteReading: () -> Unit,
  isWorking: Boolean,
  navigateToReadings: () -> Unit,
  isLastReadingLoading: Boolean
) {
  var showAddReadingDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteReadingDialog by rememberSaveable { mutableStateOf(false) }

  // --- ЛОГИКА ЯКОРЯ (800 < 877) ---
  val enabledButton by remember(newWaterReading, lastReading.current) {
    derivedStateOf {
      val newValue = newWaterReading.toIntOrNull() ?: -1
      val isValid = newValue > lastReading.current

      if (newWaterReading.isNotEmpty() && !isValid) {
        Log.w("YkisLog", "[$className.Validation]: Value $newValue is less than anchor ${lastReading.current}")
      }
      isValid
    }
  }

  LaunchedEffect(baseUIState.addressId, waterMeterEntity.vodomerId) {
    if (isWorking) {
      Log.d("YkisLog", "[$className.LaunchedEffect]: Fetching last reading for ${waterMeterEntity.vodomerId}")
      getLastReading()
    }
  }

  Crossfade(
    targetState = isLastReadingLoading,
    label = "WaterDetailLoadingFade",
    animationSpec = tween(delayMillis = 500)
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else {
      Column(
        modifier = modifier
          .verticalScroll(rememberScrollState())
          .fillMaxSize()
      ) {
        if (isWorking) {
          // Карточка последнего показания
          BaseCard(
            label = stringResource(Res.string.last_reading),
            cardModifier = Modifier
              .fillMaxWidth()
              .padding(8.dp)
              .clip(CardDefaults.shape)
              .clickable {
                Log.d("YkisLog", "[$className.Navigation]: To History")
                navigateToReadings()
              }
          ) {
            WaterReadingItemContent(reading = lastReading)
          }

          // Кнопки управления (Добавить / Удалить)
          LastReadingCardButtons(
            onAddButtonClick = {
              Log.d("YkisLog", "[$className.Action]: Show Add Dialog")
              showAddReadingDialog = true
            },
            onDeleteButtonClick = {
              Log.d("YkisLog", "[$className.Action]: Show Delete Dialog")
              showDeleteReadingDialog = true
            },
            // Сравнение дат через кроссплатформенный хелпер
            showDeleteButton = lastReading.dateDo == getCurrentDateString()
          )
        }

        // Карточка детальной информации БТИ
        BaseCard(label = stringResource(Res.string.meter_detail_text)) {
          LabelTextWithText(stringResource(Res.string.model_colon), waterMeterEntity.model)
          LabelTextWithText(stringResource(Res.string.number_colon), waterMeterEntity.nomer)
          LabelTextWithText(stringResource(Res.string.place_colon), waterMeterEntity.place)
          LabelTextWithText(stringResource(Res.string.position_colon), waterMeterEntity.position)

          LabelTextWithCheckBox(stringResource(Res.string.stoki_colon), waterMeterEntity.st.isTrue())
          LabelTextWithCheckBox(stringResource(Res.string.general_colon), waterMeterEntity.avg.isTrue())

          LabelTextWithText(stringResource(Res.string.zdate_colon), waterMeterEntity.zdate)
          LabelTextWithText(stringResource(Res.string.sdate_colon), waterMeterEntity.sdate)

          if (waterMeterEntity.spisan.isTrue()) {
            LabelTextWithText(stringResource(Res.string.date_spisan_colon), waterMeterEntity.dataSpis)
          }
        }

        if (isWorking) {
          // Карточка поверки
          BaseCard(label = stringResource(Res.string.check_water_meter)) {
            LabelTextWithText(stringResource(Res.string.pdate_colon), waterMeterEntity.pdate)
            LabelTextWithText(stringResource(Res.string.fdate_colon), waterMeterEntity.fpdate)
            LabelTextWithCheckBox(stringResource(Res.string.stop_colon), waterMeterEntity.spisan.isTrue())
          }
        }
      }
    }
  }

  // --- ДИАЛОГИ ---
  if (showAddReadingDialog) {
    AddReadingDialog(
      onDismissRequest = {
        showAddReadingDialog = false
        onNewReadingChange("")
      },
      onAddClick = {
        Log.i("YkisLog", "[$className.Submit]: Adding value $newWaterReading")
        addReading()
        showAddReadingDialog = false
      },
      currentReading = lastReading.current.toString(),
      newReading = newWaterReading,
      onReadingChange = onNewReadingChange,
      enabledButton = enabledButton, // Валидация передается сюда
      isInteger = true
    )
  }

  if (showDeleteReadingDialog) {
    DeleteReadingDialog(
      onDismissRequest = { showDeleteReadingDialog = false },
      onDeleteClick = {
        Log.w("YkisLog", "[$className.Submit]: Deleting value")
        deleteReading()
        showDeleteReadingDialog = false
      }
    )
  }
}

@Preview
@Composable
private fun PreviewWaterMeterDetail() {
  YkisPAMTheme {
    WaterMeterDetail(
      baseUIState = BaseUIState(),
      waterMeterEntity = WaterMeterEntity(model = "GLS 3 ULTRA"),
      lastReading = WaterReadingEntity(current = 877),
      getLastReading = {},
      isWorking = true,
      isLastReadingLoading = false,
      onNewReadingChange = {},
      newWaterReading = "800",
      addReading = {},
      deleteReading = {},
      navigateToReadings = {}
    )
  }
}
