package com.ykis.ykismobkmp.ui.screens.ledger

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ ТИПОВ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val className = "CardEditors"

/**
 * [DangerousCardEditor] — Красная карточка критических действий безопасности (например, Удалить аккаунт).
 */
@Composable
fun DangerousCardEditor(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  modifier: Modifier = Modifier,
  onEditClick: () -> Unit
) {
  // ИСПРАВЛЕНО: Платформозависимый Log.d заменен универсальной КМР-функцией println()
  println("[$className.DangerousCardEditor]: Clicked")
  CardEditor(
    title = title,
    icon = icon,
    content = content,
    onEditClick = onEditClick,
    modifier = modifier
  )
}

/**
 * [RegularCardEditor] — Стандартный КМР-элемент изменения строковых параметров настроек.
 */
@Composable
fun RegularCardEditor(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  modifier: Modifier = Modifier,
  onEditClick: () -> Unit
) {
  println("[$className.RegularCardEditor]: Clicked")
  CardEditor(
    title = title,
    icon = icon,
    content = content,
    onEditClick = onEditClick,
    modifier = modifier
  )
}

/**
 * Приватная КМР-реализация контейнера карточки Material 3.
 * ИСПРАВЛЕНО: surfaceColorAtElevation заменен официальным токеном контейнера.
 */
@Composable
private fun CardEditor(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  onEditClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    // ИСПРАВЛЕНО: Цепочка отступов изолирована для защиты сетки окон на Mac Desktop
    modifier = modifier
      .widthIn(max = 480.dp)
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 8.dp),
    elevation = CardDefaults.cardElevation(2.dp), // Снижено до 2.dp по спецификации Material 3
    shape = RoundedCornerShape(12.dp), // Округление приведено к стандартам M3
    onClick = onEditClick,
    colors = CardDefaults.cardColors(
      // ИСПРАВЛЕНО: Устаревший метод surfaceColorAtElevation заменен стабильным цветом контейнера
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (content.isNotBlank()) {
        Text(
          text = content,
          modifier = Modifier.padding(horizontal = 16.dp),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Image(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

/**
 * [CardEditorInfo] — Кроссплатформенная карточка вывода справочной ЖКХ-информации профиля.
 */
@Composable
fun CardEditorInfo(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  onEditClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  println("[$className.CardEditorInfo]: Clicked")
  Card(
    modifier = modifier
      .widthIn(max = 480.dp)
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 8.dp),
    elevation = CardDefaults.cardElevation(2.dp),
    shape = RoundedCornerShape(12.dp),
    onClick = onEditClick,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (content.isNotBlank()) {
        Text(
          text = content,
          modifier = Modifier.padding(horizontal = 16.dp),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.primary
      )
    }
  }
}

