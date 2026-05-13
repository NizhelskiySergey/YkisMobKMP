package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.*
import ykismobkmp.composeapp.generated.resources.Res

private const val className = "ToolbarsKt"

@Composable
fun BackIcon(
  navigateBack: () -> Unit
) {
  IconButton(
    onClick = {
      Log.d("YkisLog", "[$className.BackIcon]: Navigate back clicked")
      navigateBack()
    }
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
      contentDescription = stringResource(Res.string.back_button),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicToolbar(
  title: StringResource
) {
  Log.d("YkisLog", "[$className.BasicToolbar]: Rendering toolbar")
  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.inverseOnSurface
    ),
    title = {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.titleLarge,
        )
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionToolbar(
  title: StringResource,
  endActionIcon: DrawableResource,
  modifier: Modifier = Modifier,
  isFullScreen: Boolean,
  endAction: () -> Unit
) {
  Log.d("YkisLog", "[$className.ActionToolbar]: Rendering with isFullScreen=$isFullScreen")
  TopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.inverseOnSurface
    ),
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFullScreen) Alignment.CenterHorizontally else Alignment.Start
      ) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    navigationIcon = {
      if (isFullScreen) {
        IconButton(
          onClick = {
            Log.d("YkisLog", "[$className.ActionToolbar]: End action (back) clicked")
            endAction()
          },
          modifier = Modifier.padding(8.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.back_button),
            modifier = Modifier.size(24.dp) // Увеличил с 14dp до стандартных 24dp для кликабельности
          )
        }
      }
    },
    actions = {
      IconButton(
        onClick = {
          Log.d("YkisLog", "[$className.ActionToolbar]: Menu clicked")
          /*TODO*/
        },
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = stringResource(Res.string.driver_menu),
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  )
}
