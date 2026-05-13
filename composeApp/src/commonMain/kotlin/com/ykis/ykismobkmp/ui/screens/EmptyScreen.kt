package com.ykis.ykismobkmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.mob.ui.components.appbars.AddAppBar
import com.ykis.mob.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentViewModel
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.empty_screen_subtitle
import ykismobkmp.composeapp.generated.resources.empty_screen_title
import ykismobkmp.composeapp.generated.resources.full_name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    popUpScreen: () -> Unit,
    viewModel: ApartmentViewModel = koinViewModel()


) {
    EmptyScreenContent(
        onBackPressed = { popUpScreen()},

        )
}


@ExperimentalMaterial3Api
@Composable
fun EmptyScreenContent(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
                .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // TODO: make this appBar universal
            AddAppBar(
                modifier,
                "",
                stringResource(Res.string.full_name),
                onBackPressed = { onBackPressed() },
                canNavigateBack = true,
                onDrawerClicked = {},
                navigationType = NavigationType.BOTTOM_NAVIGATION
            )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(Res.string.empty_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(Res.string.empty_screen_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
    @Preview(showBackground = true)
    @ExperimentalMaterial3Api
    @Composable
    fun EmptyScreenPreview() {
        YkisPAMTheme {
            EmptyScreenContent(
                onBackPressed = { },
            )
        }
    }
