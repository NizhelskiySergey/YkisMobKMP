package com.ykis.ykismobkmp.ui.screens.settings

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ ТИПОВ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val className = "SettingsGroup"

/**
 * [SettingsGroup] — Кроссплатформенный контейнер для визуальной группировки элементов настроек ЮКИС.
 * Полностью очищен от Android SDK и готов к нативной компиляции под Mac Desktop и iOS.
 */
@Composable
fun SettingsGroup(
  modifier: Modifier = Modifier,
  // ИСПРАВЛЕНО: Аннотация удалена, привязка типа переведена на KMP ресурс StringResource
  name: StringResource,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp, horizontal = 12.dp) // Выровнены отступы по гайдлайнам Material 3
  ) {
    // Заголовок группы настроек БТИ / Профиля
    Text(
      text = stringResource(name),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary, // Выделяем цветом бренда ЮКИС г. Южный
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )

    // Внедряем дочерние КМР элементы (SettingsText, SettingsSwitch, SettingsNumber)
    content()
  }
}
