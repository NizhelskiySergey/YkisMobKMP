package com.ykis.mob.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterChipSample(
    text: String,
    modifier: Modifier = Modifier,
    onSelectedChanged: (String) -> Unit = {},
    isSelected: Boolean = false
) {
    FilterChip(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .animateContentSize(),
        selected = isSelected,
        label = {
            Text(text)
        },
        onClick = { onSelectedChanged(text) },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun GroupFilterChip(
    list: List<String>,
    selectedChip: Any? = null,
    onSelectedChanged: (String) -> Unit = {}
) {
    LazyRow {
        items(items = list) { text ->
            FilterChipSample(
                text = text,
                onSelectedChanged = { onSelectedChanged(text) },
                isSelected = text == selectedChip
            )
        }
    }
}
