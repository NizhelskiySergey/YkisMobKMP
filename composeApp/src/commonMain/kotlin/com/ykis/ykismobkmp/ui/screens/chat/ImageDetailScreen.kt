package com.ykis.ykismobkmp.ui.screens.chat

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobPAM / YkisMobKMP
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.domain.entity.MessageEntity
private const val className = "ImageDetailScreen"

/**
 * [ImageDetailScreen] — Кроссплатформенный Voyager-экран полноэкранного просмотра фотографий заявок ЮКИС.
 * Исправлено: Тяжелый MessageEntity удален из конструктора для предотвращения утечек ОЗУ на iOS/Mac.
 * Все данные реактивно вычитываются из сквозной ScreenModel финансового/чат хаба.
 */
class ImageDetailScreen(
  private val screenModel: ChatScreenModel // Передаем сквозную модель управления чатом
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Реактивно подписываемся на выбранное сообщение из ОЗУ вьюмодели
    val currentMessage by screenModel.selectedMessage.collectAsState()

    ImageDetailContent(
      messageEntity = currentMessage,
      navigateUp = {
        println("[YkisLogKMP.$className.Content]: Закриття перегляду фото. Нативний Voyager pop.")
        navigator.pop()
      }
    )
  }
}

/**
 * [ImageDetailContent] — Декларативная Stateless-верстка медиа-просмотрщика ГИОЦ г. Южного.
 */
@Composable
fun ImageDetailContent(
  modifier: Modifier = Modifier,
  navigateUp: () -> Unit,
  messageEntity: MessageEntity
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {

      // Кросплатформенный, аппаратно ускоренный контейнер масштабирования (Zoom) для Mac, iOS и Android
      var scale by remember { mutableStateOf(1f) }
      var offset by remember { mutableStateOf(Offset.Zero) }

      val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f) // Ограничиваем зум от 1х до 5х
        offset += offsetChange
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .transformable(state = state),
        contentAlignment = Alignment.Center
      ) {
        // Вместо Android Uri используется Coil 3 AsyncImage, принимающая обычную КМР-строку String
        AsyncImage(
          model = messageEntity.imageUrl ?: "",
          contentDescription = "Повноекранне зображення лічильника або аварії ЖКГ",
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
              scaleX = scale,
              scaleY = scale,
              translationX = offset.x,
              translationY = offset.y
            )
        )
      }

      // Подпись к фотографии (если диспетчер или житель ввел сопроводительный текст заявки в чат)
      if (!messageEntity.text.isNullOrBlank()) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color.Black.copy(alpha = 0.6f) // Полупрозрачный глубокий фон для текста под бренд ЮКІС
        ) {
          Text(
            modifier = Modifier
              .navigationBarsPadding() // КМР-отступ от нативных системных кнопок снизу дисплея смартфона
              .padding(16.dp),
            text = messageEntity.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 3, // Ограничение для длинных описаний аварий сантехники
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    // Кнопка закрытия (на фоне полупрозрачного круга, гарантированно читаемая на любом светлом фото)
    IconButton(
      modifier = Modifier
        .statusBarsPadding() // КМР-отступ от статус-бара сверху окна смартфона
        .padding(8.dp)
        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
      onClick = navigateUp
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = Color.White
      )
    }
  }
}


