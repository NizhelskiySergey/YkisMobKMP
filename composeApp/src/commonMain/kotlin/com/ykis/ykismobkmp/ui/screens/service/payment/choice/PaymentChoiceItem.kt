package com.ykis.ykismobkmp.ui.screens.service.payment.choice


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

private const val className = "PaymentChoiceItem"

/**
 * [PaymentChoiceItem] — Кроссплатформенный элемент выбора ЖКХ-службы и ручной корректировки суммы инвойса Xpay.
 */
@Composable
fun PaymentChoiceItem(
  modifier: Modifier = Modifier,
  service: String,
  debt: Double,
  onCheckedTrue: (String, Double) -> Unit,
  onCheckedFalse: (Double) -> Unit,
  userInput: String,
  onTextChange: (String) -> Unit = {}
) {
  var checked by rememberSaveable { mutableStateOf(false) }

  Card(
    modifier = modifier
      .padding(4.dp)
      .alpha(if (checked) 1f else 0.6f), // Слегка приглушаем неактивные карточки для улучшения UX
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp) // Увеличены внутренние поля по сетке Material 3
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Checkbox(
          checked = checked,
          onCheckedChange = { isChecked ->
            checked = isChecked
            if (isChecked) {
              onCheckedTrue(service, debt)
              onTextChange(debt.toString())
            } else {
              val userInputDebt = userInput.toDoubleOrNull()
              if (userInputDebt != null) {
                onCheckedFalse(userInputDebt)
              }
              onTextChange("0.00")
            }
          }
        )

        // ИСПРАВЛЕНО: Внутренний вес .weight(1f) теперь жестко изолирован от входящего modifier
        Text(
          text = "$service:",
          style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )

        Text(
          text = debt.toString(),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
          modifier = Modifier.padding(start = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Поле ручного перерасчета вносимого аванса по конкретной службе г. Южного
      TextField(
        value = userInput,
        onValueChange = { newValue ->
          // Пропускаем изменение только если это валидное число или пустая строка для безопасности
          if (newValue.isEmpty() || newValue.toDoubleOrNull() != null || newValue.endsWith(".")) {
            onTextChange(newValue)
          }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = "Сума до сплати") },
        shape = RoundedCornerShape(12.dp),
        enabled = checked,
        // ИСПРАВЛЕНО: Добавлено принудительное КМР-ограничение ввода числовых значений с копейками
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = TextFieldDefaults.colors(
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
          focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
          disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
          focusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        )
      )
    }
  }
}



@Preview(showBackground = true)
@Composable
private fun PreviewPaymentChoiceItem() {
    YkisPAMTheme {
        PaymentChoiceItem(
            service = "КП ЮЖВОДОКАНАЛ",
            debt = 245.00,
            onCheckedTrue = { _, _ ->
            },
            onCheckedFalse = {},
            onTextChange = {},
            userInput = "0.00",
            modifier = Modifier
        )
    }
}
