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
private const val className = "ChatViewModel"

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
  private var unreadCountListeners = mutableMapOf<String, Job>()

  companion object {
    var activeChatIdForNotifications: String? = null
  }

  val userList: StateFlow<List<UserEntity>> = combine(
    _userIdentifiersWithRole,
    _rawFetchedProfiles,
    _lastMessages,
    _searchQuery
  ) { keys, profiles, lastMsgs, query ->
    val fullList = keys.mapNotNull { key ->
      val parts = key.split("_")
      if (parts.size < 4) return@mapNotNull null

      val uidFromKey = parts.last()
      val addrIdFromKey = parts.getOrNull(parts.size - 2)?.toLongOrNull() ?: 0L

      val profile = profiles.find { it.uid == uidFromKey }
      val lastMsg = lastMsgs[key]
      val preview = lastMsg?.text ?: "Немає повідомлень"

      val finalDisplayName = when {
        !lastMsg?.senderAddress.isNullOrBlank() -> lastMsg.senderAddress
        profile != null -> profile.displayName ?: "Жилець (о/р $addrIdFromKey)"
        else -> "Користувач (о/р $addrIdFromKey)"
      }

      profile?.copy(
        addressId = addrIdFromKey,
        address = preview,
        displayName = finalDisplayName
      ) ?: UserEntity(
        uid = uidFromKey,
        addressId = addrIdFromKey,
        displayName = finalDisplayName,
        address = preview
      )
    }

    if (query.isBlank()) {
      fullList
    } else {
      fullList.filter { user ->
        (user.displayName?.contains(query, ignoreCase = true) == true) ||
          user.addressId.toString().contains(query) ||
          user.address.contains(query, ignoreCase = true)
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
       println("[YkisLogKMP]: [PREFIX_RESOLVED] '${service.name}' -> $prefix")
       
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
    println("[YkisLogKMP.$className]: [OPEN_CHAT] Користувач: ${user.address}, Роль: $currentRole")
    _selectedUser.value = user
    _firebaseTest.value = emptyList()
    _messageText.value = ""
    
    readFromDatabase(
      role = currentRole,
      senderUid = user.uid,
      osbbId = currentOsbbId,
      addressId = user.addressId
    )
  }

  fun getChatPath(role: UserRole, osbbId: Long, addressId: Long, targetUserUid: String?): String {
    val servicePrefix = _selectedServicePrefix.value
    
    val effectiveId = if (role == UserRole.StandardUser) {
        when (servicePrefix) {
            "WATER_SERVICE" -> 9999L
            "WARM_SERVICE" -> 9998L
            "GARBAGE_SERVICE" -> 9997L
            else -> if (osbbId != 0L) osbbId else (_selectedUser.value?.osbbId ?: 0L)
        }
    } else osbbId

    val myUid = chatRepo.currentUid ?: ""
    val path = if (role == UserRole.StandardUser) {
      "${servicePrefix}_${effectiveId}_${addressId}_${myUid}"
    } else {
      "${servicePrefix}_${effectiveId}_${addressId}_${targetUserUid}"
    }
    
    return path
  }

  fun readFromDatabase(role: UserRole, senderUid: String, osbbId: Long, addressId: Long) {
    val finalOsbbId = if (osbbId == 0L) {
        when (role) {
            UserRole.VodokanalUser -> 9999L
            UserRole.YtkeUser      -> 9998L
            UserRole.TboUser       -> 9997L
            else -> osbbId
        }
    } else osbbId

    val targetPath = getChatPath(role, finalOsbbId, addressId, senderUid)
    
    if (currentChatPath == targetPath && messageSubscriptionJob?.isActive == true) {
        println("[YkisLogKMP]: [READ_SKIP] Вже підписані на $targetPath")
        return
    }

    println("[YkisLogKMP]: [READ_START] Путь: $targetPath")

    messageSubscriptionJob?.cancel()
    messageSubscriptionJob = screenModelScope.launch {
      try {
        val activeUid = chatRepo.currentUid ?: return@launch
        currentChatPath = targetPath
        activeChatIdForNotifications = targetPath
        
        observeOpponentPresence(targetPath)
        setPresence(targetPath, true)
        observeTypingStatus(targetPath)
        firebaseService.clearNotifications(targetPath)

        if (role == UserRole.StandardUser && activeUid.isNotBlank()) {
           screenModelScope.launch {
              try {
                val tokensId = if (osbbId != 0L) osbbId else (_selectedUser.value?.osbbId ?: 0L)
                if (tokensId != 0L) {
                    val admins = chatRepo.fetchAdminsByOsbb(tokensId)
                    _recipientTokens.value = admins.flatMap { it.tokens }.distinct()
                }
              } catch (e: Exception) { }
           }
        }

        chatRepo.observeMessages(targetPath)
          .map { messages ->
            messages.filter { msg -> !msg.deletedFor.contains(activeUid) }.sortedBy { it.timestamp }
          }
          .collect { filteredMessages ->
            _firebaseTest.value = filteredMessages
            val hasUnreadFromOpponent = filteredMessages.any { !it.read && it.senderUid != activeUid }
            if (hasUnreadFromOpponent) {
              chatRepo.markMessagesAsRead(targetPath, activeUid)
            }
          }
      } catch (e: Exception) {
        if (e is CancellationException) {
          println("[YkisLogKMP]: [CLEANUP] Підписка скасована (нормально при зміні екрану).")
        } else {
          println("[YkisLogKMP]: [READ_ERROR] Помилка БД: ${e.message}")
          logService.logNonFatalCrash(e)
        }
      }
    }
  }

  fun clearCurrentChatPath() {
    val path = currentChatPath
    if (path != null) {
      screenModelScope.launch { 
          setUserTyping(false)
          chatRepo.setUserOffline(path, chatRepo.currentUid ?: "") 
      }
    }
    activeChatIdForNotifications = null
    currentChatPath = null
    messageSubscriptionJob?.cancel()
    typingStatusJob?.cancel()
    presenceJob?.cancel()
    _firebaseTest.value = emptyList()
    _isOpponentOnline.value = false
    _isOpponentTyping.value = false
    _messageText.value = ""
  }

  fun handleSendMessage(baseUIState: BaseUIState) {
      val user = _selectedUser.value
      val role = baseUIState.userRole
      
      // ВОССТАНОВЛЕНИЕ ПУТИ: Если путь пуст, пересчитываем его
      val path = currentChatPath ?: run {
          val addrId = if (role == UserRole.StandardUser) baseUIState.addressId else (user?.addressId ?: 0L)
          val targetUid = if (role == UserRole.StandardUser) (firebaseService.uid) else (user?.uid ?: "")
          val osbbId = baseUIState.osbbId ?: 0L
          val recovered = getChatPath(role, osbbId, addrId, targetUid)
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
          val addr = chatApt?.address ?: baseUIState.address ?: ""
          val nanim = chatApt?.nanim ?: baseUIState.nanim ?: "Мешканець"
          "$addr | $nanim"
      } else {
          when (role) {
            UserRole.VodokanalUser -> "КП \"ЮЖВОДОКАНАЛ\""
            UserRole.YtkeUser      -> "КП тм \"ЮТКЕ\""
            UserRole.TboUser       -> "КП \"СПЕЦТРАНС\""
            UserRole.OsbbUser      -> baseUIState.osbb.ifBlank { "ОСББ" }
            else                   -> baseUIState.displayName ?: "Диспетчер"
          }
      }

      val tokens = if (isResident) _recipientTokens.value else (user?.tokens ?: emptyList())

      println("[YkisLogKMP]: [SEND_START] Отправка в: $path")

      writeToDatabase(
          chatUid = path,
          senderUid = myUid,
          senderDisplayedName = senderName,
          senderLogoUrl = firebaseService.photoUrl,
          senderAddress = if (isResident) (baseUIState.address ?: "") else (user?.address ?: ""),
          addressId = if (isResident) baseUIState.addressId else (user?.addressId ?: 0L),
          imageUrl = null,
          fileUrl = null,
          fileName = null,
          osbbId = baseUIState.osbbId ?: 0L,
          role = role,
          recipientTokens = tokens,
          onComplete = {
              clearAiSuggestion()
          }
      )
  }

  fun writeToDatabase(
    chatUid: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    addressId: Long,
    imageUrl: String?,
    fileUrl: String?,
    fileName: String?,
    osbbId: Long,
    role: UserRole,
    recipientTokens: List<String>,
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
        println("[YkisLogKMP]: [FIREBASE_WRITE] Путь: $chatUid")
        chatRepo.sendMessage(chatUid, message)
        
        println("[YkisLogKMP]: [FIREBASE_SUCCESS]")
        _messageText.value = ""
        onComplete()
        
        if (recipientTokens.isNotEmpty() && !_isOpponentOnline.value) {
            val data = mapOf(
                "tokens" to recipientTokens,
                "title" to senderDisplayedName,
                "body" to text,
                "chatId" to chatUid,
                "imageUrl" to imageUrl
            )
            chatRepo.sendChatNotification(data)
        }
      } catch (e: Exception) {
        println("[YkisLogKMP]: [FIREBASE_ERROR] ${e.message}")
        logService.logNonFatalCrash(e)
      } finally {
        _isLoadingAfterSending.value = false
      }
    }
  }

  fun uploadFileAndSendMessage(
      chatUid: String,
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
      launchCatching {
          _isLoadingAfterSending.value = true
          try {
              val bytes = chatRepo.readFileAsBytes(path)
              val fileName = path.split("/").lastOrNull() ?: "image.jpg"
              val url = chatRepo.uploadFile(bytes, "chats/$chatUid/$fileName")
              
              writeToDatabase(
                  chatUid = chatUid,
                  senderUid = senderUid,
                  senderDisplayedName = senderDisplayedName,
                  senderLogoUrl = senderLogoUrl,
                  senderAddress = senderAddress,
                  addressId = addressId,
                  imageUrl = url,
                  fileUrl = null,
                  fileName = fileName,
                  osbbId = osbbId,
                  role = role,
                  recipientTokens = recipientTokens,
                  onComplete = { 
                      _selectedImagePath.value = null
                      onComplete() 
                  }
              )
          } catch (e: Exception) {
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
    val safeOsbbId = osbbId ?: 0L
    
    println("[YkisLogKMP]: ENTRY. Роль: $role | Квартир: ${apartments.size}")

    activeTrackerJob?.cancel()

    if (role == UserRole.StandardUser) {
      val residentKeys = mutableListOf<String>()
      apartments.forEach { apt ->
        residentKeys.add("OSBB_${apt.osmdId ?: 0L}_${apt.addressId}_$myUid")
        residentKeys.add("WATER_SERVICE_${Constants.WATER_SERVICE_ID}_${apt.addressId}_$myUid")
        residentKeys.add("WARM_SERVICE_${Constants.WARM_SERVICE_ID}_${apt.addressId}_$myUid")
        residentKeys.add("GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_${apt.addressId}_$myUid")
      }
      
      if (residentKeys.isNotEmpty()) {
        println("[YkisLogKMP]: Житель — запуск мониторинга ${residentKeys.size} веток.")
        _userIdentifiersWithRole.value = residentKeys
        subscribeToUnreadCount(residentKeys)
        subscribeToLastMessages(residentKeys)
      }
      return
    }

    // ЛОГІКА ДЛЯ АДМИНА
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
          println("[YkisLogKMP]: [FETCH_USERS] Знайдено ключів: ${chatKeys.size} -> $chatKeys")
          
          _userIdentifiersWithRole.value = chatKeys

          if (chatKeys.isNotEmpty()) {
            subscribeToUnreadCount(chatKeys)
            subscribeToLastMessages(chatKeys)
            getUsers()
          } else {
            _rawFetchedProfiles.value = emptyList()
            _unreadCounts.value = emptyMap()
          }
      }
    }
  }

  private fun subscribeToUnreadCount(chatKeys: List<String>) {
    val myUid = chatRepo.currentUid ?: return
    chatKeys.forEach { chatId ->
      if (chatId.isBlank() || unreadCountListeners.containsKey(chatId)) return@forEach
      val job = screenModelScope.launch {
        chatRepo.observeMessages(chatId)
          .map { messages ->
            messages.count { it.senderUid != myUid && !it.read }
          }
          .collect { count ->
            _unreadCounts.update { it + (chatId to count) }
            applyAppBadgeCount(_unreadCounts.value.values.sum())
          }
      }
      unreadCountListeners[chatId] = job
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

  fun getUsers() {
    val chatKeys = _userIdentifiersWithRole.value
    if (chatKeys.isEmpty()) return

    launchCatching(snackbar = false) {
      val uidsToFetch = chatKeys.map { it.substringAfterLast("_") }
        .filter { it.isNotEmpty() }
        .distinct()

      val fetchedProfiles = chatRepo.fetchUsersByIds(uidsToFetch)
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
      lastMessageListeners.values.forEach { it.cancel() }
      unreadCountListeners.values.forEach { it.cancel() }
      lastMessageListeners.clear()
      unreadCountListeners.clear()
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
           "${prefix}_${serviceId}_${baseState.addressId}_$myUid"
        } else {
           val user = targetUser ?: return@launchCatching
           getChatPath(baseState.userRole, baseState.osbbId ?: 0L, user.addressId, user.uid)
        }

        val forwardMessage = MessageEntity(
            senderUid = myUid,
            text = msg.text,
            imageUrl = msg.imageUrl,
            timestamp = currentTimeMillis(),
            isForwarded = true
        )
        chatRepo.sendMessage(targetPath, forwardMessage)
        cancelForwarding()
        SnackbarManager.showMessage(Res.string.success_send_message)
      } catch (e: Exception) { }
    }
  }
}
