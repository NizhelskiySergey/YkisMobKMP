package com.ykis.ykismobkmp.ui.screens.service.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.mob.core.ext.extractProportions
import com.ykis.mob.core.ext.formatDebt
import com.ykis.mob.ui.components.AnimatedCircle

@Composable
fun <T> ServiceListStateless(
    modifier: Modifier = Modifier,
    items: List<T>,
    colors: (T) -> Color,
    debts: (T) -> Double,
    total: Double,
    circleLabel: String,
    rows: @Composable (T) -> Unit
) {
    BoxWithConstraints {
        val height = maxHeight
        val modifierBox1 : Modifier = if (height > 600.dp)
            Modifier.height(height - 224.dp)
        else
            Modifier.height(300.dp)

        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifierBox1
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                val accountsProportion = items.extractProportions { debts(it) }
                val circleColors = items.map { colors(it) }
                AnimatedCircle(
                    accountsProportion,
                    circleColors,
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(1f)
                )
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = circleLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = formatDebt(total) + stringResource(id = R.string.uah),
                        style = MaterialTheme.typography.headlineLarge  ,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            Card {
                Column{
                    items.forEach { item ->
                        rows(item)
                    }
                }
            }
        }
    }
}
