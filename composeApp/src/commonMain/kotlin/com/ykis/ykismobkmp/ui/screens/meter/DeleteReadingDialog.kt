package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ykis.mob.R

@Composable
fun DeleteReadingDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = { onDismissRequest() },
    ) {
        Card {
            Column(
                modifier = modifier.padding(all = 24.dp)
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = modifier.fillMaxWidth(),
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.delete_reading_title),
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismissRequest()
                    }) {
                        Text(
                            text = stringResource(id = R.string.cancel)
                        )
                    }
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onDeleteClick()
                        },
                        enabled = true
                    ) {
                        Text(
                            stringResource(R.string.delete)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewDialog() {
    DeleteReadingDialog(
        onDismissRequest = { },
        onDeleteClick = {}
    )
}
