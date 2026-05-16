package com.ykis.ykismobkmp.ui.screens.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme
import com.ykis.ykismobkmp.utils.formatTimestamp // Твой будущий хелпер
import org.jetbrains.compose.ui.tooling.preview.Preview
import android.util.Log
import com.ykis.ykismobkmp.ui.components.UserImage

private const val className = "UserListItem"

/**
 * [UserListItem] — элемент списка чатов.
 * Адаптирован для отображения адреса, имени жильца и последнего сообщения.
 */
@Composable
fun UserListItem(
  modifier: Modifier = Modifier,
  user: UserEntity, // Переименовано из 'it' для ясности
  onUserClick: (UserEntity) -> Unit,
  lastMessage: MessageEntity?,
  currentUid: String = ""
) {
  // 1. ПАРСИНГ ИМЕНИ И АДРЕСА (Золотой фонд логики)
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
        Log.d("YkisLog", "[$className.onClick]: User ID: ${user.uid}")
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
        // --- СТРОКА 1: АДРЕС И ВРЕМЯ ---
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

          if (lastMessage != null && lastMessage.timestamp > 0) {
            Text(
              text = formatTimestamp(lastMessage.timestamp), // КМП версия форматирования
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
          }
        }

        // --- СТРОКА 2: ФИО ЖИЛЬЦА ---
        if (residentName.isNotEmpty()) {
          Text(
            text = residentName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1
          )
        }

        // --- СТРОКА 3: ПРЕВЬЮ СООБЩЕНИЯ ---
        val prefix = if (lastMessage?.senderUid == currentUid) "Ви: " else ""
        val displayText = when {
          lastMessage == null -> "Немає повідомлень"
          !lastMessage.text.isNullOrBlank() -> "$prefix${lastMessage.text}"
          lastMessage.imageUrl != null -> "$prefix📷 Фотографія"
          lastMessage.fileUrl != null -> "$prefix📎 Файл"
          else -> "Немає повідомлень"
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

@Preview
@Composable
private fun PreviewUserListItem() {
  YkisPAMTheme {
    UserListItem(
      user = UserEntity(displayName = "вул. Будівельників, 10 | Іванов І.І."),
      onUserClick = {},
      lastMessage = MessageEntity(
        text = "Добрий день! Коли буде вода?",
        timestamp = 1720000000000L
      ),
      currentUid = "other_uid"
    )
  }
}
