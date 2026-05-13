package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.ui.NavigationType

private const val className = "DefaultAppBar"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppBar(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String? = null,
  onBackClick: () -> Unit = {},
  onDrawerClick: () -> Unit = {},
  canNavigateBack: Boolean = true,
  navigationType: NavigationType? = null,
  actionButton: @Composable (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp
  ) {
    CenterAlignedTopAppBar(
      title = {
        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.Center
        ) {
          // Основной заголовок (Улица/Название)
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          // Подзаголовок (Номер квартиры или о/р)
          if (!subtitle.isNullOrBlank()) {
            Text(
              text = " | $subtitle",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      },
      navigationIcon = {
        when {
          // Кнопка назад (приоритет)
          canNavigateBack -> {
            IconButton(onClick = {
              Log.d("YkisLog", "[$className.onBackClick]: Navigate up")
              onBackClick()
            }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
          }
          // Бургер-меню (только на телефонах при нижней навигации)
          navigationType == NavigationType.BOTTOM_NAVIGATION -> {
            IconButton(onClick = {
              Log.d("YkisLog", "[$className.onDrawerClick]: Open drawer")
              onDrawerClick()
            }) {
              Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
          }
          // На Mac/Desktop при наличии Rail иконка навигации не нужна
        }
      },
      actions = {
        if (actionButton != null) {
          actionButton()
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    )
  }
}
