package com.ykis.mob.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetailPanel(
    modifier: Modifier = Modifier,
    showDetail: Boolean,
    detailContent: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = showDetail,
        enter = slideInVertically(
            tween(
                durationMillis = 550,
                easing = EaseOutCubic
            ),
            initialOffsetY = {
                it
            },
        ) + fadeIn(
            tween(
                durationMillis = 400
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it }
        )
                + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor =
                MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
                detailContent()
        }
    }
}