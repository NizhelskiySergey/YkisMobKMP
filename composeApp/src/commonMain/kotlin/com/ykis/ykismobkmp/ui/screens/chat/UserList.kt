package com.ykis.ykismobkmp.ui.screens.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.screens.apartment.BaseUIState
import com.ykis.ykismobkmp.ui.screens.apartment.UserRole
import com.ykis.ykismobkmp.ui.screens.chat.ChatViewModel
import android.util.Log
import com.ykis.ykismobkmp.ui.screens.chat.UserListItem

private const val className = "UserList"

data class UserWithLatestMessage(
  val user: UserEntity,
  val latestMessage: MessageEntity,
  val unreadCount: Int,
  val chatId: String
)

@Composable
fun UserList(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  userList: List<UserEntity>,
  onUserClick: (UserEntity) -> Unit,
  chatViewModel: ChatViewModel
) {
  // Подписки на мультиплатформенные StateFlow
  val latestMessages by chatViewModel.lastMessages.collectAsState()
  val unreadCounts by chatViewModel.unreadCounts.collectAsState()
  val selectedPrefix by chatViewModel.selectedServicePrefix.collectAsState()

  // Трансформация и сортировка списка (Золотой фонд логики ключей)
  val userWithMessages = remember(userList, latestMessages, unreadCounts, selectedPrefix) {
    val methodName = "Mapping"
    Log.d("YkisLog", "[$className.$methodName]: Start. Role: ${baseUIState.userRole} | Prefix: $selectedPrefix")

    userList.map { user ->
      // ГЕНЕРАЦИЯ КЛЮЧА ЧАТА (Синхронизировано с логикой PHP/Firebase)
      val chatId = when (baseUIState.userRole) {
        UserRole.StandardUser -> {
          val prefix = selectedPrefix ?: "UNKNOWN"
          "${prefix}_${user.osbbId ?: 0}_${user.addressId}_${user.uid}"
        }
        UserRole.VodokanalUser -> "WATER_SERVICE_9999_${user.addressId}_${user.uid}"
        UserRole.YtkeUser      -> "WARM_SERVICE_9998_${user.addressId}_${user.uid}"
        UserRole.TboUser       -> "GARBAGE_SERVICE_9997_${user.addressId}_${user.uid}"
        UserRole.OsbbUser      -> "OSBB_${baseUIState.osbbId}_${user.addressId}_${user.uid}"
        else                   -> "UNKNOWN_${user.addressId}_${user.uid}"
      }

      val lastMsg = latestMessages[chatId]
      val count = unreadCounts[chatId] ?: 0

      val safeMsg = lastMsg ?: MessageEntity(text = "", timestamp = 0L)

      // Если адрес отправителя пустой, используем дефолтное имя
      val stableDisplayName = if (!safeMsg.senderAddress.isNullOrBlank()) {
        safeMsg.senderAddress
      } else {
        user.displayName ?: "Користувач (о/р ${user.addressId})"
      }

      UserWithLatestMessage(
        user = user.copy(displayName = stableDisplayName),
        latestMessage = safeMsg,
        unreadCount = count,
        chatId = chatId
      )
    }.sortedWith(
      // Сначала чаты с новыми сообщениями, затем по времени последнего сообщения
      compareByDescending<UserWithLatestMessage> { it.unreadCount > 0 }
        .thenByDescending { it.latestMessage.timestamp }
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = 8.dp)
  ) {
    if (userWithMessages.isEmpty()) {
      item {
        Box(
          modifier = Modifier.fillParentMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Активних чатів не знайдено",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }

    items(
      items = userWithMessages,
      key = { it.chatId }
    ) { item ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        UserListItem(
          user = item.user,
          onUserClick = {
            Log.d("YkisLog", "[$className.onClick]: ChatId: ${item.chatId}")
            onUserClick(it)
          },
          lastMessage = if (item.latestMessage.timestamp > 0L) item.latestMessage else null,
          currentUid = baseUIState.uid.toString()
        )

        // Бейдж непрочитанных сообщений
        if (item.unreadCount > 0) {
          Surface(
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .padding(end = 16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            tonalElevation = 6.dp
          ) {
            Text(
              text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onError,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }
  }
}
