package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.ui.components.className

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobKMP
import com.ykis.ykismobkmp.ui.navigation.NavigationType

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
  // ИСПРАВЛЕНО НАМЕРТВО: Принудительно зажали общую высоту верхнего тулбара до компактных 52.dp!
  // Это уберет лишнюю пустую воздушную полосу сверху смартфона.
  Surface(
    modifier = modifier.height(52.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp
  ) {
    CenterAlignedTopAppBar(
      // ИСПРАВЛЕНО НАМЕРТВО: Сбросили скрытые инсеты windowInsets, чтобы убрать микро-сжатие иконок и кнопок!
      windowInsets = WindowInsets(0, 0, 0, 0),
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically, // Центрируем адрес идеально по линии тулбара
          horizontalArrangement = Arrangement.Center
        ) {
          // Текст адреса или службы остается КРУПНЫМ, сочным и отлично читаемым!
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )

          if (!subtitle.isNullOrBlank()) {
            Text(
              text = " | $subtitle",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(start = 4.dp),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      },
      navigationIcon = {
        if (canNavigateBack) {
          IconButton(onClick = {
            println("[YkisLogKMP.DefaultAppBar]: Клик назад — триггер сквозного Stateless-возврата.")
            onBackClick()
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Назад",
              modifier = Modifier.size(24.dp) // Сохраняем крупный читаемый размер стрелки
            )
          }
        } else if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
          IconButton(onClick = {
            println("[YkisLogKMP.DefaultAppBar]: Клик по бургер-меню (Открыть Drawer)")
            onDrawerClick()
          }) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = "Меню",
              modifier = Modifier.size(24.dp) // Сохраняем крупный читаемый размер бургера
            )
          }
        }
      },
      actions = {
        if (actionButton != null) actionButton()
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = Color.Unspecified,
        navigationIconContentColor = Color.Unspecified,
        titleContentColor = Color.Unspecified,
        actionIconContentColor = Color.Unspecified
      )
    )
  }
}


