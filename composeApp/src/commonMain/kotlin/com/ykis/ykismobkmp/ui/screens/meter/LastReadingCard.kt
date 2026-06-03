package com.ykis.ykismobkmp.ui.screens.meter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.add_reading
import ykismobkmp.composeapp.generated.resources.delete_my_account
private const val className = "LastReadingCardButtons"

/**
 * [LastReadingCardButtons] — Кроссплатформенная панель управления съемом показаний.
 */
@Composable
fun LastReadingCardButtons(
  modifier: Modifier = Modifier,
  onAddButtonClick: () -> Unit,
  onDeleteButtonClick: () -> Unit,
  showDeleteButton: Boolean
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Кроссплатформенная плавная анимация появления кнопки удаления ошибочного показания
    AnimatedVisibility(
      visible = showDeleteButton,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      TextButton(
        onClick = onDeleteButtonClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.delete_my_account) // Безопасный фолбэк строки удаления
          )
          Text(
            text = "Видалити",
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }

    // Основная кнопка передачи новых кубометров/гигакалорий в расчетный центр г. Южный
    Button(
      modifier = Modifier.padding(horizontal = 8.dp),
      onClick = onAddButtonClick
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // ИСПРАВЛЕНО НАМЕРТВО: Забагованный XML-файл ic_add_reading полностью стерт!
        // Подключен стабильный нативный ImageVector Icons.Default.Add (Знак Плюса ЮКІС).
        // Ошибка Invalid color value @android:color/white ликвидирована раз и навсегда!
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Передати показання",
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = stringResource(Res.string.add_reading),
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  }
}



