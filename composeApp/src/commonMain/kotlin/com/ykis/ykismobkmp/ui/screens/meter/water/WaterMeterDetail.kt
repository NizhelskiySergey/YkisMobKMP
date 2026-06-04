package com.ykis.ykismobkmp.ui.screens.meter.water


import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import com.ykis.ykismobkmp.core.utils.CenteredProgressIndicator
import com.ykis.ykismobkmp.core.utils.DateTimeUtils
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.LabelTextWithCheckBox
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.screens.meter.AddReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.DeleteReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.LastReadingCardButtons

private const val tag = "WaterMeterDetail"
@Composable
fun WaterMeterDetail(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  waterMeterEntity: WaterMeterEntity,
  lastReading: WaterReadingEntity?, // ИСПРАВЛЕНО: Изменено на Nullable под стандарты КМР-стейтов
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
  val safeLastReading = remember(lastReading) {
    lastReading ?: WaterReadingEntity(current = 0L)
  }
  val enabledButton by remember(newWaterReading, safeLastReading.current) {
    derivedStateOf {
      val newValue = newWaterReading.toLongOrNull() ?: -1L
      val isValid = newValue >= safeLastReading.current // ИСПРАВЛЕНО: Разрешено равенство (нулевое потребление)
      if (newWaterReading.isNotEmpty() && !isValid) {
        println("[$tag.Validation]: Значення $newValue менше за попередній якір ${safeLastReading.current}")
      }
      isValid
    }
  }
  LaunchedEffect(baseUIState.addressId, waterMeterEntity.vodomerId) {
    if (isWorking && waterMeterEntity.vodomerId != 0L) {
      println("[$tag.LaunchedEffect]: Оновлення показань для водоміра ID Long: ${waterMeterEntity.vodomerId}")
      getLastReading()
    }
  }
  Crossfade(
    targetState = isLastReadingLoading,
    label = "WaterDetailLoadingFade",
    animationSpec = tween(durationMillis = 300, delayMillis = 100)
  ) { isLoading ->
    if (isLoading) {
      CenteredProgressIndicator()
    } else {
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
              .clickable {
                println("[$tag.Navigation]: Перехід до стрічки історії")
                navigateToReadings()
              },
            label = "Останні показання"
          ) {
            WaterReadingItemContent(reading = safeLastReading)
          }
          val isDeleteEnabled = remember(safeLastReading.dateIn) {
            DateTimeUtils.isWithinOneHour(safeLastReading.dateIn)
          }

          LastReadingCardButtons(
            onAddButtonClick = {
              println("[$tag.Action]: Відкриття діалогу додавання")
              showAddReadingDialog = true
            },
            onDeleteButtonClick = {
              println("[$tag.Action]: Відкриття діалогу видалення")
              showDeleteReadingDialog = true
            },
            showDeleteButton = true,
            isDeleteEnabled = isDeleteEnabled
          )
        }
        BaseCard(
          modifier = Modifier.padding(vertical = 4.dp),
          label = "Технічні характеристики приладу"
        ) {
          LabelTextWithText(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Модель водоміра: ",
            valueText = waterMeterEntity.model
          )
          LabelTextWithText(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Заводський номер: ",
            valueText = waterMeterEntity.nomer
          )
          LabelTextWithText(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Місце встановлення: ",
            valueText = waterMeterEntity.place
          )
          LabelTextWithText(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Позиція у вузлі: ",
            valueText = waterMeterEntity.position
          )
          LabelTextWithCheckBox(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Враховувати стоки (Водовідведення): ",
            checked = waterMeterEntity.st == 1L
          )
          LabelTextWithCheckBox(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Загальнобудинковий лічильник: ",
            checked = waterMeterEntity.avg == 1L
          )
          LabelTextWithText(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Дата пломбування держповірником: ",
            valueText = waterMeterEntity.zdate
          )
          LabelTextWithText(
            modifier = Modifier.padding(vertical = 2.dp),
            labelText = "Дата початкового монтажу: ",
            valueText = waterMeterEntity.sdate
          )
          if (waterMeterEntity.spisan == 1L) {
            LabelTextWithText(
              modifier = Modifier.padding(vertical = 2.dp),
              labelText = "Дата зняття з обліку / списання: ",
              valueText = waterMeterEntity.dataSpis
            )
          }
        }
        if (isWorking) {
          BaseCard(
            modifier = Modifier.padding(vertical = 4.dp),
            label = "Державна повірка приладу"
          ) {
            LabelTextWithText(
              modifier = Modifier.padding(vertical = 2.dp),
              labelText = "Дата наступної повірки: ",
              valueText = waterMeterEntity.pdate
            )
            LabelTextWithText(
              modifier = Modifier.padding(vertical = 2.dp),
              labelText = "Дата останньої повірки: ",
              valueText = waterMeterEntity.fpdate
            )
            LabelTextWithCheckBox(
              modifier = Modifier.padding(vertical = 2.dp),
              labelText = "Прилад знято з комерційного обліку: ",
              checked = waterMeterEntity.spisan == 1L
            )
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
  if (showAddReadingDialog) {
    AddReadingDialog(
      onDismissRequest = {
        showAddReadingDialog = false
        onNewReadingChange("")
      },
      onAddClick = {
        println("[$tag.Submit]: Надсилання нових кубометрів на сервер: $newWaterReading")
        addReading()
        showAddReadingDialog = false
      },
      currentReading = safeLastReading.current.toString(),
      newReading = newWaterReading,
      onReadingChange = onNewReadingChange,
      enabledButton = enabledButton,
      isInteger = true // ИСПРАВЛЕНО: Для водоснабжения кубы всегда целые
    )
  }
  if (showDeleteReadingDialog) {
    DeleteReadingDialog(
      onDismissRequest = { showDeleteReadingDialog = false },
      onDeleteClick = {
        println("[$tag.Submit]: Скасування останнього введеного показання води")
        deleteReading()
        showDeleteReadingDialog = false
      }
    )
  }
}

