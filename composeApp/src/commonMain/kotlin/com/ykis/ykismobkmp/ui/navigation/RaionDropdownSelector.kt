package com.ykis.ykismobkmp.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*

private const val className = "RaionDropdownSelector"

/**
 * [RaionDropdownSelector] — Кроссплатформенный выпадающий селектор выбора района города Южный.
 * ИСПРАВЛЕНО: menuAnchor обновлен до актуального KMP-стандарта с использованием ExposedDropdownMenuAnchorType.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaionDropdownSelector(
  modifier: Modifier = Modifier,
  raions: List<RaionEntity>,
  onRaionSelected: (RaionEntity) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  // Извлекаем строку по умолчанию из кроссплатформенных ресурсов JetBrains Res
  val defaultPlaceholder = stringResource(Res.string.choose_raion)
  var selectedName by remember(defaultPlaceholder) { mutableStateOf(defaultPlaceholder) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    OutlinedTextField(
      value = selectedName,
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(Res.string.city_raion)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      // ИСПРАВЛЕНО: Применен современный, актуальный КМР-синтаксис menuAnchor
      // с передачей правильного перечисления ExposedDropdownMenuAnchorType.PrimaryNotEditable
      modifier = Modifier
        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
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
            // Логирование согласно правилу [Класс.Метод] через КМР-команду println
            println("[$className.RaionDropdownSelector]: Клієнт обрав район биллинга ЮКИС: ${raion.raion}")
            selectedName = raion.raion
            expanded = false
            onRaionSelected(raion)
          }
        )
      }
    }
  }
}
