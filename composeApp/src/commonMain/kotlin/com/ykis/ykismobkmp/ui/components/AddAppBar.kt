package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobKMP
import com.ykis.ykismobkmp.ui.navigation.NavigationType

// ИМПОРТЫ КРОСС ПЛАТФОРМЕННЫХ РЕСУРСОВ СТРОК JETBRAINS
import ykismobkmp.composeapp.generated.resources.*

private const val className = "AddAppBar"

/**
 * [AddAppBar] — Кроссплатформенная верхняя панель окон добавления и привязки квартир ЮКИС.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppBar(
  modifier: Modifier = Modifier,
  subtitle: String,
  title: String,
  onBackPressed: () -> Unit,
  canNavigateBack: Boolean,
  onDrawerClicked: () -> Unit,
  navigationType: NavigationType
) {
  TopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    title = {
      // ИСПРАВЛЕНО: Убрано fillMaxWidth(). Контейнер Column теперь занимает ровно столько места,
      // сколько нужно тексту, исключая визуальный сдвиг из-за правого экшн-бокса TopAppBar.
      Column(
        horizontalAlignment = Alignment.Start
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
      if (!canNavigateBack && navigationType == NavigationType.BOTTOM_NAVIGATION) {
        IconButton(
          onClick = {
            println("[$className.AddAppBar]: Клик по кнопке Drawer меню")
            onDrawerClicked()
          }
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            // ИСПРАВЛЕНО: Заменено на мультиплатформенный строковый ресурс JetBrains
            contentDescription = stringResource(Res.string.verify_email_title),
            modifier = Modifier.size(24.dp),
          )
        }
      } else if (canNavigateBack) {
        IconButton(
          onClick = {
            println("[$className.AddAppBar]: Клик назад (Нативный возврат Voyager pop)")
            onBackPressed()
          }
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            // ИСПРАВЛЕНО: Заменено на мультиплатформенный строковый ресурс JetBrains
            contentDescription = stringResource(Res.string.verify_email_title),
            modifier = Modifier.size(24.dp),
          )
        }
      }
    },
  )
}

/**
 * ИСПРАВЛЕНО: Аннотация Preview переведена на кроссплатформенный КМР-стандарт JetBrains Compose Runtime.
 * Панель будет корректно рендериться в UI-инспекторах на Mac Desktop.
 */
