package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.CameraScreenDest
import com.ykis.ykismobkmp.ui.navigation.LocalContentType
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import com.ykis.ykismobkmp.core.utils.rememberFilePicker
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

sealed class ChatItem {
  data class DateHeader(val date: String) : ChatItem()
  data class MessageItem(val message: MessageEntity) : ChatItem()
}

class ChatScreen(
  val chatId: String? = null,
  private val onBackClick: () -> Unit = {}
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val chatScreenModel = koinInject<ChatScreenModel>()
    val apartmentScreenModel = koinInject<ApartmentScreenModel>()

    val baseUIState by apartmentScreenModel.uiState.collectAsState()
    val selectedUser by chatScreenModel.selectedUser.collectAsState()

    LaunchedEffect(chatId, selectedUser?.addressId) {
      val isResident = baseUIState.userRole == UserRole.StandardUser
      val targetAddrId = if (!chatId.isNullOrBlank()) {
         // Ключ чата: PREFIX_ID_ADDRESS. addressId - всегда последний элемент.
         chatId.split("_").lastOrNull()?.toLongOrNull() ?: 0L
      } else {
         selectedUser?.addressId ?: 0L
      }

      if (isResident && targetAddrId != 0L && targetAddrId != baseUIState.addressId) {
        apartmentScreenModel.setAddressId(targetAddrId)
        chatScreenModel.selectUserByAddressId(targetAddrId)
      }
    }

    ChatScreenStateful(
      screenModel = chatScreenModel,
      baseUIState = baseUIState,
      onBackClick = {
        onBackClick()
        navigator.pop()
      }
    )
  }
}

@Composable
fun ChatScreenStateful(
  screenModel: ChatScreenModel,
  baseUIState: BaseUIState,
  onBackClick: () -> Unit
) {
  val navigator = LocalNavigator.currentOrThrow
  val selectedUser by screenModel.selectedUser.collectAsState()

  val chatUid = remember(baseUIState.userRole, selectedUser, baseUIState.uid) {
    if (baseUIState.userRole == UserRole.StandardUser) {
      baseUIState.uid ?: ""
    } else {
      selectedUser?.uid ?: ""
    }
  }

  ChatScreenContent(
    userEntity = selectedUser ?: UserEntity(),
    screenModel = screenModel,
    baseUIState = baseUIState,
    chatUid = chatUid,
    navigateBack = onBackClick,
    navigateToSendImageScreen = {
      val path = screenModel.selectedImagePath.value ?: ""
      val addressText = baseUIState.address ?: "м. Южне"
      val pathFromModel = screenModel.activeChatPath
      navigator.push(SendImageScreen(imagePath = path, address = addressText, chatId = pathFromModel))
    },
    navigateToCameraScreen = {
      navigator.push(CameraScreenDest())
    },
    navigateToImageDetailScreen = { message ->
      screenModel.setSelectedMessage(message)
      navigator.push(ImageDetailScreen(screenModel = screenModel))
    }
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreenContent(
  modifier: Modifier = Modifier,
  userEntity: UserEntity,
  screenModel: ChatScreenModel,
  baseUIState: BaseUIState,
  navigateBack: () -> Unit,
  navigateToSendImageScreen: () -> Unit,
  chatUid: String,
  navigateToCameraScreen: () -> Unit,
  navigateToImageDetailScreen: (MessageEntity) -> Unit
) {
  val messageText by screenModel.messageText.collectAsState()
  val messageList by screenModel.firebaseTest.collectAsState()
  val aiAssistantResponse by screenModel.assistantResponse.collectAsState()
  val isOpponentTyping by screenModel.isOpponentTyping.collectAsState()
  val isForwardingMode by screenModel.isForwardingMode.collectAsState()
  val editingMessage by screenModel.editingMessage.collectAsState()
  val selectedService by screenModel.selectedService.collectAsState()
  val selectedServicePrefix by screenModel.selectedServicePrefix.collectAsState()
  val isLoadingAfterSending by screenModel.isLoadingAfterSending.collectAsState()

  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  var isFirstLoad by remember { mutableStateOf(true) } 
  val myUid = baseUIState.uid.toString()
  val filePicker = rememberFilePicker()

  val chatItems = remember(messageList, myUid) {
    val filtered = messageList.filter { !it.deletedFor.contains(myUid) }
    val grouped = filtered.groupBy { com.ykis.ykismobkmp.core.utils.formatDateFull(it.timestamp) }
    val result = mutableListOf<ChatItem>()
    grouped.forEach { (date, messages) ->
        result.add(ChatItem.DateHeader(date))
        result.addAll(messages.map { ChatItem.MessageItem(it) })
    }
    result.reversed() 
  }

  LaunchedEffect(baseUIState.addressId, baseUIState.userRole, chatUid, userEntity.uid, baseUIState.osbbId) {
    val role = baseUIState.userRole
    val addrId = if (role == UserRole.StandardUser) baseUIState.addressId else userEntity.addressId
    val osbbId = baseUIState.osbbId ?: 0L
    if (role != UserRole.Unknown && addrId > 0L) {
      screenModel.readFromDatabase(role, osbbId, addrId)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      screenModel.clearCurrentChatPath()
      screenModel.cancelForwarding()
    }
  }

  LaunchedEffect(listState.canScrollForward, chatItems.size) {
    if (!listState.canScrollForward && chatItems.isNotEmpty() && !isFirstLoad) {
        screenModel.loadMoreMessages()
    }
  }

  LaunchedEffect(chatItems.size) {
    if (chatItems.isNotEmpty()) {
      if (isFirstLoad) {
        listState.scrollToItem(0)
        isFirstLoad = false
      } else {
        kotlinx.coroutines.yield() 
        listState.animateScrollToItem(0)
      }
    }
  }

  // ВЫЧИСЛЕНИЕ ТЕКУЩЕЙ ДАТЫ ДЛЯ ПЛАВАЮЩЕГО ЗАГОЛОВКА (Исправлено для reverseLayout)
  val currentVisibleDate by remember {
    derivedStateOf {
      val info = listState.layoutInfo.visibleItemsInfo
      if (info.isNotEmpty()) {
        // В reverseLayout верхний элемент на экране имеет МАКСИМАЛЬНЫЙ индекс
        val topItem = info.maxByOrNull { it.index }
        topItem?.let { 
           val dataIndex = it.index - 1 // Учитываем Spacer
           if (dataIndex in chatItems.indices) {
               val item = chatItems[dataIndex]
               when (item) {
                   is ChatItem.DateHeader -> item.date
                   is ChatItem.MessageItem -> com.ykis.ykismobkmp.core.utils.formatDateFull(item.message.timestamp)
               }
           } else null
        }
      } else null
    }
  }

  val appBarTitle = when {
    isForwardingMode -> "Переслати повідомлення"
    baseUIState.userRole == UserRole.StandardUser -> {
      when(selectedServicePrefix) {
          "WATER_SERVICE"   -> stringResource(Res.string.vodokanal)
          "WARM_SERVICE"    -> stringResource(Res.string.ytke)
          "GARBAGE_SERVICE" -> stringResource(Res.string.yzhtrans)
          "OSBB" -> {
              // ИСПРАВЛЕНО: Для жильца с несколькими квартирами всегда берем 
              // название конкретного ОСББ из активного профиля квартиры
              baseUIState.osbb.takeIf { it.isNotBlank() && it != "0" } ?: "Мій ОСББ"
          }
          else -> {
              selectedService?.name?.takeIf { it.isNotBlank() } 
                ?: if (baseUIState.osbb.isNotBlank()) baseUIState.osbb else "Чат"
          }
      }
    }
    else -> userEntity.displayName?.substringBefore("|")?.trim() ?: "Чат"
  }

  val appBarSubtitle = remember(baseUIState.address, userEntity, isOpponentTyping, baseUIState.userRole) {
    when {
      isOpponentTyping -> "друкує..."
      baseUIState.userRole == UserRole.StandardUser -> baseUIState.address ?: ""
      else -> "о/р: ${userEntity.addressId}"
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    topBar = {
      Column {
        DefaultAppBar(
          title = appBarTitle,
          subtitle = appBarSubtitle,
          canNavigateBack = true,
          onBackClick = {
            keyboardController?.hide()
            focusManager.clearFocus()
            if (isForwardingMode) screenModel.cancelForwarding() else navigateBack()
          }
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
      }
    }
  ) { innerPadding ->
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val messageToDelete by screenModel.messageToDelete.collectAsState()

    if (messageToDelete != null) {
      MessageActionsDialog(
        messageToDelete = messageToDelete,
        myUid = myUid,
        screenModel = screenModel,
        navigateBack = { if (screenModel.forwardingMessage.value != null) navigateBack() }
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding()) // Только от шапки
    ) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        // 1. СПИСОК СООБЩЕНИЙ
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          state = listState,
          reverseLayout = true,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Невидимый элемент-прокладка над меню (индекс 0 внизу)
          item { Spacer(modifier = Modifier.height(20.dp)) }

          chatItems.forEach { chatItem ->
            when (chatItem) {
              is ChatItem.DateHeader -> {
                item(key = "header_${chatItem.date}") { DateChip(date = chatItem.date) }
              }
              is ChatItem.MessageItem -> {
                val msg = chatItem.message
                item(key = "msg_${msg.id}") {
                  MessageListItem(
                    isUserAdmin = baseUIState.userRole != UserRole.StandardUser,
                    messageEntity = msg,
                    onLongClick = { screenModel.showDeleteConfirmation(msg) },
                    onClick = {
                      keyboardController?.hide()
                      navigateToImageDetailScreen(msg)
                    },
                    onFileClick = { fileUrl -> try { uriHandler.openUri(fileUrl) } catch (e: Exception) {} }
                  )
                }
              }
            }
          }
        }

        // 2. ПЛАВАЮЩИЙ ЗАГОЛОВОК ДАТЫ (Telegram Style)
        val floatingDate by remember(chatItems) {
          derivedStateOf {
            val items = listState.layoutInfo.visibleItemsInfo
            if (items.isNotEmpty() && chatItems.isNotEmpty()) {
              val topVisible = items.maxByOrNull { it.index }
              topVisible?.let { info ->
                val actualIdx = info.index - 1
                if (actualIdx in chatItems.indices) {
                  when (val data = chatItems[actualIdx]) {
                    is ChatItem.DateHeader -> data.date
                    is ChatItem.MessageItem -> com.ykis.ykismobkmp.core.utils.formatDateFull(data.message.timestamp)
                  }
                } else null
              }
            } else null
          }
        }

        // 2. ПОСТОЯННО ВИДИМЫЙ ПЛАВАЮЩИЙ ЗАГОЛОВОК ДАТЫ
        Surface(
           color = Color.Black.copy(alpha = 0.4f), // Постоянная полупрозрачность
           shape = RoundedCornerShape(16.dp),
           modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
           tonalElevation = 4.dp
        ) {
           Text(
               text = floatingDate ?: "Загрузка...",
               modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
               style = MaterialTheme.typography.labelSmall,
               color = Color.White
           )
        }
      }

      // 3. МЕНЮ ВВОДА
      Surface(
        color = MaterialTheme.colorScheme.surface, 
        tonalElevation = 3.dp, 
        modifier = Modifier.fillMaxWidth().imePadding() // Убрал navigationBarsPadding для плотного прилегания
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          AnimatedVisibility(visible = !aiAssistantResponse.isNullOrBlank()) {
            AiHintCard(
              text = aiAssistantResponse ?: "",
              title = if (baseUIState.userRole != UserRole.StandardUser) "Порада" else "Помічник",
              onClose = { screenModel.clearAiSuggestion() },
              onApply = { screenModel.applyAiHint() }
            )
          }
          ComposeMessageBox(
            text = messageText,
            onTextChanged = { screenModel.onMessageTextChanged(it) },
            onSent = {
              if (editingMessage != null) screenModel.updateMessage(messageText)
              else screenModel.handleSendMessage(baseUIState)
            },
            onImageSent = { path ->
                if (path.isNotBlank()) {
                    println("[YkisLogKMP.ChatScreen]: Знімок з камери або файл отримано")
                    screenModel.setSelectedImagePath(path)
                    if (baseUIState.userRole == UserRole.StandardUser && path.contains("image")) {
                        screenModel.analyzePhotoWithGemini(path, baseUIState.address)
                    }
                    navigateToSendImageScreen()
                }
            },
            onAiClick = {
              if (messageText.isNotBlank()) {
                screenModel.askAssistant(messageText, baseUIState.userRole, baseUIState.address ?: "Южне")
              }
            },
            onCameraClick = { navigateToCameraScreen() },
            isLoading = isLoadingAfterSending,
            canSend = messageText.isNotBlank() || editingMessage != null,
            filePicker = filePicker // Пряма передача об'єкта для Web-сумісності
          )
        }
      }
    }
  }
}

@Composable
fun MessageActionsDialog(messageToDelete: MessageEntity?, myUid: String, screenModel: ChatScreenModel, navigateBack: () -> Unit) {
  if (messageToDelete == null) return
  val isMyMessage = messageToDelete.senderUid == myUid

  AlertDialog(
    onDismissRequest = { screenModel.dismissDeleteDialog() },
    title = { Text(text = stringResource(Res.string.message_actions_title)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isMyMessage && messageToDelete.imageUrl == null) {
          TextButton(onClick = { screenModel.startEditing(messageToDelete); screenModel.dismissDeleteDialog() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, null); Spacer(Modifier.width(12.dp)); Text(stringResource(Res.string.edit), modifier = Modifier.weight(1f))
          }
        }
        TextButton(onClick = { screenModel.startForwarding(messageToDelete); screenModel.dismissDeleteDialog(); navigateBack() }, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.graphicsLayer(scaleX = -1f)); Spacer(Modifier.width(12.dp)); Text(stringResource(Res.string.forward), modifier = Modifier.weight(1f))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        TextButton(onClick = { screenModel.deleteForMe(messageToDelete.id); screenModel.dismissDeleteDialog() }, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(12.dp)); Text(stringResource(Res.string.delete_for_me), modifier = Modifier.weight(1f))
        }
        if (isMyMessage) {
          TextButton(onClick = { screenModel.confirmDeletion() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(12.dp)); Text(text = stringResource(Res.string.delete_for_everyone), color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = { screenModel.dismissDeleteDialog() }) { Text(stringResource(Res.string.cancel)) } }
  )
}

@Composable
fun AiHintCard(text: String, title: String, onClose: () -> Unit, onApply: () -> Unit) {
  Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.SmartToy, "AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Close", modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f)) }
      }
      Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(text = stringResource(Res.string.apply_suggestion), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onApply() }.padding(top = 4.dp))
      }
    }
  }
}

@Composable
fun DateChip(date: String) {
  Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp)) {
      Text(text = date, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
  }
}
