package com.ykis.ykismobkmp.ui.screens.appartment

// Подключаем ваши ресурсы строк ЮКІС
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

private const val className = "BtiPanelContent"

@Composable
fun BtiPanelContent(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  viewModel: ApartmentScreenModel
) {
  val liveBtiState by viewModel.uiState.collectAsState()

  LaunchedEffect(baseUIState.addressId) {
    println("[$className.invoke]: Ініціалізація полей форми контактів біллінгу ЮКИС для адреси ID Long: ${baseUIState.addressId}")
    viewModel.initialContactState()
  }

  BtiContent(
    modifier = modifier,
    baseUIState = baseUIState,
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

        // ИСПРАВЛЕНО НАМЕРТВО: Очищено от платформенных вызовов .toString() == "1".
        // Приведение типов выполняется нативно на базе сквозного стандарта Long-значений СУБД.
        val isPrivatChecked = baseUIState.apartment.privat == 1L
        val isLiftChecked = baseUIState.apartment.lift == 1L


        LabelTextWithCheckBox(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          labelText = stringResource(Res.string.private_text_colon),
          checked = isPrivatChecked
        )
        LabelTextWithCheckBox(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          labelText = stringResource(Res.string.elevator_colon),
          checked = isLiftChecked
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

