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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.core.utils.formatDateFull
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.CameraScreenDest
import com.ykis.ykismobkmp.ui.navigation.ImageDetailScreenDest
import com.ykis.ykismobkmp.ui.navigation.NavigationType
import com.ykis.ykismobkmp.ui.navigation.SendImageScreenDest
import org.jetbrains.compose.resources.stringResource
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.apply_suggestion
import ykismobkmp.composeapp.generated.resources.cancel
import ykismobkmp.composeapp.generated.resources.delete_for_everyone
import ykismobkmp.composeapp.generated.resources.delete_for_me
import ykismobkmp.composeapp.generated.resources.edit
import ykismobkmp.composeapp.generated.resources.forward
import ykismobkmp.composeapp.generated.resources.message_actions_title

private const val className = "ChatScreen"
sealed class ChatItem {
  data class DateHeader(val date: String) : ChatItem()
  data class MessageItem(val message: MessageEntity) : ChatItem()
}
@Composable
fun ChatScreenStateful(
  screenModel: ChatScreenModel,
  baseUIState: BaseUIState,
  navigationType: NavigationType,
  onBackClick: () -> Unit // ИСПРАВЛЕНО: Callback для безопасного возврата в String-роутер Хаба
) {
  val navigator = LocalNavigator.currentOrThrow

  // Получаем выбранного пользователя из ScreenModel
  val selectedUser by screenModel.selectedUser.collectAsState()

  // --- ЗОЛОТОЙ ФОНД ЛОГИКИ: Вычисление UID ---
  val chatUid = remember(baseUIState.userRole, selectedUser, baseUIState.uid) {
    val uid = if (baseUIState.userRole == UserRole.StandardUser) {
      baseUIState.uid?.toString() ?: ""
    } else {
      selectedUser?.uid ?: ""
    }
    println("[ChatScreen.Stateful]: [RE-CALC] UID: ${uid.takeLast(5)}")
    uid
  }

  ChatScreenContent(
    userEntity = selectedUser ?: UserEntity(),
    screenModel = screenModel,
    baseUIState = baseUIState,
    navigationType = navigationType,
    chatUid = chatUid,
    navigateBack = onBackClick, // ИСПРАВЛЕНО: Возвращаемся на UserListScreen без падения стека Voyager

    // ИСПРАВЛЕНО НАМЕРТВО: Сняты заглушки навигации через реестр ScreensRegistry
    navigateToSendImageScreen = {
      navigator.push(SendImageScreenDest)
    },
    navigateToCameraScreen = {
      navigator.push(CameraScreenDest)
    },
    navigateToImageDetailScreen = { message ->
      screenModel.setSelectedMessage(message)
      // Передаем URL медиафайла в типизированный роут Voyager
      navigator.push(ImageDetailScreenDest(imageUrl = message.imageUrl ?: ""))
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
  navigationType: NavigationType,
  navigateBack: () -> Unit,
  navigateToSendImageScreen: () -> Unit,
  chatUid: String,
  navigateToCameraScreen: () -> Unit,
  navigateToImageDetailScreen: (MessageEntity) -> Unit
) {
  // --- ПОЛУЧЕНИЕ СОСТОЯНИЙ (KMP) ---
  val messageText by screenModel.messageText.collectAsState()
  val messageList by screenModel.firebaseTest.collectAsState()
  val selectedService by screenModel.selectedService.collectAsState()
  val isLoadingAfterSending by screenModel.isLoadingAfterSending.collectAsState()
  val aiAssistantResponse by screenModel.assistantResponse.collectAsState()
  val isPartnerTyping by screenModel.isOpponentTyping.collectAsState() // Исправили имя
  val isForwardingMode by screenModel.uiState.collectAsState().let {
    remember { derivedStateOf { it.value.isForwarding } }
  }
  val editingMessage by screenModel.editingMessage.collectAsState()
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  val screenScope = rememberCoroutineScope()
  val myUid = baseUIState.uid.toString()

  // --- 1. ЛОГИКА ФОРМИРОВАНИЯ КОНТЕНТА (Золотой фонд) ---
  val currentChatOsbbId = remember(baseUIState.userRole, baseUIState.osbbId) {
    when (baseUIState.userRole) {
      UserRole.YtkeUser -> 9998
      UserRole.VodokanalUser -> 9999
      UserRole.TboUser -> 9997
      UserRole.OsbbUser -> baseUIState.osbbId ?: 0
      else -> baseUIState.osmdId ?: 0
    }
  }

  // Группировка сообщений по датам
  // указываем тип List<ChatItem>, чтобы Compose точно знал, что лежит в списке
  val chatItems = remember<List<ChatItem>>(messageList, myUid) {
    Log.d("YkisLog", "[$className.Filter]: Processing ${messageList.size} messages")

    val filtered = messageList.filter { msg ->
      val deletedList = msg.deletedFor ?: emptyList()
      !deletedList.contains(myUid)
    }

    filtered.groupBy { com.ykis.ykismobkmp.core.utils.formatDateFull(it.timestamp) }
      .flatMap { (date, messages) ->
        // Здесь формируется смешанный список из заголовков дат и самих сообщений
        listOf(ChatItem.DateHeader(date)) + messages.map { ChatItem.MessageItem(it) }
      }
  }


  // --- 2. ЭФФЕКТЫ ЗАГРУЗКИ ---
  LaunchedEffect(baseUIState.addressId, baseUIState.userRole, chatUid, userEntity.uid) {
    val role = baseUIState.userRole
    val addrId =
      if (role == UserRole.StandardUser) baseUIState.addressId else userEntity.addressId ?: 0
    val targetUid = if (role == UserRole.StandardUser) chatUid else userEntity.uid ?: ""

    if (role != UserRole.Unknown && addrId > 0 && targetUid.isNotBlank()) {
      screenModel.readFromDatabase(
        role = role,
        senderUid = targetUid,
        osbbId = currentChatOsbbId.toInt(),
        addressId = addrId.toInt()
      )
    }
  }

  // Очистка статусов при выходе
  DisposableEffect(Unit) {
    onDispose {
      screenModel.setTypingStatus(false)
      screenModel.cancelForwarding()
    }
  }

  // Авто-скролл
  LaunchedEffect(chatItems.size) {
    if (chatItems.isNotEmpty()) {
      listState.animateScrollToItem(chatItems.size - 1)
    }
  }
// --- ЛОГИКА ЗАГОЛОВКА (TITLE) ---
  val appBarTitle = remember<String>(baseUIState.osbb, selectedService, isForwardingMode, baseUIState.userRole, userEntity) {
    when {
      isForwardingMode -> "Переслати повідомлення"
      baseUIState.userRole == UserRole.StandardUser -> {
        val serviceName = selectedService?.name ?: ""
        if (serviceName.contains("ОСББ", ignoreCase = true)) {
          baseUIState.osbb ?: "ОСББ"
        } else {
          serviceName
        }
      }
      else -> {
        // АДМИН: Основной заголовок — это АДРЕС (берем часть ДО черты '|')
        userEntity.displayName?.substringBefore("|")?.trim() ?: "Чат"
      }
    }
  }

// --- ЛОГИКА ПОДЗАГОЛОВКА (SUBTITLE) ---
  val appBarSubtitle = remember<String>(baseUIState.address, userEntity, isPartnerTyping, baseUIState.userRole) {
    when {
      isPartnerTyping -> "друкує..."
      baseUIState.userRole == UserRole.StandardUser -> baseUIState.address ?: ""
      else -> {
        // АДМИН: Подзаголовок — это ЛИЦЕВОЙ СЧЕТ (о/р)
        val accountNum = userEntity.addressId.toInt()
        if (accountNum != 0) {
          "о/р: $accountNum"
        } else {
          userEntity.displayName?.substringAfter("|")?.trim() ?: ""
        }
      }
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
          },
          navigationType = navigationType
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
      }
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .navigationBarsPadding()
          .imePadding()
      ) {
        AnimatedVisibility(visible = !aiAssistantResponse.isNullOrBlank()) {
          AiHintCard(
            text = aiAssistantResponse ?: "",
            title = if (baseUIState.userRole != UserRole.StandardUser) "Порада диспетчеру" else "Помічник",
            onClose = { screenModel.clearAiSuggestion() },
            onApply = {
              println("[ChatScreen.AiApply]: Applying suggestion to input")
              screenModel.applyAiHint()
            }
          )
        }
        Surface(tonalElevation = 6.dp) {
          ComposeMessageBox(
            text = messageText,
            onTextChanged = { screenModel.onMessageTextChanged(it) },
            onSent = {
              if (editingMessage != null) {
                println("[ChatScreen.onSent]: Updating message")
                screenModel.updateMessage(messageText)
              } else {
                val curAddrId = if (baseUIState.userRole == UserRole.StandardUser)
                  baseUIState.addressId else userEntity.addressId
                val curAddr = if (baseUIState.userRole == UserRole.StandardUser)
                  (baseUIState.address ?: "") else (userEntity.displayName ?: "")
                println("[ChatScreen.onSent]: Sending message to $curAddr (ID: $curAddrId)")
                screenModel.writeToDatabase(
                  chatUid = chatUid,
                  senderUid = myUid,
                  senderDisplayedName = baseUIState.displayName ?: "Користувач",
                  senderLogoUrl = baseUIState.photoUrl,
                  senderAddress = curAddr,
                  addressId = curAddrId.toInt(),
                  imageUrl = null,
                  fileUrl = null,
                  fileName = null,
                  osbbId = currentChatOsbbId.toInt(),
                  role = baseUIState.userRole,
                  recipientTokens = userEntity.tokens ?: emptyList(),
                  onComplete = {
                    println("[ChatScreen.onSent]: Success, clearing suggestion")
                    screenModel.clearAiSuggestion()
                  }
                )
              }
            },
            onImageSent = { path ->
              println("[ChatScreen.onImageSent]: Attach path: $path")
              screenModel.setSelectedImagePath(path)
              if (baseUIState.userRole == UserRole.StandardUser) {
                println("[ChatScreen.Gemini]: Auto-analyzing photo")
                screenModel.analyzePhotoWithGemini(path, baseUIState.address ?: "")
              }
              navigateToSendImageScreen()
            },
            onAiClick = {
              if (messageText.isNotBlank()) {
                println("[ChatScreen.onAiClick]: Requesting text assistant")
                screenModel.askAssistant(messageText)
              }
            },
            onCameraClick = {
              println("[ChatScreen.onCamera]: Opening camera")
              navigateToCameraScreen()
            },
            isLoading = isLoadingAfterSending,
            canSend = messageText.isNotBlank() || editingMessage != null
          )
        }
      }
    }
  ) { innerPadding ->
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding())
        .imePadding(),
      state = listState,
      contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      chatItems.forEach { chatItem ->
        when (chatItem) {
          is ChatItem.DateHeader -> {
            stickyHeader(key = "date_${chatItem.date}") {
              DateChip(date = chatItem.date)
            }
          }

          is ChatItem.MessageItem -> {
            val msg = chatItem.message
            val uniqueKey = "msg_${msg.id}_${msg.timestamp}"
            item(key = uniqueKey) {
              MessageListItem(
                uid = myUid,
                isUserAdmin = baseUIState.userRole != UserRole.StandardUser,
                messageEntity = msg,
                onLongClick = {
                  println("[ChatScreen.onLongClick]: Message ID: ${msg.id}")
                  screenModel.showDeleteConfirmation(msg)
                },
                onClick = {
                  keyboardController?.hide()
                  navigateToImageDetailScreen(msg)
                },
                onFileClick = { fileUrl ->
                  try {
                    uriHandler.openUri(fileUrl)
                  } catch (e: Exception) {
                    println("[ChatScreen.UriError]: ${e.message}")
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}
  @Composable
  fun MessageActionsDialog(
    messageToDelete: MessageEntity?,
    myUid: String,
    screenModel: ChatScreenModel, // Наш новый ScreenModel
    navigateBack: () -> Unit
  ) {
    if (messageToDelete == null) return
    val isMyMessage = messageToDelete.senderUid == myUid

    AlertDialog(
      onDismissRequest = { screenModel.dismissDeleteDialog() },
      title = {
        Text(
          text = stringResource(Res.string.message_actions_title),
          style = MaterialTheme.typography.titleMedium
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. РЕДАКТИРОВАНИЕ (Только свои текстовые сообщения)
          if (isMyMessage && messageToDelete.imageUrl == null) {
            TextButton(
              onClick = {
                println("[ChatScreen.Edit]: Message ID ${messageToDelete.id}")
                screenModel.startEditing(messageToDelete)
                screenModel.dismissDeleteDialog()
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Edit, contentDescription = null)
              Spacer(Modifier.width(12.dp))
              Text(stringResource(Res.string.edit), modifier = Modifier.weight(1f))
            }
          }

          // 2. ПЕРЕСЫЛКА (Любое сообщение)
          TextButton(
            onClick = {
              println("[ChatScreen.Forward]: Selecting recipient for ${messageToDelete.id}")
              screenModel.startForwarding(messageToDelete)
              screenModel.dismissDeleteDialog()
              // Возвращаемся в список чатов для выбора получателя
              navigateBack()
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Reply,
              modifier = Modifier.graphicsLayer(scaleX = -1f),
              contentDescription = null
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.forward), modifier = Modifier.weight(1f))
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          // 3. УДАЛИТЬ У МЕНЯ
          TextButton(
            onClick = {
              println("[ChatScreen.DeleteLocal]: ID ${messageToDelete.id}")
              screenModel.deleteForMe(messageToDelete.id)
              screenModel.dismissDeleteDialog()
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.delete_for_me), modifier = Modifier.weight(1f))
          }

          // 4. УДАЛИТЬ У ВСЕХ (Только свои)
          if (isMyMessage) {
            TextButton(
              onClick = {
                println("[ChatScreen.DeleteGlobal]: ID ${messageToDelete.id}")
                screenModel.confirmDeletion()
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
              )
              Spacer(Modifier.width(12.dp))
              Text(
                text = stringResource(Res.string.delete_for_everyone),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { screenModel.dismissDeleteDialog() }) {
          Text(stringResource(Res.string.cancel))
        }
      }
    )
  }

  @Composable
  fun AiHintCard(
    text: String,
    title: String,
    onClose: () -> Unit,
    onApply: () -> Unit
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 4.dp,
      shadowElevation = 2.dp
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI Hint",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.weight(1f))
          IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f)
            )
          }
        }
        Text(
          text = text,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(vertical = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
          Text(
            text = stringResource(Res.string.apply_suggestion),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clickable {
                println("[AiHintCard.onApply]: Applying Gemini hint")
                onApply()
              }
              .padding(top = 4.dp)
          )
        }
      }
    }
  }

  @Composable
  fun DateChip(date: String) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(
          text = date,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
    }
  }



