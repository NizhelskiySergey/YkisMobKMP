package com.ykis.ykismobkmp.ui.screens.service.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HeaderInTable(text: String, modifier: Modifier, textAlign: TextAlign) {

    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Normal
        ),
        textAlign = textAlign,
    )
}


@Composable
fun ColumnItemInTable(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal,
    value1: String,
    value2: String,
    value3: String,
    value4: String,
    header: String,
    summary: String,
    headerAlign: TextAlign
) {
    Column(
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    {
        HeaderInTable(
            modifier = modifier,
            text = header,
            textAlign = headerAlign
        )
        if (value1 !== "null" && value1 !== "none") {
            Text(
                text = value1,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
        if (value2 != "null" && value2 != "none") {
            Text(
                text = value2,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
        if (value3 != "null" && value3 != "none") {
            Text(
                text = value3,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
        if (value4 != "null" && value4 != "none") {
            Text(
                text = value4,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
        HeaderInTable(
            text = summary,
            modifier = modifier,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TableDivider(
    previousValue:String? = null,
    modifier: Modifier = Modifier
) {
    if(previousValue != "null" && previousValue != "none") {
        HorizontalDivider(
            modifier = modifier.fillMaxWidth().alpha(0.3f).padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
