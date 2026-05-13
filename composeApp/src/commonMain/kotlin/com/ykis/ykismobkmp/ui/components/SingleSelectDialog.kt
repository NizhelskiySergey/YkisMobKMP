package com.ykis.mob.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SingleSelectDialog(
    modifier: Modifier,
    title: String,
    optionsList: List<String>,
    defaultSelected: Int,
    submitButtonText: String,
    dismissButtonText: String,
    onSubmitButtonClick: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {

    var selectedOption by remember {
        mutableIntStateOf(defaultSelected)
    }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card{
            Column(modifier = modifier.padding(24.dp)) {
                Icon(
                    modifier = modifier.fillMaxWidth(),
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null
                )
                Text(
                    textAlign = TextAlign.Center,
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = modifier.fillMaxWidth().padding( vertical = 16.dp)
                )

                LazyColumn {
                    items(optionsList) {
                        RadioButtonForDialog(
                            modifier = modifier, it, optionsList[selectedOption]
                        ) { selectedValue ->
                            selectedOption = optionsList.indexOf(selectedValue)
                        }
                    }
                }

                Spacer(modifier = modifier.height(24.dp))

                Row(
                    modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismissRequest()
                    }) {
                        Text(
                            text = dismissButtonText
                        )
                    }
                    Spacer(modifier = modifier.width(8.dp))
                    TextButton(onClick = {
                        onSubmitButtonClick.invoke(selectedOption)
                        onDismissRequest()
                    }) {
                        Text(
                            text = submitButtonText
                        )
                    }
                }
            }
        }

    }
}
@Composable
fun RadioButtonForDialog(
    modifier: Modifier, text: String, selectedValue: String, onClickListener: (String) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(8.dp)
            )
            .clickable {
                onClickListener(text)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (text == selectedValue), onClick = {
                onClickListener(text)
            }
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            modifier = modifier.padding(start = 2.dp)
        )
    }
}