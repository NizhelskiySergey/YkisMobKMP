package com.ykis.ykismobkmp.ui.screens.service.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.mob.core.ext.formatDebt

@Composable
fun ServiceRow(
    modifier: Modifier = Modifier,
    color: Color,
    title: String,
    debt: Double,
    icon : ImageVector,
    onClick: ()->Unit
) {
    val formattedDebt = formatDebt(debt)
    Box(modifier = modifier.clickable {
        onClick()
    }){
        Row(
            modifier = modifier
                .padding(start = 12.dp ,end = 8.dp)
                .height(54.dp)
                .clearAndSetSemantics {
                    contentDescription =
                        "Ваш борг для компанії $title становить $debt"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typography = MaterialTheme.typography
            ServiceIndicator(
                color = color,
                modifier = Modifier
            )
            Icon(
                modifier = modifier.padding(horizontal = 8.dp),
                imageVector = icon,
                contentDescription = null
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

            }
            Spacer(modifier = modifier.width(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDebt,
                    style = typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = stringResource(R.string.uah),
                    style = typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp)
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.background , thickness = 1.dp)
}
