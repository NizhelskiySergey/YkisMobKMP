package com.ykis.ykismobkmp.ui.screens.meter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

@Composable
fun LastReadingCardButtons(
    modifier: Modifier = Modifier,
    onAddButtonClick: ()->Unit,
    onDeleteButtonClick:()->Unit,
    showDeleteButton:Boolean,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    )
    {
        AnimatedVisibility(
            visible = showDeleteButton,
            enter = fadeIn(),
            exit = fadeOut()
        ){
            TextButton(
                onClick = {onDeleteButtonClick()},
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(id = R.string.delete)
                    )
                }
            }
        }

        Button(
            modifier = modifier.padding(horizontal = 8.dp),
            onClick = {
                onAddButtonClick()
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add_reading),
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.add_reading)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun PreviewLastWaterReadingCard() {
    YkisPAMTheme {
        LastReadingCardButtons(
            onAddButtonClick = {},
            onDeleteButtonClick = {},
            showDeleteButton = true
        )
    }
}
