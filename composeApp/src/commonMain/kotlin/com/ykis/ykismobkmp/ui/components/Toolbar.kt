package com.ykis.ykismobkmp.ui.components

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ ТИПОВ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.back_button
import ykismobkmp.composeapp.generated.resources.driver_menu

private const val className = "Toolbars"

/**
 * [BackIcon] — Кроссплатформенная кнопка возврата на предыдущий экран Voyager.
 */
@Composable
fun BackIcon(
  navigateBack: () -> Unit
) {
  IconButton(
    onClick = {
      // ИСПРАВЛЕНО: Нативный Android Log.d заменен универсальной функцией println() общего кода Котлина
      println("[$className.BackIcon]: Navigate back clicked")
      navigateBack()
    }
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
      contentDescription = stringResource(Res.string.back_button)
    )
  }
}

/**
 * [BasicToolbar] — Минималистичная КМР-панель навигации по центру для служебных окон ЮКИС.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicToolbar(
  title: StringResource
) {
  println("[$className.BasicToolbar]: Rendering toolbar")
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
          style = MaterialTheme.typography.titleLarge
        )
      }
    }
  )
}

/**
 * [ActionToolbar] — Адаптивная КМР-панель чатов и диспетчеризации ОСМД г. Южное.
 * Автоматически меняет выравнивание текста и шапку в зависимости от форм-фактора (isFullScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionToolbar(
  title: StringResource,
  endActionIcon: DrawableResource,
  modifier: Modifier = Modifier,
  isFullScreen: Boolean,
  endAction: () -> Unit
) {
  println("[$className.ActionToolbar]: Rendering with isFullScreen=$isFullScreen")
  TopAppBar(
    // ИСПРАВЛЕНО: Базовый модификатор гарантирует растягивание шапки на всю ширину Mac-окна
    modifier = modifier.fillMaxWidth(),
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
            println("[$className.ActionToolbar]: End action (back) clicked")
            endAction()
          },
          modifier = Modifier.padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.back_button),
            modifier = Modifier.size(24.dp) // Сохранена увеличенная кликабельная зона Material 3
          )
        }
      }
    },
    actions = {
      IconButton(
        onClick = {
          println("[$className.ActionToolbar]: Menu clicked")
          /*TODO: Внедрение выпадающего КМР DropdownMenu действий */
        }
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
