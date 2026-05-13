package com.ykis.mob.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.layout.DisplayFeature
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane

@Composable
fun BaseDualPanelContent(
    modifier: Modifier = Modifier,
    displayFeatures : List<DisplayFeature>,
    firstScreen: @Composable () -> Unit,
    secondScreen: @Composable () -> Unit,

) {
    TwoPane(
        modifier = modifier.fillMaxSize(),
        first = {
            firstScreen()
        },
        second = {
            secondScreen()
        },
        strategy = HorizontalTwoPaneStrategy(splitFraction = 0.5f),
        displayFeatures = displayFeatures
    )
}