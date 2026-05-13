package com.ykis.mob.ui.components.appbars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ykis.mob.R
import com.ykis.mob.ui.navigation.NavigationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppBar(
    modifier: Modifier = Modifier,
    subtitle: String,
    title: String,
    onBackPressed: () -> Unit,
    canNavigateBack : Boolean,
    onDrawerClicked : () -> Unit,
    navigationType: NavigationType
) {

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        navigationIcon = {
            if(!canNavigateBack && navigationType == NavigationType.BOTTOM_NAVIGATION) {
                IconButton(
                    onClick = onDrawerClicked
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.back_button),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }else if(canNavigateBack) {
                IconButton(
                    onClick = onBackPressed,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back_button),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun PreviewAddAppBar() {
    AddAppBar(subtitle = "За допомогою секретного коду", title = "Додати квартиру" , onBackPressed = {} , canNavigateBack = false, onDrawerClicked = {} , navigationType = NavigationType.BOTTOM_NAVIGATION)
}