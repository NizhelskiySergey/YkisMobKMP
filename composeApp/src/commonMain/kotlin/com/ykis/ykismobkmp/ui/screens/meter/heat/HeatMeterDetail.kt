package com.ykis.ykismobkmp.ui.screens.meter.heat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
 * ИСПРАВЛЕНО: Типы lastHeatReading переведены в безопасный nullable формат HeatReadingEntity?
 * для ликвидации Type mismatch конфликтов со стейтом HeatMeterState.
 */
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

  // Безопасно распаковываем зануляемую доменную сущность показаний во избежание NullPointerException
  val safeLastReading = remember(lastHeatReading) { lastHeatReading ?: HeatReadingEntity(current = 0.0, avg = 0) }

  // Валидация якоря показаний (Double) — новое показание должно быть строго больше предыдущего
  val enabledButton by remember(newHeatReading, safeLastReading.current) {
    derivedStateOf {
      (newHeatReading.takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: -1.0) > safeLastReading.current
    }
  }

  // Каскадный триггер обновления прибора учета при смене лицевого счета СУБД SQLDelight
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
        // ИСПРАВЛЕНО: Передаем безопасный распакованный объект safeLastReading
        HeatReadingItemContent(
          reading = safeLastReading,
          isAverage = safeLastReading.avg == 1
        )
      }

      // Панель управляющих кнопок (Добавить/Удалить показание)
      LastReadingCardButtons(
        onAddButtonClick = { showAddReadingDialog = true },
        onDeleteButtonClick = { showDeleteReadingDialog = true },
        showDeleteButton = true
      )
    }

    // Карточка технического паспорта БТИ счетчика тепла г. Южный
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
        checked = heatMeterEntity.out_ == 1
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

  // --- КМР МОДАЛЬНЫЕ ОКНА ВВОДА ПОКАЗАНИЙ ---
  if (showAddReadingDialog) {
    AddReadingDialog(
      onDismissRequest = {
        showAddReadingDialog = false
        onNewReadingChange("")
      },
      onAddClick = addReading,
      currentReading = safeLastReading.current.toString(),
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
