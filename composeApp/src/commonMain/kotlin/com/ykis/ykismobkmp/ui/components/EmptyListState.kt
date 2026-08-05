package com.ykis.ykismobkmp.ui.components

// КРОСС ПЛАТФОРМЕННЫЕ ИМПОРТЫ РЕСУРСОВ JETBRAINS:
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import com.ykis.ykismobkmp.Res
import com.ykis.ykismobkmp.ic_empty_box_dark
import com.ykis.ykismobkmp.ic_empty_box_light

private const val className = "EmptyListState"

/**
 * [EmptyListState] — Кроссплатформенный компонент состояния пустых списков (счетчиков, оплат, чатов) ЮКИС.
 * Полностью адаптивен под темную/светлую тему рантайма и стабилен на Mac Desktop (JVM), Android и iOS.
 */
@Composable
fun EmptyListState(
  modifier: Modifier = Modifier,
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  title: String,
  subtitle: String = "",
  photoUrl: String? = null // НОВОЕ ПОЛЕ: Опциональный аватар пользователя
) {
  // КМР-выбор графической заглушки коробки в зависимости от темы оформления Mac/смартфона
  val paintRes = remember(useDarkTheme) {
    if (useDarkTheme) Res.drawable.ic_empty_box_dark else Res.drawable.ic_empty_box_light
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    contentAlignment = Alignment.Center
  ) {
    val currentMaxHeight = maxHeight

    // ИСПРАВЛЕНО: Условие переведено на адекватный порог плотности пикселей (500.dp вместо 5000.dp)
    val contentModifier = if (currentMaxHeight > 500.dp) {
      Modifier.fillMaxHeight().wrapContentHeight(Alignment.CenterVertically)
    } else {
      Modifier.wrapContentHeight()
    }

    Column(
      modifier = contentModifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (!photoUrl.isNullOrBlank()) {
          // ИСПРАВЛЕНО: Если передан URL аватара — показываем его вместо коробки
          UserImage(
            photoUrl = photoUrl,
            modifier = Modifier
              .size(140.dp)
              .padding(bottom = 20.dp)
          )
      } else {
          // ИСПРАВЛЕНО: painterResource адаптирован под KMP-ресурсы JetBrains, удален дублирующийся modifier
          Image(
            painter = painterResource(paintRes),
            contentDescription = "Список порожній",
            modifier = Modifier
              .size(140.dp) // Фиксируем красивый читаемый размер коробки на любой ОС
              .padding(bottom = 16.dp)
          )
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          lineHeight = 24.sp,
          letterSpacing = 0.15.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      if (subtitle.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}
