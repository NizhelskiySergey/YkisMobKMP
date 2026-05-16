package com.ykis.ykismobkmp.ui.components

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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.back_button

private const val className = "AddAppBar"

/**
 * [AddAppBar] — Кроссплатформенная панель навигации для экранов добавления и привязки лицевых счетов.
 * Полностью адаптивна, очищена от легаси-типов навигации и готова к рендерингу на любой ОС.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppBar(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String,
  canNavigateBack: Boolean,
  onBackPressed: () -> Unit,
  onDrawerClicked: () -> Unit
) {
  TopAppBar(
    // ИСПРАВЛЕНО: Принудительное заполнение ширины страхует шапку от сжатия в окнах Mac Desktop
    modifier = modifier.fillMaxWidth(),
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    title = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp), // Небольшой отступ справа для баланса с иконкой
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle.isNotEmpty()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    },
    navigationIcon = {
      if (canNavigateBack) {
        IconButton(onClick = onBackPressed) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.back_button),
            modifier = Modifier.size(24.dp)
          )
        }
      } else {
        // Если назад идти нельзя (главный подэкран) — выводим кнопку вызова бокового Drawer-меню
        IconButton(onClick = onDrawerClicked) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Меню",
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  )
}

