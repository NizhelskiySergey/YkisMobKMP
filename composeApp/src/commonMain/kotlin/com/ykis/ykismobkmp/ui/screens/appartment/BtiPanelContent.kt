package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.*

private const val className = "BtiPanelContent"

@Composable
fun BtiPanelContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  viewModel: ApartmentScreenModel
) {
  val liveBtiState by viewModel.apartmentUiState.collectAsState()

  LaunchedEffect(baseUIState.addressId) {
    println("[$className.invoke]: Ініціалізація полей форми контактів біллінгу ЮКИС для адреси ID Long: ${baseUIState.addressId}")
    viewModel.initialContactState()
  }

  BtiContent(
    modifier = modifier,
    baseUIState = baseUIState, // Панелі площ читають стабільний snapshot
    currentEmail = liveBtiState.email ?: "",
    currentPhone = liveBtiState.phone ?: "",
    onEmailChange = viewModel::onEmailChange,
    onPhoneChange = viewModel::onPhoneChange,
    onUpdateBti = { typedPhone, typedEmail ->
      println("[YkisLogKMP.$className.onUpdateBti]: Користувач підтвердив збереження контактів. Тел: '$typedPhone', Email: '$typedEmail'")
      viewModel.onUpdateBti(phone = typedPhone, email = typedEmail)
    }
  )
}

@Composable
fun BtiContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  currentEmail: String,
  currentPhone: String,
  onEmailChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit,
  // ИСПРАВЛЕНО НАМЕРТВО: Сигнатура приведена к (String, String) -> Unit! Ошибка Mismatch полностью уничтожена!
  onUpdateBti: (String, String) -> Unit
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    BaseCard {
      ColumnLabelTextWithTextAndIcon(
        imageVector = Icons.Default.Person,
        labelText = stringResource(Res.string.employer_text_colon),
        valueText = baseUIState.apartment.nanim
      )
    }
    BaseCard(label = stringResource(Res.string.compound_text)) {
      Row(modifier = Modifier.fillMaxWidth()) {
        InfoItem(
          modifier = Modifier.weight(1f),
          label = stringResource(Res.string.tenant_text),
          value = baseUIState.apartment.tenant?.toString() ?: "0",
          icon = Icons.Default.Groups
        )
        InfoItem(
          modifier = Modifier.weight(1f),
          label = stringResource(Res.string.podnan_text),
          value = baseUIState.apartment.podnan?.toString() ?: "0"
        )
      }
      Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        InfoItem(
          modifier = Modifier.weight(1f),
          label = stringResource(Res.string.absent_text),
          value = baseUIState.apartment.absent?.toString() ?: "0"
        )
      }
    }
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
    BaseCard(label = stringResource(Res.string.data_bti)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        InfoItem(Modifier.weight(1f), stringResource(Res.string.rooms_colon), baseUIState.apartment.room?.toString() ?: "0")

        LabelTextWithCheckBox(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          labelText = stringResource(Res.string.private_text_colon),
          checked = baseUIState.apartment.privat == 1 || baseUIState.apartment.privat?.toString() == "1"
        )
        LabelTextWithCheckBox(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          labelText = stringResource(Res.string.elevator_colon),
          checked = baseUIState.apartment.lift == 1 || baseUIState.apartment.lift?.toString() == "1"
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

    // Передаем обновленную двухпараметрическую лямбду в ContactsCard
    ContactsCard(
      baseUIState = baseUIState,
      phone = currentPhone,
      email = currentEmail,
      onEmailChange = onEmailChange,
      onPhoneChange = onPhoneChange,
      onUpdateBti = onUpdateBti
    )
  }
}




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
