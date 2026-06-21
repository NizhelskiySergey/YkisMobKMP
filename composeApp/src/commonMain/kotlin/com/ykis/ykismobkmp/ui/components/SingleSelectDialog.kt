package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val className = "SingleSelectDialog"

/**
 * [SingleSelectDialog] — Кроссплатформенное модальное окно выбора тем и конфигураций ЮКИС.
 */
@Composable
fun SingleSelectDialog(
  modifier: Modifier = Modifier,
  title: String,
  optionsList: List<String>,
  defaultSelected: Int,
  submitButtonText: String,
  dismissButtonText: String,
  icon: ImageVector? = null,
  headerText: String? = null,
  onSubmitButtonClick: (Int) -> Unit,
  onDismissRequest: () -> Unit
) {
  var selectedOptionIndex by remember { mutableStateOf(defaultSelected) }

  AlertDialog(
    modifier = modifier.widthIn(max = 400.dp),
    onDismissRequest = onDismissRequest,
    icon = {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
      }
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (headerText != null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Color(0xFFFFB300), // Желтый цвет для иконки
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = headerText,
              style = MaterialTheme.typography.bodyLarge.copy(
                  color = Color(0xFFFFB300),
                  fontWeight = FontWeight.Bold,
                  lineHeight = 20.sp
              ),
              textAlign = TextAlign.Center
            )
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          itemsIndexed(optionsList) { index, optionText ->
            RadioButtonForDialog(
              text = optionText,
              isSelected = index == selectedOptionIndex,
              onSelect = { selectedOptionIndex = index }
            )
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(
          text = dismissButtonText,
          style = MaterialTheme.typography.labelLarge
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmitButtonClick(selectedOptionIndex)
          onDismissRequest()
        },
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(
          text = submitButtonText,
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  )
}

/**
 * [RadioButtonForDialog] — Кроссплатформенная строка Radio-выбора Material 3.
 */
@Composable
fun RadioButtonForDialog(
  modifier: Modifier = Modifier,
  text: String,
  isSelected: Boolean,
  onSelect: () -> Unit
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable { onSelect() }
      .padding(vertical = 4.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = isSelected,
      onClick = onSelect
    )

    Spacer(modifier = Modifier.width(8.dp))

    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}
