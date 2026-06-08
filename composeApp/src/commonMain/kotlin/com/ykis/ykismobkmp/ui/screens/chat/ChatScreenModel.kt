package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.applyAppBadgeCount
import com.ykis.ykismobkmp.core.utils.currentTimeMillis
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.ledger.list.TotalServiceDebt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.success_send_message

private const val tag = "ChatViewModel"

class ChatScreenModel(
  private val chatRepo: ChatRepository,
  val firebaseService: FirebaseService,
  logService: LogService
) : BaseScreenModel(logService) {

  private val _assistantResponse = MutableStateFlow<String?>(null)
  val assistantResponse: StateFlow<String?> = _assistantResponse.asStateFlow()

  private val _isAssistantLoading = MutableStateFlow(false)
  val isAssistantLoading: StateFlow<Boolean> = _isAssistantLoading.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _userIdentifiersWithRole = MutableStateFlow<List<String>>(emptyList())
  private val _rawFetchedProfiles = MutableStateFlow<List<UserEntity>>(emptyList())

  private val _selectedUser = MutableStateFlow<UserEntity?>(null)
  val selectedUser: StateFlow<UserEntity?> = _selectedUser.asStateFlow()

  private val _selectedService = MutableStateFlow<TotalServiceDebt?>(null)
  val selectedService: StateFlow<TotalServiceDebt?> = _selectedService.asStateFlow()

  private val _selectedServicePrefix = MutableStateFlow("OSBB")
  val selectedServicePrefix: StateFlow<String> = _selectedServicePrefix.asStateFlow()

  private val _messageText = MutableStateFlow("")
  val messageText: StateFlow<String> = _messageText.asStateFlow()

  private val _isLoadingAfterSending = MutableStateFlow(false)
  val isLoadingAfterSending: StateFlow<Boolean> = _isLoadingAfterSending.asStateFlow()

  private val _editingMessage = MutableStateFlow<MessageEntity?>(null)
  val editingMessage: StateFlow<MessageEntity?> = _editingMessage.asStateFlow()

  private val _messageToDelete = MutableStateFlow<MessageEntity?>(null)
  val messageToDelete: StateFlow<MessageEntity?> = _messageToDelete.asStateFlow()

  private val _selectedImagePath = MutableStateFlow<String?>(null)
  val selectedImagePath: StateFlow<String?> = _selectedImagePath.asStateFlow()

  private val _selectedMessage = MutableStateFlow<MessageEntity?>(null)
  val selectedMessage: StateFlow<MessageEntity?> = _selectedMessage.asStateFlow()

  private val _firebaseTest = MutableStateFlow<List<MessageEntity>>(emptyList())
  val firebaseTest: StateFlow<List<MessageEntity>> = _firebaseTest.asStateFlow()

  private val _lastMessages = MutableStateFlow<Map<String, MessageEntity>>(emptyMap())
  val lastMessages: StateFlow<Map<String, MessageEntity>> = _lastMessages.asStateFlow()

  private val _isOpponentOnline = MutableStateFlow(false)
  val isOpponentOnline: StateFlow<Boolean> = _isOpponentOnline.asStateFlow()

  private val _isOpponentTyping = MutableStateFlow(false)
  val isOpponentTyping: StateFlow<Boolean> = _isOpponentTyping.asStateFlow()

  private val _globalTypingStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val globalTypingStatuses: StateFlow<Map<String, Boolean>> = _globalTypingStatuses.asStateFlow()

  private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
  val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

  private val _recipientTokens = MutableStateFlow<List<String>>(emptyList())
  val recipientTokens: StateFlow<List<String>> = _recipientTokens.asStateFlow()
  
  private val _recipientUids = MutableStateFlow<List<String>>(emptyList())
  val recipientUids: StateFlow<List<String>> = _recipientUids.asStateFlow()

  private val _pendingPushChatId = MutableStateFlow<String?>(null)
  val pendingPushChatId: StateFlow<String?> = _pendingPushChatId.asStateFlow()

  private val _forwardingMessage = MutableStateFlow<MessageEntity?>(null)
  val forwardingMessage = _forwardingMessage.asStateFlow()

  val isForwardingMode = _forwardingMessage
    .map { it != null }
    .stateIn(screenModelScope, SharingStarted.Lazily, false)

  private var currentChatPath: String? = null
  private var messageSubscriptionJob: Job? = null
  private var typingStatusJob: Job? = null
  private var presenceJob: Job? = null
  private var activeTrackerJob: Job? = null
  private var lastMessageListeners = mutableMapOf<String, Job>()
  
  private var unreadSubscriptionJob: Job? = null

  companion object {
    var activeChatIdForNotifications: String? = null
  }

  /**
   * [userList] — Синхронізований список чатів квартир.
   */
  val userList: StateFlow<List<UserEntity>> = combine(
    _userIdentifiersWithRole,
    _rawFetchedProfiles,
    _lastMessages,
    _searchQuery
  ) { keys, profiles, lastMsgs, query ->
    val fullList = keys.mapNotNull { key ->
      val parts = key.split("_")
      if (parts.size < 3) return@mapNotNull null

      val addrIdFromKey = parts.last().toLongOrNull() ?: 0L
      val profile = profiles.find { it.addressId == addrIdFromKey }
      val lastMsg = lastMsgs[key]

      // ГАРАНТИРУЕМ АДРЕС В ЗАГОЛОВКЕ
      val finalAddress = when {
        profile != null && !profile.address.isNullOrBlank() -> profile.address
        !lastMsg?.senderAddress.isNullOrBlank() -> lastMsg?.senderAddress ?: ""
        else -> "Квартира (о/р $addrIdFromKey)"
      }
      
      // ГАРАНТИРУЕМ ИМЯ ЖИЛЬЦА В ПОДЗАГОЛОВКЕ
      val finalResidentName = when {
        profile != null && !profile.displayName.isNullOrBlank() -> {
            // Если в профиле имя уже "Адрес | Фамилия", берем только Фамилию
            profile.displayName.substringAfter("|").trim()
        }
        !lastMsg?.senderDisplayedName.isNullOrBlank() && lastMsg?.senderUid != firebaseService.uid -> {
            lastMsg?.senderDisplayedName?.substringAfter("|")?.trim() ?: "Абонент"
        }
        else -> "Абонент"
      }

      UserEntity(
        uid = key, 
        addressId = addrIdFromKey,
        displayName = profile?.displayName ?: finalAddress,
        address = finalAddress,
        fio = profile?.fio ?: finalResidentName
      )
    }.distinctBy { it.addressId }

    if (query.isBlank()) {
      fullList
    } else {
      fullList.filter { user ->
        (user.displayName?.contains(query, ignoreCase = true) == true) ||
          user.addressId.toString().contains(query)
      }
    }
  }.stateIn(
    scope = screenModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun setPendingPushChatId(id: String?) {
    _pendingPushChatId.value = id
  }

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  fun onMessageTextChanged(text: String) {
    _messageText.value = text
    if (currentChatPath != null) {
      setUserTyping(text.isNotEmpty())
    }
  }

  fun setSelectedService(service: TotalServiceDebt?) {
    _selectedService.value = service
    if (service != null) {
       val name = service.name.lowercase()
       val prefix = when {
          name.contains("водоканал") -> "WATER_SERVICE"
          name.contains("тепло") -> "WARM_SERVICE"
          name.contains("сміття") || name.contains("транс") -> "GARBAGE_SERVICE"
          else -> "OSBB"
       }
       _selectedServicePrefix.value = prefix
       
       screenModelScope.launch {
          val profile = firebaseService.getUserProfile()
          trackUserIdentifiersWithRole(UserRole.fromString(profile.userRole), profile.osbbId)
       }
    }
  }

  fun onServiceSelectedForResident(prefix: String) {
     _selectedServicePrefix.value = prefix
     val serviceName = when(prefix) {
       "WATER_SERVICE" -> "Водоканал"
       "WARM_SERVICE" -> "Тепломережі"
       "GARBAGE_SERVICE" -> "Вивіз сміття"
       else -> "ОСББ"
     }
     val detail = when(prefix) {
       "WATER_SERVICE" -> ContentDetail.WATER_SERVICE
       "WARM_SERVICE" -> ContentDetail.WARM_SERVICE
       "GARBAGE_SERVICE" -> ContentDetail.GARBAGE_SERVICE
       else -> ContentDetail.OSBB
     }
     _selectedService.value = TotalServiceDebt(
        name = serviceName,
        color = Color.Gray,
        debt = 0.0,
        icon = Icons.Default.Home,
        contentDetail = detail
     )
     
     screenModelScope.launch {
        val profile = firebaseService.getUserProfile()
        trackUserIdentifiersWithRole(UserRole.fromString(profile.userRole), profile.osbbId)
     }
  }

  fun selectUserByAddressId(addressId: Long) {
     screenModelScope.launch {
        val existing = userList.value.find { it.addressId == addressId }
        if (existing != null) {
            _selectedUser.value = existing
        } else {
            val user = chatRepo.fetchUserByAddressId(addressId)
            if (user != null) {
              _selectedUser.value = user
            }
        }
     }
  }

  fun openChatWithUser(user: UserEntity, currentRole: UserRole, currentOsbbId: Long) {
    _selectedUser.value = user
    _firebaseTest.value = emptyList()
    _messageText.value = ""
    
    readFromDatabase(
      role = currentRole,
      osbbId = currentOsbbId,
      addressId = user.addressId
    )
  }

  fun getChatPath(role: UserRole, osbbId: Long, addressId: Long): String {
    val servicePrefix = _selectedServicePrefix.value ?: "OSBB"
    
    val effectiveId = if (role == UserRole.StandardUser) {
        when (servicePrefix) {
            "WATER_SERVICE" -> 9999L
            "WARM_SERVICE" -> 9998L
            "GARBAGE_SERVICE" -> 9997L
            else -> if (osbbId != 0L) osbbId else (_selectedUser.value?.osbbId ?: 0L)
        }
    } else osbbId

    return "${servicePrefix}_${effectiveId}_${addressId}"
  }

  fun readFromDatabase(role: UserRole, osbbId: Long, addressId: Long) {
    val finalOsbbId = if (osbbId == 0L) {
        when (role) {
            UserRole.VodokanalUser -> 9999L
            UserRole.YtkeUser      -> 9998L
            UserRole.TboUser       -> 9997L
            else -> osbbId
        }
    } else osbbId

    val targetPath = getChatPath(role, finalOsbbId, addressId)
    
    if (currentChatPath == targetPath && messageSubscriptionJob?.isActive == true) return

    println("[YkisLogKMP]: [READ_START] Путь: $targetPath")

    messageSubscriptionJob?.cancel()
    messageSubscriptionJob = screenModelScope.launch {
      try {
        val activeUid = chatRepo.currentUid ?: return@launch
        currentChatPath = targetPath
        activeChatIdForNotifications = targetPath
        
        // Регистрируем пользователя как участника чата
        chatRepo.addChatParticipant(targetPath, activeUid)
        // Обнуляем бэйдж при входе в чат
        chatRepo.resetUnreadCount(targetPath, activeUid)
        
        observeOpponentPresence(targetPath)
        setPresence(targetPath, true)
        observeTypingStatus(targetPath)
        firebaseService.clearNotifications(targetPath)

        // СОБИРАЕМ ТОКЕНЫ ПОЛУЧАТЕЛЕЙ ПРИ ВХОДЕ В ЧАТ (УЛУЧШЕНО)
        screenModelScope.launch {
            try {
                // Извлекаем ID организации из пути чата (предпоследний элемент)
                val parts = targetPath.split("_")
                val effectiveOrgId = if (parts.size >= 3) parts[parts.size - 2].toLongOrNull() ?: 0L else 0L

                val targetUids = if (role == UserRole.StandardUser) {
                    // Житель ищет админов КОНКРЕТНОЙ службы/ОСББ
                    println("[ChatScreenModel]: Поиск админов для организации ID: $effectiveOrgId")
                    chatRepo.fetchAdminsByOsbb(effectiveOrgId).map { it.uid }
                } else {
                    // Админ ищет ВСЕХ жильцов этой квартиры
                    // ІСПРАВЛЕНО: Ми беремо ТІЛЬКИ тих, хто реально приписаний до цієї квартири в Firestore,
                    // і додаємо тих, хто є в chat_access, АЛЕ фільтруємо активних.
                    val fromFirestore = chatRepo.fetchAllUsersByAddressId(addressId).map { it.uid }
                    val fromAccess = chatRepo.realtime.reference("chat_access/$targetPath").valueEvents.first()
                        .children.mapNotNull { it.key }

                    (fromAccess + fromFirestore).distinct().filter { it != activeUid }
                }
                
                if (targetUids.isNotEmpty()) {
                    val users = chatRepo.fetchUsersByIds(targetUids)
                    val tokens = users.flatMap { it.tokens }.distinct()
                    _recipientTokens.value = tokens
                    _recipientUids.value = targetUids
                    println("[ChatScreenModel]: Сбор токенов завершен. Найдено получателей: ${targetUids.size} ($targetUids), токенов: ${tokens.size}")
                } else {
                    println("[ChatScreenModel]: Получатели не найдены для организации $effectiveOrgId")
                }
            } catch (e: Exception) {
                println("[ChatScreenModel_ERROR]: Ошибка сбора токенов: ${e.message}")
            }
        }

        chatRepo.observeMessages(targetPath)
          .map { messages ->
            messages.filter { msg -> !msg.deletedFor.contains(activeUid) }.sortedBy { it.timestamp }
          }
          .collect { filteredMessages ->
            _firebaseTest.value = filteredMessages
            
            // Если мы в чате, сбрасываем счетчик при любом входящем сообщении
            if (filteredMessages.any { it.senderUid != activeUid }) {
                chatRepo.resetUnreadCount(targetPath, activeUid)
            }
            
            val hasUnreadFromOpponent = filteredMessages.any { !it.read && it.senderUid != activeUid }
            if (hasUnreadFromOpponent) {
              chatRepo.markMessagesAsRead(targetPath, activeUid)
            }
          }
      } catch (e: Exception) {
        if (e is CancellationException) {
          println("[YkisLogKMP]: [CLEANUP] Подписка завершена.")
        } else {
          logService.logNonFatalCrash(e)
        }
      }
    }
  }

  fun clearCurrentChatPath() {
    val path = currentChatPath
    val myUid = chatRepo.currentUid
    if (path != null && myUid != null) {
      screenModelScope.launch { 
          // Финальный сброс счетчика при выходе из чата
          chatRepo.resetUnreadCount(path, myUid)
          setUserTyping(false)
          chatRepo.setUserOffline(path, myUid) 
      }
    }
    activeChatIdForNotifications = null
    currentChatPath = null
    messageSubscriptionJob?.cancel()
    _firebaseTest.value = emptyList()
    _isOpponentOnline.value = false
    _isOpponentTyping.value = false
    _messageText.value = ""
    _recipientTokens.value = emptyList()
    _recipientUids.value = emptyList()
  }

  fun handleSendMessage(baseUIState: BaseUIState) {
      val methodName = "handleSendMessage"
      val user = _selectedUser.value
      val role = baseUIState.userRole
      
      println("[YkisLogKMP.$tag.$methodName]: Початок відправки. Role: $role, User: ${user?.address}, CurrentPath: $currentChatPath")

      val path = currentChatPath ?: run {
          val addrId = if (role == UserRole.StandardUser) baseUIState.addressId else (user?.addressId ?: 0L)
          val osbbId = baseUIState.osbbId ?: 0L
          val recovered = getChatPath(role, osbbId, addrId)
          println("[YkisLogKMP]: [PATH_RECOVERED] Восстановлен путь: $recovered")
          currentChatPath = recovered
          recovered
      }

      val myUid = firebaseService.uid
      val currentMessageText = _messageText.value
      
      if (currentMessageText.isBlank()) return

      val isResident = role == UserRole.StandardUser
      val senderName = if (isResident) {
          val chatApt = baseUIState.apartments.find { it.addressId == (user?.addressId ?: baseUIState.addressId) }
          chatApt?.address ?: baseUIState.address
      } else {
          when (role) {
            UserRole.VodokanalUser -> "КП \"ЮЖВОДОКАНАЛ\""
            UserRole.YtkeUser      -> "КП тм \"ЮТКЕ\""
            UserRole.TboUser       -> "КП \"СПЕЦТРАНС\""
            UserRole.OsbbUser      -> baseUIState.osbb.ifBlank { "ОСББ" }
            else                   -> baseUIState.displayName ?: "Диспетчер"
          }
      }

      println("[YkisLogKMP]: [SEND_START] Отправка в: $path")

      writeToDatabaseInternal(
          chatId = path,
          senderUid = myUid,
          senderDisplayedName = senderName,
          senderLogoUrl = firebaseService.photoUrl,
          senderAddress = if (isResident) (baseUIState.address) else (user?.address ?: ""),
          imageUrl = null,
          fileUrl = null,
          fileName = null,
          recipientTokens = _recipientTokens.value,
          recipientUids = _recipientUids.value,
          onComplete = {
              clearAiSuggestion()
          }
      )
  }

  private fun writeToDatabaseInternal(
    chatId: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    imageUrl: String?,
    fileUrl: String?,
    fileName: String?,
    recipientTokens: List<String>,
    recipientUids: List<String> = emptyList(),
    onComplete: () -> Unit
  ) {
    val text = _messageText.value
    _isLoadingAfterSending.value = true
    setUserTyping(false)

    launchCatching {
      try {
        val message = MessageEntity(
            senderUid = senderUid,
            senderDisplayedName = senderDisplayedName,
            senderLogoUrl = senderLogoUrl,
            senderAddress = senderAddress,
            text = text,
            imageUrl = imageUrl,
            fileUrl = fileUrl,
            fileName = fileName,
            timestamp = currentTimeMillis()
        )
        println("[YkisLogKMP]: [FIREBASE_WRITE] Путь: $chatId")
        chatRepo.sendMessage(chatId, message)
        
        // 1. Инкрементируем бэйджи (С УЧЕТОМ РОЛИ)
        val allRecipientUids = (recipientUids).distinct()
        if (allRecipientUids.isNotEmpty()) {
            chatRepo.incrementUnreadForUids(chatId, allRecipientUids)
        } else {
            // Если список пуст, используем фильтрованный fallback
            if (uiState.value.userRole != UserRole.StandardUser) {
                // Если пишет АДМИН — уведомляем всех жильцов
                chatRepo.incrementUnreadForParticipants(chatId, senderUid)
            } else {
                // Если пишет ЖИЛЕЦ — мы НЕ используем общий инкремент, 
                // чтобы не спамить бэйджами другим членам семьи.
                println("[YkisLogKMP]: [BADGE_SKIP] Жилец пишет в пустой чат, ждем входа админа.")
            }
        }
        
        // 2. ОТПРАВКА ПУША
        println("[YkisLogKMP]: [PUSH_ATTEMPT] Токенов получателей: ${recipientTokens.size}")
        if (recipientTokens.isNotEmpty() && !_isOpponentOnline.value) {
            val data = mapOf(
                "tokens" to recipientTokens,
                "title" to senderDisplayedName,
                "body" to text,
                "chatId" to chatId,
                "imageUrl" to imageUrl
            )
            chatRepo.sendChatNotification(data)
            println("[YkisLogKMP]: [PUSH_SENT_CALL_SUCCESS]")
        } else {
            println("[YkisLogKMP]: [PUSH_SKIP] Причина: ${if(recipientTokens.isEmpty()) "Нет токенов" else "Оппонент ОНЛАЙН"}")
        }
        
        println("[YkisLogKMP]: [FIREBASE_SUCCESS]")
        _messageText.value = ""
        onComplete()
        
      } catch (e: Exception) {
        println("[YkisLogKMP]: [FIREBASE_ERROR] ${e.message}")
        logService.logNonFatalCrash(e)
      } finally {
        _isLoadingAfterSending.value = false
      }
    }
  }

  fun uploadFileAndSendMessage(
      senderUid: String,
      senderDisplayedName: String,
      senderLogoUrl: String?,
      senderAddress: String,
      addressId: Long,
      osbbId: Long,
      role: UserRole,
      recipientTokens: List<String>,
      onComplete: () -> Unit
  ) {
      val path = _selectedImagePath.value ?: return
      
      val user = _selectedUser.value
      val baseUIState = uiState.value // Используем текущее состояние для восстановления

      val targetChatId = currentChatPath ?: run {
          val addrId = if (role == UserRole.StandardUser) addressId else (user?.addressId ?: 0L)
          val recovered = getChatPath(role, osbbId, addrId)
          currentChatPath = recovered
          recovered
      }

      launchCatching {
          _isLoadingAfterSending.value = true
          try {
              println("[YkisLogKMP]: [FILE_UPLOAD_START] Путь: $path")
              val bytes = chatRepo.readFileAsBytes(path)
              
              val isImage = path.lowercase().let { 
                  it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp") 
              }
              
              val fileName = path.split("/").lastOrNull() ?: "file_${currentTimeMillis()}"
              val ext = if (isImage) "jpg" else path.substringAfterLast(".", "bin")
              
              val storagePath = "chat_files/${osbbId}/${addressId}/${currentTimeMillis()}.$ext"
              val url = chatRepo.uploadFile(bytes, storagePath)
              
              println("[YkisLogKMP]: [FILE_UPLOAD_SUCCESS] URL: $url")

              writeToDatabaseInternal(
                  chatId = targetChatId,
                  senderUid = senderUid,
                  senderDisplayedName = senderDisplayedName,
                  senderLogoUrl = senderLogoUrl,
                  senderAddress = senderAddress,
                  imageUrl = if (isImage) url else null,
                  fileUrl = if (!isImage) url else null,
                  fileName = fileName,
                  recipientTokens = _recipientTokens.value,
                  recipientUids = _recipientUids.value,
                  onComplete = { 
                      _selectedImagePath.value = null
                      onComplete() 
                  }
              )
          } catch (e: Exception) {
              println("[YkisLogKMP]: [FILE_ERROR] ${e.message}")
              logService.logNonFatalCrash(e)
          } finally {
              _isLoadingAfterSending.value = false
          }
      }
  }

  fun updateMessage(newText: String) {
    val msg = _editingMessage.value ?: return
    val path = currentChatPath ?: return
    launchCatching {
      chatRepo.updateMessage(path, msg.id, mapOf("text" to newText, "edited" to true))
      _editingMessage.value = null
      _messageText.value = ""
    }
  }

  fun startEditing(message: MessageEntity) {
    _editingMessage.value = message
    _messageText.value = message.text
  }

  fun showDeleteConfirmation(message: MessageEntity) {
    _messageToDelete.value = message
  }

  fun dismissDeleteDialog() {
    _messageToDelete.value = null
  }

  fun confirmDeletion() {
    val msg = _messageToDelete.value ?: return
    val path = currentChatPath ?: return
    launchCatching {
      chatRepo.removeMessage(path, msg.id)
      dismissDeleteDialog()
    }
  }

  fun deleteForMe(messageId: String) {
    val path = currentChatPath ?: return
    val myUid = chatRepo.currentUid ?: return
    launchCatching {
      chatRepo.deleteMessageForUser(path, messageId, myUid)
    }
  }

  fun setSelectedMessage(message: MessageEntity?) {
    _selectedMessage.value = message
  }

  fun setSelectedImagePath(path: String?) {
    _selectedImagePath.value = path
  }

  fun analyzePhotoWithGemini(path: String, address: String?) {
    screenModelScope.launch {
      _isAssistantLoading.value = true
      try {
        val bytes = chatRepo.readFileAsBytes(path)
        val prompt = "Проаналізуй фото лічильника для адреси ${address ?: "м. Южне"}"
        val result = chatRepo.analyzeMeterImage(prompt, bytes)
        _assistantResponse.value = result
      } catch (e: Exception) {
        logService.logNonFatalCrash(e)
      } finally {
        _isAssistantLoading.value = false
      }
    }
  }

  fun applyAiHint() {
    _assistantResponse.value?.let { _messageText.value = it }
    _assistantResponse.value = null
  }

  fun clearAiSuggestion() {
    _assistantResponse.value = null
  }

  fun askAssistant(prompt: String, currentRole: UserRole, currentAddress: String) {
    screenModelScope.launch {
      _isAssistantLoading.value = true
      try {
        val fullPrompt = "Роль: $currentRole, Адреса: $currentAddress. Питання: $prompt"
        val result = chatRepo.askAiAssistant(fullPrompt)
        _assistantResponse.value = result.getOrNull()
      } catch (e: Exception) {
        logService.logNonFatalCrash(e)
      } finally {
        _isAssistantLoading.value = false
      }
    }
  }

  private fun setPresence(path: String, isOnline: Boolean) {
    val myUid = chatRepo.currentUid ?: return
    screenModelScope.launch {
        if (isOnline) chatRepo.setUserOnline(path, myUid) 
        else chatRepo.setUserOffline(path, myUid)
    }
  }

  private fun observeOpponentPresence(path: String) {
    presenceJob?.cancel()
    presenceJob = screenModelScope.launch {
      chatRepo.observePresence(path)
        .collect { presenceMap ->
          val myUid = chatRepo.currentUid
          val opponentOnline = presenceMap.filter { it.key != myUid }.values.any { it }
          _isOpponentOnline.value = opponentOnline
        }
    }
  }

  private fun observeTypingStatus(path: String) {
    typingStatusJob?.cancel()
    typingStatusJob = screenModelScope.launch {
      chatRepo.observeTyping(path)
        .collect { typingMap ->
          val myUid = chatRepo.currentUid
          val opponentTyping = typingMap.filter { it.key != myUid }.values.any { it }
          _isOpponentTyping.value = opponentTyping
        }
    }
  }

  fun setUserTyping(isTyping: Boolean) {
    val path = currentChatPath ?: return
    val myUid = chatRepo.currentUid ?: return
    screenModelScope.launch { chatRepo.setTypingStatus(path, myUid, isTyping) }
  }

  fun trackUserIdentifiersWithRole(role: UserRole, osbbId: Long, apartments: List<ApartmentEntity> = emptyList()) {
    val methodName = "trackUserIdentifiers"
    val myUid = chatRepo.currentUid ?: ""
    val safeOsbbId = osbbId
    
    println("[YkisLogKMP]: ENTRY. Роль: $role | Квартир: ${apartments.size}")

    activeTrackerJob?.cancel()
    
    // Подписываемся на бэйджи глобально
    subscribeToUnreadCountGlobal(myUid)

    if (role == UserRole.StandardUser) {
      val residentKeys = mutableListOf<String>()
      apartments.forEach { apt ->
        residentKeys.add("OSBB_${apt.osmdId ?: 0L}_${apt.addressId}")
        residentKeys.add("WATER_SERVICE_${Constants.WATER_SERVICE_ID}_${apt.addressId}")
        residentKeys.add("WARM_SERVICE_${Constants.WARM_SERVICE_ID}_${apt.addressId}")
        residentKeys.add("GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_${apt.addressId}")
      }
      
      if (residentKeys.isNotEmpty()) {
        println("[YkisLogKMP]: Житель — запуск мониторинга ${residentKeys.size} веток.")
        _userIdentifiersWithRole.value = residentKeys
        subscribeToLastMessages(residentKeys)
      }
      return
    }

    val prefix = _selectedServicePrefix.value ?: "OSBB"
    val targetPrefix = when (role) {
      UserRole.VodokanalUser -> "WATER_SERVICE_${Constants.WATER_SERVICE_ID}_"
      UserRole.YtkeUser      -> "WARM_SERVICE_${Constants.WARM_SERVICE_ID}_"
      UserRole.TboUser       -> "GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_"
      UserRole.OsbbUser      -> "OSBB_${safeOsbbId}_"
      else                   -> "${prefix}_${safeOsbbId}_"
    }

    println("[YkisLogKMP]: [FETCH_USERS] Пошук за префіксом: $targetPrefix")

    activeTrackerJob = screenModelScope.launch {
      chatRepo.observeChatKeys(targetPrefix).collect { chatKeys ->
          println("[YkisLogKMP]: [FETCH_USERS] Знайдено ключів: ${chatKeys.size}")
          
          _userIdentifiersWithRole.value = chatKeys

          if (chatKeys.isNotEmpty()) {
            subscribeToLastMessages(chatKeys)
            getUsers(chatKeys)
            
            // ІСПРАВЛЕНО: Адмін автоматично реєструє себе як учасника у всіх знайдених чатах.
            // Це дозволяє жильцям нараховувати йому бейджі через chat_access, 
            // навіть якщо Firestore заблокований для пошуку.
            chatKeys.forEach { chatId ->
                launch { chatRepo.addChatParticipant(chatId, myUid) }
            }
          } else {
            _rawFetchedProfiles.value = emptyList()
          }
      }
    }
  }

  private fun subscribeToUnreadCountGlobal(myUid: String) {
    unreadSubscriptionJob?.cancel()
    unreadSubscriptionJob = launchCatching(snackbar = false) {
      chatRepo.observeUnreadCounts(myUid).collect { countMap ->
        _unreadCounts.value = countMap
        
        // ІСПРАВЛЕНО: Рахуємо суму лише для валідних КМР-чатів (формат PREFIX_ID_ADDRESS)
        // Це відфільтрує старі UID-базовані бейджі, які могли залишитись в базі.
        val validChats = countMap.filter { (key, _) -> key.contains("_") }
        val total = validChats.values.sum()
        
        applyAppBadgeCount(total)
        println("[ChatScreenModel]: Оновлено бейджів: $total (відфільтровано: ${countMap.size - validChats.size})")
      }
    }
  }

  private fun subscribeToLastMessages(chatKeys: List<String>) {
    chatKeys.forEach { chatId ->
      if (chatId.isBlank() || lastMessageListeners.containsKey(chatId)) return@forEach
      val job = screenModelScope.launch {
        chatRepo.observeLastMessage(chatId).collect { message ->
          if (message != null) {
            _lastMessages.update { it + (chatId to message) }
          }
        }
      }
      lastMessageListeners[chatId] = job
    }
  }

  fun getUsers(chatKeys: List<String>) {
    if (chatKeys.isEmpty()) return

    launchCatching(snackbar = false) {
      val addressIdsToFetch = chatKeys.map { it.substringAfterLast("_").toLongOrNull() ?: 0L }
        .filter { it != 0L }
        .distinct()

      println("[YkisLogKMP]: Запит профілів для о/р: $addressIdsToFetch")
      
      val fetchedProfiles = mutableListOf<UserEntity>()
      addressIdsToFetch.forEach { addrId ->
          chatRepo.fetchUserByAddressId(addrId)?.let { fetchedProfiles.add(it) }
      }
      _rawFetchedProfiles.value = fetchedProfiles
    }
  }

  fun startForwarding(message: MessageEntity) {
    _forwardingMessage.value = message
  }

  fun cancelForwarding() {
    _forwardingMessage.value = null
  }

  fun stopAllListeners() {
      println("[ChatScreenModel]: Зупинка всіх фонових слухачів...")
      messageSubscriptionJob?.cancel()
      typingStatusJob?.cancel()
      presenceJob?.cancel()
      activeTrackerJob?.cancel()
      unreadSubscriptionJob?.cancel()
      lastMessageListeners.values.forEach { it.cancel() }
      lastMessageListeners.clear()
  }

  fun confirmForwardToService(service: ContentDetail, baseState: BaseUIState, targetUser: UserEntity? = null) {
    val msg = _forwardingMessage.value ?: return
    val myUid = chatRepo.currentUid ?: return

    launchCatching {
      try {
        val targetPath = if (baseState.userRole == UserRole.StandardUser) {
           val serviceId = when(service) {
             ContentDetail.WATER_SERVICE -> Constants.WATER_SERVICE_ID
             ContentDetail.WARM_SERVICE -> Constants.WARM_SERVICE_ID
             ContentDetail.GARBAGE_SERVICE -> Constants.GARBAGE_SERVICE_ID
             else -> baseState.osbbId ?: 0L
           }
           val prefix = when(service) {
             ContentDetail.WATER_SERVICE -> "WATER_SERVICE"
             ContentDetail.WARM_SERVICE -> "WARM_SERVICE"
             ContentDetail.GARBAGE_SERVICE -> "GARBAGE_SERVICE"
             else -> "OSBB"
           }
           "${prefix}_${serviceId}_${baseState.addressId}"
        } else {
           val user = targetUser ?: return@launchCatching
           getChatPath(baseState.userRole, baseState.osbbId ?: 0L, user.addressId)
        }

        val forwardMessage = MessageEntity(
            senderUid = myUid,
            text = msg.text,
            imageUrl = msg.imageUrl,
            timestamp = currentTimeMillis(),
            isForwarded = true
        )
        chatRepo.sendMessage(targetPath, forwardMessage)
        chatRepo.incrementUnreadForParticipants(targetPath, myUid)
        
        cancelForwarding()
        SnackbarManager.showMessage(Res.string.success_send_message)
      } catch (e: Exception) { }
    }
  }

  private fun markMessagesAsRead(chatId: String) {
    val myUid = chatRepo.currentUid ?: return
    launchCatching {
      chatRepo.markMessagesAsRead(chatId, myUid)
    }
  }
}
