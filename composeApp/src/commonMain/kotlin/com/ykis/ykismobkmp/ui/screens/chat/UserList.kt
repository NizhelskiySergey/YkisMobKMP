import com.ykis.ykismobkmp.core.Constants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import com.ykis.ykismobkmp.ui.screens.chat.UserListItem

private const val className = "UserList"

@Composable
fun UserList(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  userList: List<UserEntity>,
  onUserClick: (UserEntity) -> Unit,
  chatScreenModel: ChatScreenModel
) {
  val latestMessages by chatScreenModel.lastMessages.collectAsState()
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val typingStatuses by chatScreenModel.globalTypingStatuses.collectAsState()
  val selectedPrefix by chatScreenModel.selectedServicePrefix.collectAsState()

  // ОПТИМИЗАЦИЯ: Упрощаем логику формирования списка, чтобы не блокировать Main Thread
  val userWithMessages = remember(userList, latestMessages, unreadCounts, typingStatuses, selectedPrefix) {
    userList.map { user ->
      val chatId = when (baseUIState.userRole) {
        UserRole.StandardUser -> {
          val prefix = selectedPrefix
          val sysId = when(prefix) {
            "WATER_SERVICE" -> Constants.WATER_SERVICE_ID
            "WARM_SERVICE" -> Constants.WARM_SERVICE_ID
            "GARBAGE_SERVICE" -> Constants.GARBAGE_SERVICE_ID
            else -> user.osbbId ?: 0L
          }
          "${prefix}_${sysId}_${user.addressId}_${user.uid}"
        }
        UserRole.VodokanalUser -> "WATER_SERVICE_${Constants.WATER_SERVICE_ID}_${user.addressId}_${user.uid}"
        UserRole.YtkeUser      -> "WARM_SERVICE_${Constants.WARM_SERVICE_ID}_${user.addressId}_${user.uid}"
        UserRole.TboUser       -> "GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_${user.addressId}_${user.uid}"
        UserRole.OsbbUser      -> "OSBB_${baseUIState.osbbId}_${user.addressId}_${user.uid}"
        else                   -> "UNKNOWN_${user.addressId}_${user.uid}"
      }

      val lastMsg = latestMessages[chatId]
      val count = unreadCounts[chatId] ?: 0
      val isTyping = typingStatuses[chatId] ?: false
      
      Triple(user, lastMsg, Pair(count, isTyping))
    }.sortedByDescending { it.second?.timestamp ?: 0L } // Сортируем только по времени последнего сообщения
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
      key = { it.first.uid + it.first.addressId } // ИСПРАВЛЕНО: Более стабильный ключ для предотвращения SIGSEGV
    ) { (user, lastMsg, stats) ->
      val (count, isTyping) = stats
      
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        UserListItem(
          user = user,
          onUserClick = onUserClick,
          lastMessage = lastMsg,
          currentUid = baseUIState.uid.toString(),
          isTyping = isTyping
        )

        if (count > 0) {
          Surface(
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .padding(end = 16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            tonalElevation = 6.dp
          ) {
            Text(
              text = if (count > 99) "99+" else count.toString(),
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
