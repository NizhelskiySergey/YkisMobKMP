package com.ykis.ykismobkmp.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val className = "SettingsSwitch"

/**
 * [SettingsSwitch] — Кроссплатформенный элемент тумблера изменения параметров (например, Темная тема).
 * Полностью стабилен на Mac Desktop (JVM), Android и iOS без привязок к Android SDK.
 */
@Composable
fun SettingsSwitch(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Аннотации удалены, привязка типов переведена на KMP ресурсы JetBrains
  icon: DrawableResource,
  iconDesc: StringResource,
  name: StringResource,
  state: Boolean,
  onToggle: () -> Unit // Переименовано в onToggle для семантической чистоты
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 12.dp) // Выровнены отступы по гайдлайнам Material 3
      .clickable { onToggle() },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 16.dp), // Увеличена зона тача для Mac/Desktop
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ИСПРАВЛЕНО: painterResource адаптирован под кроссплатформенный тип DrawableResource
      Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(iconDesc),
        modifier = Modifier.size(24.dp),
        tint = if (state) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.width(16.dp))

      // ИСПРАВЛЕНО: Текст вычитывается через КМР stringResource
      Text(
        text = stringResource(name),
        modifier = Modifier.weight(1f), // Текст занимает все свободное пространство
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurface
      )

      // Системный переключатель настроек Material 3
      Switch(
        checked = state,
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White, // Используется универсальный KMP Color
          checkedTrackColor = MaterialTheme.colorScheme.primary
        ),
        // ИСПРАВЛЕНО: Передаем null, так как клик уже атомарно обрабатывается родителем (.clickable на Card)
        // Это предотвращает двойной триггер корутин DataStore на Mac Desktop при клике мышкой
        onCheckedChange = null
      )
    }
  }
}


