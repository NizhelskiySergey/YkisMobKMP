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
import androidx.compose.ui.tooling.preview.Preview
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
import com.ykis.ykismobkmp.ui.screens.meter.heat.reading.HeatReadingItemContent
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val tag = "HeatMeterDetail"

/**
 * [HeatMeterDetail] — Кроссплатформенный Stateless-экран съема и детализации счетчиков тепла ЮКИС.
 * Полностью автономен, синхронизирован с сигнатурой BaseCard и готов к сборке на Mac Desktop.
 */
@Composable
fun HeatMeterDetail(
  modifier: Modifier = Modifier,
  heatMeterEntity: HeatMeterEntity,
  baseUIState: BaseUIState,
  getLastHeatReading: () -> Unit,
  lastHeatReading: HeatReadingEntity,
  onNewReadingChange: (String) -> Unit,
  newHeatReading: String,
  addReading: () -> Unit,
  deleteReading: () -> Unit,
  navigateToReadings: () -> Unit,
  isWorking: Boolean
) {
  var showAddReadingDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteReadingDialog by rememberSaveable { mutableStateOf(false) }

  // Валидация якоря показаний (Double)
  val enabledButton by remember(newHeatReading, lastHeatReading.current) {
    derivedStateOf {
      (newHeatReading.takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: -1.0) > lastHeatReading.current
    }
  }

  // Каскадный триггер обновления прибора учета
  LaunchedEffect(baseUIState.addressId, heatMeterEntity.teplomerId) {
    if (isWorking && heatMeterEntity.teplomerId != 0L) {
      println("[$tag.LaunchedEffect]: Оновлення показань тепла для лічильника: ${heatMeterEntity.teplomerId}")
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
      // Карточка последних переданных гигакалорий
      BaseCard(
        // ИСПРАВЛЕНО: Параметр cardModifier заменен на стандартный универсальный modifier
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(CardDefaults.shape)
          .clickable { navigateToReadings() },
        label = "Останні показання"
      ) {
        HeatReadingItemContent(
          reading = lastHeatReading,
          isAverage = lastHeatReading.avg == 1
        )
      }

      // Панель кнопок (Добавить/Удалить показание)
      LastReadingCardButtons(
        onAddButtonClick = { showAddReadingDialog = true },
        onDeleteButtonClick = { showDeleteReadingDialog = true },
        showDeleteButton = true
      )
    }

    // Карточка технического паспорта БТИ счетчика тепла г. Южный
    // Карточка технического паспорта БТИ счетчика тепла г. Южный
    BaseCard(
      modifier = Modifier.padding(vertical = 4.dp),
      label = "Технічні характеристики приладу"
    ) {
      // ИСПРАВЛЕНО: Каждому компоненту добавлен модификатор вертикального отступа для идеальной сетки UI
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

      // ИСПРАВЛЕНО: Информационные чекбоксы Read-Only также разделены адаптивными отступами
      LabelTextWithCheckBox(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Прилад знаходиться на повірці: ",
        checked = heatMeterEntity.out == 1
      )
      LabelTextWithCheckBox(
        modifier = Modifier.padding(vertical = 2.dp),
        labelText = "Прилад знято з обліку / списано: ",
        checked = heatMeterEntity.spisan == 1
      )
    }


    if (isWorking) {
      // Картка державної повірки приладу обліку тепла м. Южне
      BaseCard(
        modifier = Modifier.padding(vertical = 4.dp),
        label = "Державна повірка приладу"
      ) {
        // ИСПРАВЛЕНО: Каждой строке передан явный модификатор отступа для симметрии с БТИ блоком
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
          checked = heatMeterEntity.spisan == 1
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // --- КМР МОДАЛЬНЫЕ ОКНА УПРАВЛЕНИЯ ---
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
