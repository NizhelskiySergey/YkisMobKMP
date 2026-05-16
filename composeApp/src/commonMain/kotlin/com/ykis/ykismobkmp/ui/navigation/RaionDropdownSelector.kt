package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "RaionDropdownSelector"

// Временная КМР-заглушка сущности района БТИ расчетного центра г. Южный
data class RaionEntity(
  val id: Long = 0L, // Сквозной КМР Long-стандарт
  val raion: String = ""
)

/**
 * [RaionDropdownSelector] — Кроссплатформенный выпадающий селектор выбора района города Южный.
 * ИСПРАВЛЕНО: Синтаксис menuAnchor обновлен до стандартов Compose Multiplatform, строки локализованы.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaionDropdownSelector(
  modifier: Modifier = Modifier,
  raions: List<RaionEntity>,
  onRaionSelected: (RaionEntity) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  // ИСПРАВЛЕНО: Хардкод-строка "Выберите район" переведена на КМР-ресурс Res.string
  val defaultPlaceholder = stringResource(Res.string.choose_raion)
  var selectedName by remember { mutableStateOf(defaultPlaceholder) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    OutlinedTextField(
      value = selectedName,
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(Res.string.city_raion)) }, // ИСПРАВЛЕНО: Вызов строки через Res.string
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      // ИСПРАВЛЕНО: Обновлен синтаксис menuAnchor для нередактируемых текстовых полей Material 3 KMP
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        .fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      )
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      raions.forEach { raion ->
        DropdownMenuItem(
          text = {
            Text(
              text = raion.raion,
              style = MaterialTheme.typography.bodyLarge
            )
          },
          onClick = {
            selectedName = raion.raion
            expanded = false
            onRaionSelected(raion)
          }
        )
      }
    }
  }
}

