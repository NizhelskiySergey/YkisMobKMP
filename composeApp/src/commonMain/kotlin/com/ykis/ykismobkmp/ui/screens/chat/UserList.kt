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
import com.ykis.ykismobkmp.ui.screens.chat.UserListItem
import org.koin.compose.koinInject
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
 * [UserList] — Декларативная верстка, маршалинг ключей Firebase и сортировка ленты диалогов Material 3.
 * Исправлено: Дублирующий класс UserListScreen полностью вырезан, предотвращая ошибку Redeclaration!
 */
@Composable
fun UserList(
  modifier: Modifier = Modifier,
  baseUIState: BaseUIState,
  userList: List<UserEntity>,
  onUserClick: (UserEntity) -> Unit,
  chatScreenModel: ChatScreenModel // Работаем на единой сквозной стейт-модели Хаба ЮКІС
) {
  // Подписки на мультиплатформенные StateFlow из ChatScreenModel
  val latestMessages by chatScreenModel.lastMessages.collectAsState()
  val unreadCounts by chatScreenModel.unreadCounts.collectAsState()
  val selectedPrefix by chatScreenModel.selectedServicePrefix.collectAsState()

  // Трансформация и высокоскоростная сортировка списка комнат на смартфонах
  val userWithMessages = remember(userList, latestMessages, unreadCounts, selectedPrefix) {
    val methodName = "Mapping"
    println("[YkisLogKMP.$className.$methodName]: Початок маршалінгу кімнат. Роль користувача: ${baseUIState.userRole} | Активний префікс: $selectedPrefix")

    userList.map { user ->
      // ГЕНЕРАЦИЯ КЛЮЧА ЧАТА (Синхронизировано на 100% с логикой СУБД, Firebase и ChatScreenModel!)
      val chatId = when (baseUIState.userRole) {
        UserRole.StandardUser -> {
          val prefix = selectedPrefix ?: "UNKNOWN"
          "${prefix}_${user.osbbId}_${user.addressId}_${user.uid}"
        }
        // Исправлено: Системные коды Long-идентификаторов коммунальных предприятий города Южного
        // приведены к жесткому и единому стандарту вьюмодели (9999L / 9998L / 9997L)!
        UserRole.VodokanalUser -> "WATER_SERVICE_9999_${user.addressId}_${user.uid}"
        UserRole.YtkeUser      -> "WARM_SERVICE_9998_${user.addressId}_${user.uid}"
        UserRole.TboUser       -> "GARBAGE_SERVICE_9997_${user.addressId}_${user.uid}"
        UserRole.OsbbUser      -> "OSBB_${baseUIState.osbbId}_${user.addressId}_${user.uid}"
        else                   -> "UNKNOWN_${user.addressId}_${user.uid}"
      }

      val lastMsg = latestMessages[chatId]
      val count = unreadCounts[chatId] ?: 0
      val safeMsg = lastMsg ?: MessageEntity(text = "", timestamp = 0L)

      // Если адрес отправителя пустой, нативно подставляем легитимный адрес/ФИО из карточки абонента БТИ
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
      // Сначала чаты с новыми сообщениями (бейджами), затем по времени последнего сообщения (DESC)
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

    // Рендерим отсортированную КМР-коллекцию чатов на дисплей смартфона
    items(
      items = userWithMessages,
      key = { it.chatId } // Уникальный составной токен в качестве КМР-ключа строки для Skiko
    ) { item ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        UserListItem(
          user = item.user,
          onUserClick = {
            println("[YkisLogKMP.$className.onClick]: Обрано діалог для входу. Сформований ChatId: ${item.chatId}")
            onUserClick(it)
          },
          lastMessage = if (item.latestMessage.timestamp > 0L) item.latestMessage else null,
          currentUid = baseUIState.uid.toString()
        )

        // Сферический бейдж непрочитанных сообщений коммунальных ведомостей ОСББ
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



