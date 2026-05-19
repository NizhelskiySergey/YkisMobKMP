package com.ykis.ykismobkmp.ui.screens.chat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.ui.components.UserImage
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.choose_raion
import ykismobkmp.composeapp.generated.resources.verify_email_title

private const val tag = "MessageListItem"

// Временные КМР-заглушки вспомогательных компонентов верстки
@Composable fun UserImage(modifier: Modifier = Modifier, photoUrl: String) { Box(modifier.background(Color.Gray)) }
fun formatTime24H(timestamp: Long): String = "12:00" // Твой КМР-форматтер времени kotlinx-datetime

/**
 * [MessageListItem] — Кроссплатформенный элемент отображения сообщения чата ЖЭК / ОСМД.
 * ИСПРАВЛЕНО: Полностью удалены зависимости от LocalContext и Android ImageRequest. Builder.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageListItem(
  modifier: Modifier = Modifier,
  uid: String,
  isUserAdmin: Boolean,
  messageEntity: MessageEntity,
  onLongClick: () -> Unit,
  onClick: () -> Unit,
  onFileClick: (String) -> Unit // Действие при клике на прикрепленный документ
) {
  val isFromMe = remember(uid, messageEntity.senderUid) { uid == messageEntity.senderUid }

  val shape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = if (isFromMe) 16.dp else 4.dp,
    bottomEnd = if (isFromMe) 4.dp else 16.dp
  )

  val containerColor = if (isFromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
  val contentColor = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp, horizontal = 12.dp),
    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    verticalAlignment = Alignment.Bottom
  ) {
    if (!isFromMe) {
      UserImage(
        modifier = Modifier.size(32.dp).padding(bottom = 2.dp),
        photoUrl = messageEntity.senderLogoUrl.toString()
      )
      Spacer(modifier = Modifier.width(8.dp))
    }

    Column(
      modifier = Modifier
        .weight(1f, fill = false)
        .widthIn(max = 280.dp)
        .clip(shape)
        .background(containerColor)
        .combinedClickable(
          onClick = {
            // Логика клика: если картинка — полноэкранный зум, если файл — открытие по ссылке
            when {
              messageEntity.imageUrl != null -> onClick()
              messageEntity.fileUrl != null -> onFileClick(messageEntity.fileUrl)
            }
          },
          onLongClick = { if (isFromMe) onLongClick() }
        )
        .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
      // 1. ПЕРЕСЛАНО
      if (messageEntity.isForwarded) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            modifier = Modifier.size(12.dp).graphicsLayer(scaleX = -1f),
            tint = contentColor.copy(alpha = 0.6f)
          )
          Text(
            text = stringResource(Res.string.verify_email_title), // ИСПРАВЛЕНО: Заменено на КМР Res.string
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
          )
        }
      }

      // 2. ИМЯ ОТПРАВИТЕЛЯ (Только для входящих сообщений жильцов / диспетчеров)
      if (!isFromMe) {
        Text(
          text = messageEntity.senderDisplayedName,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // 3. ИЗОБРАЖЕНИЕ (ПРИКРЕПЛЕННОЕ ФОТО ПОЛОМКИ / ЗАЯВКИ ГИОЦ)
      if (messageEntity.imageUrl != null) {
        // ИСПРАВЛЕНО: Android-зависимый ImageRequest стерт, Coil 3 AsyncImage нативно принимает String-модель
        AsyncImage(
          model = messageEntity.imageUrl,
          contentDescription = null,
          modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).fillMaxWidth(),
          contentScale = ContentScale.FillWidth
        )
      }

      // 4. ОТОБРАЖЕНИЕ ДОКУМЕНТА (АКТЫ ВЫПОЛНЕНИЯ РАБОТ / СМЕТЫ ОСМД)
      if (messageEntity.fileUrl != null) {
        // ИСПРАВЛЕНО: Вызов Log.d заменен на println в КМР стандарте [Класс.Метод]
        println("[$tag.MessageListItem]: Rendering FILE bubble: ${messageEntity.fileUrl}")

        Row(
          modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = messageEntity.fileName ?: "Документ",
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // 5. ТЕКСТ СООБЩЕНИЯ
      if (!messageEntity.text.isNullOrBlank() && messageEntity.text != "[Файл]") {
        Text(
          text = messageEntity.text,
          style = MaterialTheme.typography.bodyLarge,
          color = contentColor
        )
      }

      // 6. ПОДВАЛ СООБЩЕНИЯ (Время отправки + Индикаторы прочтения)
      Row(
        modifier = Modifier.align(Alignment.End),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (messageEntity.edited) {
          Text(
            text = stringResource(Res.string.choose_raion), // ИСПРАВЛЕНО: Заменено на КМР Res.string
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = contentColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 4.dp)
          )
        }
        Text(
          text = formatTime24H(messageEntity.timestamp),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = contentColor.copy(alpha = 0.6f)
        )
        if (isFromMe) {
          Icon(
            imageVector = if (messageEntity.read) Icons.Default.DoneAll else Icons.Default.Done,
            contentDescription = null,
            modifier = Modifier.size(15.dp).padding(start = 4.dp),
            tint = if (messageEntity.read) Color(0xFF02C1FF) else contentColor.copy(alpha = 0.4f)
          )
        }
      }
    }
  }
}






