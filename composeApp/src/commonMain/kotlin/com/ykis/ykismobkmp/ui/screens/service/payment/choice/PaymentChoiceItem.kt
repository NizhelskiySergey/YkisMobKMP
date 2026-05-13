package com.ykis.ykismobkmp.ui.screens.service.payment.choice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme


@Composable
fun PaymentChoiceItem(
    modifier: Modifier = Modifier,
    service: String,
    debt: Double,
    onCheckedTrue: (String, Double) -> Unit,
    onCheckedFalse: (Double) -> Unit,
    userInput : String,
    onTextChange: (String) -> Unit = {},
) {
    var checked by rememberSaveable {
        mutableStateOf(false)
    }
//    var text by rememberSaveable {
//        mutableStateOf(
//            "0.00"
//        )
//    }
//    LaunchedEffect(key1 = checked) {
//        text = if(checked){
//            debt.toString()
//        }else "0.00"
//    }
    Card(
        modifier = modifier
            .padding(4.dp)
            .alpha(if (checked) 1f else 0.7f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked, onCheckedChange = {
                        checked = it
                        if (it) {
                            onCheckedTrue(
                                service, debt
                            )
//                            text = debt.toString()
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
                Text(
                    modifier = modifier.weight(1f),
                    text = "$service:",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 12.sp
                    )
                )
                Text(
                    modifier = modifier.padding(start = 4.dp),
                    text = debt.toString(),
                )
            }
            TextField(
                modifier = modifier,
                value = userInput,
                onValueChange = {
                    onTextChange(it)
                },
                label = {
                    Text(
                        text = "Сума до сплати"
                    )
                },
                shape = RoundedCornerShape(12.dp),
                enabled = checked,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
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
