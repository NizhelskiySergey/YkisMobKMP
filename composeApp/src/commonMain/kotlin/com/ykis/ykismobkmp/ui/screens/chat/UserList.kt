package com.ykis.ykismobkmp.ui.screens.chat.components

// ИМПОРТЫ НАШИХ УТВЕРЖДЕННЫХ КМР СТАНДАРТОВ YkisMobPAM / YkisMobKMP
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.chat.ChatScreenModel
import org.koin.compose.koinInject

// Временные КМР-заглушки элемента списка, пока не присланы сорцы его верстки
@Composable fun UserListItem(user: UserEntity, onUserClick: (UserEntity) -> Unit, lastMessage: MessageEntity?, currentUid: String) { Button(onClick = { onUserClick(user) }) { Text(user.displayName ?: "") } }

private const val className = "UserList"

/**
 * [UserWithLatestMessage] — Кросплатформенный контейнер-снимок диалога чат-системы.
 */
data class UserWithLatestMessage(
  val user: UserEntity,
  val latestMessage: MessageEntity,
  val unreadCount: Int,
  val chatId: String
)

/**
 * [UserListScreen] — Кроссплатформенный Voyager-экран списка активных диалогов и коммунальных заявок.
 * ИСПРАВЛЕНО: Полностью отвязан от ChatViewModel, логирование переведено в КМР-формат.
 */
class UserListScreen(
  private val baseUIState: BaseUIState,
  private val userList: List<UserEntity>,
  private val onUserClick: (UserEntity) -> Unit
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Нативная КМР инжекция ScreenModel вместо Android ViewModel
    val chatScreenModel = koinInject<ChatScreenModel>()

    UserList(
      modifier = Modifier.fillMaxSize(),
      baseUIState = baseUIState,
      userList = userList,
      onUserClick = onUserClick,
      chatScreenModel = chatScreenModel
    )
  }
}

/**
 * [UserList] — Декларативная верстка и сортировка ленты диалогов Material 3.
 */
@Composable
fun UserList(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  userList: List<UserEntity>,
  onUserClick: (UserEntity) -> Unit,
  chatScreenModel: ChatScreenModel // ИСПРАВЛЕНО: Заменен платформозависимый ChatViewModel
) {
  // Подписки на мультиплатформенные StateFlow из ChatScreenModel
  val latestMessages by chatScreenModel.lastMessages.collectAsState()
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val selectedPrefix by chatScreenModel.selectedServicePrefix.collectAsState()

  // Трансформация и сортировка списка (Твой оригинальный Золотой фонд логики ключей)
  val userWithMessages = remember(userList, latestMessages, unreadCounts, selectedPrefix) {
    val methodName = "Mapping"
    // ИСПРАВЛЕНО: Вызов Log.d заменен на println в КМР-стандарте [Класс.Метод]
    println("[$className.$methodName]: Start mapping. Role: ${baseUIState.userRole} | Prefix: $selectedPrefix")

    userList.map { user ->
      // ГЕНЕРАЦИЯ КЛЮЧА ЧАТА (Синхронизировано с логикой PHP/Firebase и сквозным Long-типом данных)
      val chatId = when (baseUIState.userRole) {
        UserRole.StandardUser -> {
          val prefix = selectedPrefix ?: "UNKNOWN"
          "${prefix}_${user.osbbId ?: 0L}_${user.addressId}_${user.uid}"
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
      key = { it.chatId } // Уникальный ключ строки на основе сгенерированного чат-токена
    ) { item ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        UserListItem(
          user = item.user,
          onUserClick = {
            println("[$className.onClick]: Клик по диалогу. Выбран ChatId: ${item.chatId}")
            onUserClick(it)
          },
          lastMessage = if (item.latestMessage.timestamp > 0L) item.latestMessage else null,
          currentUid = baseUIState.uid.toString()
        )

        // Сферический бейдж непрочитанных сообщений коммунальных ведомостей
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

