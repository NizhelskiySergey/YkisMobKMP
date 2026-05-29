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
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.LabelTextWithCheckBox
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.screens.meter.AddReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.DeleteReadingDialog
import com.ykis.ykismobkmp.ui.screens.meter.LastReadingCardButtons

private const val tag = "HeatMeterDetail"

@Composable
fun HeatMeterDetail(
  modifier: Modifier = Modifier,
  heatMeterEntity: HeatMeterEntity,
  baseUIState: BaseUIState,
  getLastHeatReading: () -> Unit,
  lastHeatReading: HeatReadingEntity?, // ИСПРАВЛЕНО: Изменено на Nullable тип под стандарты KMP-стейтов
  onNewReadingChange: (String) -> Unit,
  newHeatReading: String,
  addReading: () -> Unit,
  deleteReading: () -> Unit,
  navigateToReadings: () -> Unit,
  isWorking: Boolean
) {
  var showAddReadingDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteReadingDialog by rememberSaveable { mutableStateOf(false) }

  // ИСПРАВЛЕНО: avg приведен к Long-стандарту (0L) во избежание конфликта типов
  val safeLastReading = remember(lastHeatReading) {
    lastHeatReading ?: HeatReadingEntity(current = 0.0, avg = 0L)
  }

  val enabledButton by remember(newHeatReading, safeLastReading.current) {
    derivedStateOf {
      val newValue = newHeatReading.replace(',', '.').toDoubleOrNull() ?: -1.0
      val isValid = newValue > safeLastReading.current
      if (newHeatReading.isNotEmpty() && !isValid) {
        println("[$tag.Validation]: Значення $newValue менше або дорівнює попередньому якорю ${safeLastReading.current}")
      }
      isValid
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
      // Карточка последних переданных гигакалорий в теплосеть г. Южного
      BaseCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(CardDefaults.shape)
          .clickable { navigateToReadings() },
        label = "Останні показання"
      ) {
        HeatReadingItemContent(
          reading = safeLastReading,
          isAverage = safeLastReading.avg == 1L
        )
      }
      LastReadingCardButtons(
        onAddButtonClick = { showAddReadingDialog = true },
        onDeleteButtonClick = { showDeleteReadingDialog = true },
        showDeleteButton = true
      )
    }

    BaseCard(
      modifier = Modifier.padding(vertical = 4.dp),
      label = "Технічні характеристики приладу"
    ) {
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Модель лічильника: ",
        valueText = heatMeterEntity.model
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Заводський номер: ",
        valueText = heatMeterEntity.number
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Одиниця виміру: ",
        valueText = heatMeterEntity.edizm
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Коефіцієнт приладу: ",
        valueText = heatMeterEntity.koef
      )
      LabelTextWithText(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Опалювальна площа: ",
        valueText = "${heatMeterEntity.area} м²"
      )
      LabelTextWithCheckBox(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Прилад знаходиться на повірці: ",
        checked = heatMeterEntity.isOut == 1L
      )
      LabelTextWithCheckBox(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Прилад знято з обліку / списано: ",
        checked = heatMeterEntity.spisan == 1L
      )
    }

    if (isWorking) {
      BaseCard(
        modifier = Modifier.padding(vertical = 4.dp),
        label = "Державна повірка приладу"
      ) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = "Дата наступної повірки: ",
          valueText = heatMeterEntity.pdate
        )
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = "Дата останньої повірки: ",
          valueText = heatMeterEntity.fpdate
        )
        LabelTextWithCheckBox(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = "Комерційний облік призупинено: ",
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
        // ИСПРАВЛЕНО: Добавлен триггер скрытия диалога при подтверждении
        addReading()
        showAddReadingDialog = false
      },
      currentReading = safeLastReading.current.toString(),
      newReading = newHeatReading,
      onReadingChange = onNewReadingChange,
      enabledButton = enabledButton,
      isInteger = false // Для тепла разрешен ввод дробной части через точку/запятую
    )
  }

  if (showDeleteReadingDialog) {
    DeleteReadingDialog(
      onDismissRequest = { showDeleteReadingDialog = false },
      onDeleteClick = {
        // ИСПРАВЛЕНО: Добавлен триггер скрытия диалога при удалении
        deleteReading()
        showDeleteReadingDialog = false
      }
    )
  }
}

