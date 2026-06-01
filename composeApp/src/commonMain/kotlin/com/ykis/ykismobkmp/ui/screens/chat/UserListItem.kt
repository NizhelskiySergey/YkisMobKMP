package com.ykis.ykismobkmp.ui.screens.chat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.components.UserImage
import kotlinx.datetime.toLocalDateTime

private const val className = "UserListItem"

/**
 * [formatTimestamp] — Кроссплатформенный форматтер времени на базе библиотеки kotlinx-datetime.
 * Исправлено: Заглушка "12:00" удалена. Метод нативно вычисляет часы и минуты для Android/iOS.
 */
private fun formatTimestamp(timestamp: Long): String {
  if (timestamp <= 0L) return ""
  return try {
    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val hourStr = localDateTime.hour.toString().padStart(2, '0')
    val minuteStr = localDateTime.minute.toString().padStart(2, '0')
    "$hourStr:$minuteStr"
  } catch (e: Exception) {
    "00:00"
  }
}

/**
 * [UserListItem] — Кроссплатформенный элемент отображения строки активного диалога жильца / диспетчера ЮКИС.
 */
@Composable
fun UserListItem(
  modifier: Modifier = Modifier,
  user: UserEntity,
  onUserClick: (UserEntity) -> Unit,
  lastMessage: MessageEntity?,
  currentUid: String = ""
) {
  // 1. ПАРСИНГ ИМЕНИ И АДРЕСА (Твой оригинальный Золотой фонд логики)
  val displayName = user.displayName ?: lastMessage?.senderAddress ?: "Користувач"
  val (displayAddress, residentName) = remember(displayName) {
    val parts = displayName.split("|")
    val address = parts.getOrNull(0)?.trim() ?: displayName
    val name = parts.getOrNull(1)?.trim() ?: ""
    address to name
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        println("[YkisLogKMP.$className.onClick]: Клик по строке чата. Собеседник UID: ${user.uid}")
        onUserClick(user)
      }
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      UserImage(
        modifier = Modifier.size(52.dp),
        photoUrl = user.photoUrl.toString()
      )

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
      ) {
        // --- СТРОКА 1: АДРЕС ОБЪЕКТА НЕДВИЖИМОСТИ И ВРЕМЯ ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            modifier = Modifier.weight(1f),
            text = displayAddress,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          if (lastMessage != null && lastMessage.timestamp > 0L) {
            Text(
              text = formatTimestamp(lastMessage.timestamp),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
          }
        }

        // --- СТРОКА 2: ФИО ЗАРЕГИСТРИРОВАННОГО ЖИЛЬЦА ---
        if (residentName.isNotEmpty()) {
          Text(
            text = residentName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1
          )
        }

        // --- СТРОКА 3: ДИНАМИЧЕСКОЕ ПРЕВЬЮ ПОСЛЕДНЕГО СООБЩЕНИЯ ВЕТКИ ЧАТА ---
        val prefix = if (lastMessage?.senderUid == currentUid) "Ви: " else ""
        val displayText = remember(lastMessage, prefix) {
          when {
            lastMessage == null -> "Немає повідомлень"
            !lastMessage.text.isNullOrBlank() -> "$prefix${lastMessage.text}"
            lastMessage.imageUrl != null -> "$prefix📷 Фотографія поломки"
            lastMessage.fileUrl != null -> "$prefix📎 Прикріплений файл"
            else -> "Немає повідомлень"
          }
        }

        Text(
          text = displayText,
          style = MaterialTheme.typography.bodySmall,
          color = if (lastMessage == null)
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    HorizontalDivider(
      modifier = Modifier.padding(start = 76.dp, end = 16.dp),
      thickness = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant
    )
  }
}



