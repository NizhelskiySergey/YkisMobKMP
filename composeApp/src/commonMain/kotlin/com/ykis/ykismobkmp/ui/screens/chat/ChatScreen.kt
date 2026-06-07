package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.animation.*
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.apply_suggestion
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.delete_for_everyone
import ykismobkmp.composeapp.generated.resources.delete_for_me
import ykismobkmp.composeapp.generated.resources.edit
import ykismobkmp.composeapp.generated.resources.forward
import ykismobkmp.composeapp.generated.resources.message_actions_title

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
         chatId.split("_").getOrNull(chatId.split("_").size - 2)?.toLongOrNull() ?: 0L
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
      navigator.push(SendImageScreen(imagePath = path, address = addressText))
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
  val selectedService by screenModel.selectedService.collectAsState()
  val isLoadingAfterSending by screenModel.isLoadingAfterSending.collectAsState()
  val aiAssistantResponse by screenModel.assistantResponse.collectAsState()
  val isOpponentTyping by screenModel.isOpponentTyping.collectAsState()
  val isForwardingMode by screenModel.isForwardingMode.collectAsState()
  val editingMessage by screenModel.editingMessage.collectAsState()
  val selectedServicePrefix by screenModel.selectedServicePrefix.collectAsState()

  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  val myUid = baseUIState.uid.toString()
  val filePicker = rememberFilePicker()

  val chatItems = remember(messageList, myUid) {
    println("[YkisLogKMP.ChatScreen]: [RENDER_TRACE] Обробка ${messageList.size} повідомлень для LazyColumn")
    messageList.filter { msg ->
      !msg.deletedFor.contains(myUid)
    }.groupBy { com.ykis.ykismobkmp.core.utils.formatDateFull(it.timestamp) }
      .flatMap { (date, messages) ->
        listOf(ChatItem.DateHeader(date)) + messages.map { ChatItem.MessageItem(it) }
      }
  }

  val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
  LaunchedEffect(imeHeight) {
    if (imeHeight > 0.dp && chatItems.isNotEmpty()) {
      listState.animateScrollToItem(chatItems.size - 1)
    }
  }

  LaunchedEffect(baseUIState.addressId, baseUIState.userRole, chatUid, userEntity.uid, baseUIState.osbbId) {
    val role = baseUIState.userRole
    val addrId = if (role == UserRole.StandardUser) baseUIState.addressId else userEntity.addressId
    val osbbId = baseUIState.osbbId ?: 0L

    if (role != UserRole.Unknown && addrId > 0L) {
      println("[YkisLogKMP.ChatScreen]: Запуск підписки з UI. Role: $role, OSBB: $osbbId, Addr: $addrId")
      screenModel.readFromDatabase(role, osbbId, addrId)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      screenModel.clearCurrentChatPath()
      screenModel.cancelForwarding()
    }
  }

  LaunchedEffect(chatItems.size) {
    if (chatItems.isNotEmpty()) {
      listState.animateScrollToItem(chatItems.size - 1)
    }
  }

  val appBarTitle = remember(baseUIState.osbb, selectedService, selectedServicePrefix, isForwardingMode, baseUIState.userRole, userEntity) {
    when {
      isForwardingMode -> "Переслати повідомлення"
      baseUIState.userRole == UserRole.StandardUser -> {
        // ИСПРАВЛЕНО: Гарантируем название службы в заголовке для жителя
        selectedService?.name ?: when(selectedServicePrefix) {
            "WATER_SERVICE"   -> "КП \"ЮЖВОДОКАНАЛ\""
            "WARM_SERVICE"    -> "КП тм \"ЮТКЕ\""
            "GARBAGE_SERVICE" -> "КП \"СПЕЦТРАНС\""
            else              -> if (baseUIState.osbb.isNotBlank()) baseUIState.osbb else "ОСББ"
          }
      }
      else -> userEntity.displayName?.substringBefore("|")?.trim() ?: "Чат"
    }
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
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
    },
    bottomBar = {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
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
              if (path.isBlank()) {
                filePicker.pickFile { pickedPath ->
                  screenModel.setSelectedImagePath(pickedPath)
                  navigateToSendImageScreen()
                }
              } else {
                screenModel.setSelectedImagePath(path)
                if (baseUIState.userRole == UserRole.StandardUser) screenModel.analyzePhotoWithGemini(path, baseUIState.address)
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
            canSend = messageText.isNotBlank() || editingMessage != null
          )
        }
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

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
      state = listState,
      contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      chatItems.forEach { chatItem ->
        when (chatItem) {
          is ChatItem.DateHeader -> {
            stickyHeader(key = "date_${chatItem.date}") { DateChip(date = chatItem.date) }
          }
          is ChatItem.MessageItem -> {
            val msg = chatItem.message
            item(key = "msg_${msg.id}") {
              MessageListItem(
                uid = myUid,
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
