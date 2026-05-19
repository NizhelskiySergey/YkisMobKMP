package com.ykis.ykismobkmp.ui.screens.bti
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.ColumnLabelTextWithTextAndIcon
import com.ykis.ykismobkmp.ui.components.LabelTextWithCheckBox
import com.ykis.ykismobkmp.ui.components.LabelTextWithText
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.absent_text
import ykismobkmp.composeapp.generated.resources.area_extra
import ykismobkmp.composeapp.generated.resources.area_flat
import ykismobkmp.composeapp.generated.resources.area_full
import ykismobkmp.composeapp.generated.resources.area_life
import ykismobkmp.composeapp.generated.resources.area_otopl
import ykismobkmp.composeapp.generated.resources.compound_text
import ykismobkmp.composeapp.generated.resources.data_bti
import ykismobkmp.composeapp.generated.resources.date_orde_colon
import ykismobkmp.composeapp.generated.resources.elevator_colon
import ykismobkmp.composeapp.generated.resources.employer_text_colon
import ykismobkmp.composeapp.generated.resources.order_text
import ykismobkmp.composeapp.generated.resources.podnan_text
import ykismobkmp.composeapp.generated.resources.private_text_colon
import ykismobkmp.composeapp.generated.resources.rooms_colon
import ykismobkmp.composeapp.generated.resources.secret_сode
import ykismobkmp.composeapp.generated.resources.tenant_text

// Временные КМР-заглушки специфических UI-карточек и чекбоксов ЮКИС г. Южный

private const val className = "BtiPanelContent"

/**
 * [BtiPanelContent] — Кроссплатформенный Stateful-контейнер панели характеристик БТИ.
 * ИСПРАВЛЕНО: Сигнатура согласована с BtiTab, повторный get/koinInject удален во избежание дублирования сессий в ОЗУ.
 */
@Composable
fun BtiPanelContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  viewModel: ApartmentScreenModel // ИСПРАВЛЕНО: Принимаем заинжекченную ScreenModel напрямую из вызывающего контекста вкладок
) {
  // Реактивно подписываемся на локальный поток контактов БТИ формы изменения из переданного инстанса
  val contactUiState by viewModel.contactUIState.collectAsState()

  // Инициализируем стартовые текстовые поля при смене активного лицевого счета
  LaunchedEffect(baseUIState.addressId) {
    println("[$className.invoke]: Ініціалізація полей форми контактів біллінгу ЮКИС для адреси ID Long: ${baseUIState.addressId}")
    viewModel.initialContactState()
  }

  BtiContent(
    modifier = modifier,
    baseUIState = baseUIState,
    contactUiState = contactUiState,
    onEmailChange = viewModel::onEmailChange,
    onPhoneChange = viewModel::onPhoneChange,
    onUpdateBti = {
      baseUIState.uid?.let { currentUid ->
        viewModel.onUpdateBti(currentUid)
      }
    }
  )
}

/**
 * [BtiContent] — Чистая Stateless-верстка характеристик квартиры и площадей Material 3.
 */
@Composable
fun BtiContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  contactUiState: Any?, // Заменено на Any?, пока не прислана структура ContactUiState
  onEmailChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit,
  onUpdateBti: () -> Unit
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Карточка Ответственного (Нанимателя / Собственника БТИ)
    BaseCard {
      ColumnLabelTextWithTextAndIcon(
        imageVector = Icons.Default.Person,
        labelText = stringResource(Res.string.employer_text_colon),
        valueText = baseUIState.apartment.nanim
      )
    }

    // 2. Карточка состава семьи и зарегистрированных жильцов г. Южный
    BaseCard(label = stringResource(Res.string.compound_text)) {
      Row(modifier = Modifier.fillMaxWidth()) {
        InfoItem(
          modifier = Modifier.weight(1f),
          label = stringResource(Res.string.tenant_text),
          value = baseUIState.apartment.tenant.toString(),
          icon = Icons.Default.Groups
        )
        InfoItem(
          modifier = Modifier.weight(1f),
          label = stringResource(Res.string.podnan_text),
          value = baseUIState.apartment.podnan.toString()
        )
      }
      Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        InfoItem(
          modifier = Modifier.weight(1f),
          label = stringResource(Res.string.absent_text),
          value = baseUIState.apartment.absent.toString()
        )
      }
    }

    // 3. Расчетные площади БТИ (Акцент на цифрах для биллинга коммунальных услуг)
    BaseCard(label = stringResource(Res.string.area_flat)) {
      Row(modifier = Modifier.fillMaxWidth()) {
        InfoItem(Modifier.weight(1f), stringResource(Res.string.area_full), baseUIState.apartment.areaFull.toString())
        InfoItem(Modifier.weight(1f), stringResource(Res.string.area_life), baseUIState.apartment.areaLife.toString())
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
      Row(modifier = Modifier.fillMaxWidth()) {
        InfoItem(Modifier.weight(1f), stringResource(Res.string.area_extra), baseUIState.apartment.areaDop.toString())
        InfoItem(Modifier.weight(1f), stringResource(Res.string.area_otopl), baseUIState.apartment.areaOtopl.toString())
      }
    }

    // 4. Технический паспорт, приватизация и архивные ордера
    BaseCard(label = stringResource(Res.string.data_bti)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        InfoItem(Modifier.weight(1f), stringResource(Res.string.rooms_colon), baseUIState.apartment.room.toString())

        LabelTextWithCheckBox(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          labelText = stringResource(Res.string.private_text_colon),
          checked = baseUIState.apartment.privat == 1
        )
        LabelTextWithCheckBox(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          labelText = stringResource(Res.string.elevator_colon),
          checked = baseUIState.apartment.lift == 1
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = stringResource(Res.string.secret_сode),
          valueText = baseUIState.apartment.kod
        )
      }

      Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = stringResource(Res.string.order_text),
          valueText = baseUIState.apartment.order
        )
        LabelTextWithText(
          modifier = Modifier.padding(vertical = 2.dp),
          labelText = stringResource(Res.string.date_orde_colon),
          valueText = baseUIState.apartment.dataOrder
        )
      }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

    // 5. Вложенная КМР-карточка вывода и изменения контактов абонента БТИ
    ContactsCard(
      baseUIState = baseUIState,
      phone = "", // Настрой вычитывание строк, когда пришлешь дата-класс ContactUiState
      email = "",
      onEmailChange = onEmailChange,
      onPhoneChange = onPhoneChange,
      onUpdateBti = onUpdateBti
    )
  }
}

/**
 * [InfoItem] — Вспомогательный КМР-компонент ячейки численных параметров площадей БТИ.
 */
@Composable
fun InfoItem(
  modifier: Modifier = Modifier,
  label: String,
  value: String,
  icon: ImageVector? = null
) {
  Column(modifier = modifier.padding(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp).padding(end = 4.dp),
          tint = MaterialTheme.colorScheme.primary
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

