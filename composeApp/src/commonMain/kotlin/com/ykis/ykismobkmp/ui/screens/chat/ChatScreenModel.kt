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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.success_send_message

private const val tag = "ChatViewModel"

class ChatScreenModel(
  private val chatRepo: ChatRepository,
  val firebaseService: FirebaseService,
  logService: LogService
) : BaseScreenModel(logService) {

  companion object {
    var activeChatIdForNotifications: String? = null
  }

  private val _assistantResponse = MutableStateFlow<String?>(null)
  val assistantResponse = _assistantResponse.asStateFlow()

  private val _isAssistantLoading = MutableStateFlow(false)
  val isAssistantLoading = _isAssistantLoading.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()

  private val _userIdentifiersWithRole = MutableStateFlow<List<String>>(emptyList())
  private val _rawFetchedProfiles = MutableStateFlow<List<UserEntity>>(emptyList())

  private val _selectedUser = MutableStateFlow<UserEntity?>(null)
  val selectedUser = _selectedUser.asStateFlow()

  private val _selectedService = MutableStateFlow<TotalServiceDebt?>(null)
  val selectedService = _selectedService.asStateFlow()

  private val _selectedServicePrefix = MutableStateFlow("OSBB")
  val selectedServicePrefix = _selectedServicePrefix.asStateFlow()

  private val _messageLimit = MutableStateFlow(50)
  val messageLimit = _messageLimit.asStateFlow()
  
  private var currentParams: Triple<UserRole, Long, Long>? = null

  private val _messageText = MutableStateFlow("")
  val messageText = _messageText.asStateFlow()

  private val _isLoadingAfterSending = MutableStateFlow(false)
  val isLoadingAfterSending = _isLoadingAfterSending.asStateFlow()

  private val _editingMessage = MutableStateFlow<MessageEntity?>(null)
  val editingMessage: StateFlow<MessageEntity?> = _editingMessage.asStateFlow()

  private val _messageToDelete = MutableStateFlow<MessageEntity?>(null)
  val messageToDelete: StateFlow<MessageEntity?> = _messageToDelete.asStateFlow()

  private val _selectedImagePath = MutableStateFlow<String?>(null)
  val selectedImagePath = _selectedImagePath.asStateFlow()

  private val _selectedFileName = MutableStateFlow<String?>(null)
  val selectedFileName = _selectedFileName.asStateFlow()

  private val _selectedMessage = MutableStateFlow<MessageEntity?>(null)
  val selectedMessage: StateFlow<MessageEntity?> = _selectedMessage.asStateFlow()

  private val _firebaseTest = MutableStateFlow<List<MessageEntity>>(emptyList())
  val firebaseTest = _firebaseTest.asStateFlow()

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
  val pendingPushChatId = _pendingPushChatId.asStateFlow()

  private val _forwardingMessage = MutableStateFlow<MessageEntity?>(null)
  val forwardingMessage = _forwardingMessage.asStateFlow()

  val isForwardingMode = _forwardingMessage
    .map { it != null }
    .stateIn(screenModelScope, SharingStarted.Lazily, false)

  val activeChatPath: String? get() = currentParams?.let { getChatPath(it.first, it.second, it.third) }

  private var messageSubscriptionJob: Job? = null
  private var typingStatusJob: Job? = null
  private var presenceJob: Job? = null
  private var activeTrackerJob: Job? = null
  private var lastMessageListeners = mutableMapOf<String, Job>()
  private var unreadSubscriptionJob: Job? = null

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

      val finalAddress = when {
        profile != null && !profile.address.isNullOrBlank() -> {
            if (profile.address.contains("|")) profile.address.substringBefore("|").trim()
            else profile.address
        }
        lastMsg != null && !lastMsg.senderAddress.isNullOrBlank() && lastMsg.senderAddress != " " -> lastMsg.senderAddress
        lastMsg != null && !lastMsg.senderDisplayedName.isNullOrBlank() && lastMsg.senderDisplayedName.contains("|") -> {
            lastMsg.senderDisplayedName.substringBefore("|").trim()
        }
        else -> "Квартира (о/р $addrIdFromKey)"
      }

      val finalResidentName = when {
        profile != null && !profile.fio.isNullOrBlank() -> profile.fio
        profile != null && !profile.displayName.isNullOrBlank() && profile.displayName.contains("|") -> {
            profile.displayName.substringAfter("|").trim()
        }
        lastMsg != null && !lastMsg.senderDisplayedName.isNullOrBlank() -> {
            if (lastMsg.senderDisplayedName.contains("|")) lastMsg.senderDisplayedName.substringAfter("|").trim()
            else lastMsg.senderDisplayedName
        }
        else -> ""
      }

      UserEntity(
        uid = key, 
        addressId = addrIdFromKey,
        displayName = profile?.displayName ?: finalAddress,
        address = finalAddress,
        nanim = finalResidentName,
        fio = finalResidentName
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

  fun setPendingPushChatId(id: String?) { _pendingPushChatId.value = id }
  fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
  fun onMessageTextChanged(text: String) {
    _messageText.value = text
    activeChatPath?.let { setUserTyping(text.isNotEmpty()) }
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

  fun onServiceSelectedForResident(prefix: String?) {
     val finalPrefix = prefix ?: "OSBB"
     _selectedServicePrefix.value = finalPrefix
     val serviceName = when(finalPrefix) {
       "WATER_SERVICE" -> "Водоканал"
       "WARM_SERVICE" -> "Тепломережі"
       "GARBAGE_SERVICE" -> "Вивіз сміття"
       else -> "ОСББ"
     }
     val detail = when(finalPrefix) {
       "WATER_SERVICE" -> ContentDetail.WATER_SERVICE
       "WARM_SERVICE" -> ContentDetail.WARM_SERVICE
       "GARBAGE_SERVICE" -> ContentDetail.GARBAGE_SERVICE
       else -> ContentDetail.OSBB
     }
     _selectedService.value = TotalServiceDebt(
        name = serviceName, color = Color.Gray, debt = 0.0, icon = Icons.Default.Home, contentDetail = detail
     )
     screenModelScope.launch {
        val profile = firebaseService.getUserProfile()
        trackUserIdentifiersWithRole(UserRole.fromString(profile.userRole), profile.osbbId)
     }
  }

  fun selectUserByAddressId(addressId: Long) {
     screenModelScope.launch {
        val user = chatRepo.fetchUserByAddressId(addressId)
        if (user != null) _selectedUser.value = user
     }
  }

  fun openChatWithUser(user: UserEntity, currentRole: UserRole, currentOsbbId: Long) {
    _selectedUser.value = user
    _firebaseTest.value = emptyList()
    _messageText.value = ""
    readFromDatabase(role = currentRole, osbbId = currentOsbbId, addressId = user.addressId)
  }

  fun getChatPath(role: UserRole, osbbId: Long, addressId: Long): String {
    val servicePrefix = _selectedServicePrefix.value
    val effectiveId = if (role == UserRole.StandardUser) {
        when (servicePrefix) {
            "WATER_SERVICE"   -> Constants.WATER_SERVICE_ID
            "WARM_SERVICE"    -> Constants.WARM_SERVICE_ID
            "GARBAGE_SERVICE" -> Constants.GARBAGE_SERVICE_ID
            else -> if (osbbId != 0L) osbbId else (_selectedUser.value?.osbbId ?: 0L)
        }
    } else osbbId
    return "${servicePrefix}_${effectiveId}_${addressId}"
  }

  fun readFromDatabase(role: UserRole, osbbId: Long, addressId: Long) {
    currentParams = Triple(role, osbbId, addressId)
    val finalOsbbId = if (osbbId == 0L) {
        when (role) {
            UserRole.VodokanalUser -> Constants.WATER_SERVICE_ID
            UserRole.YtkeUser      -> Constants.WARM_SERVICE_ID
            UserRole.TboUser       -> Constants.GARBAGE_SERVICE_ID
            else -> osbbId
        }
    } else osbbId

    val targetPath = getChatPath(role, finalOsbbId, addressId)
    if (activeChatIdForNotifications == targetPath) return

    println("[YkisLogKMP]: [READ_START] Путь: $targetPath")
    messageSubscriptionJob?.cancel()
    messageSubscriptionJob = screenModelScope.launch {
      try {
        val activeUid = chatRepo.currentUid ?: return@launch
        activeChatIdForNotifications = targetPath
        
        chatRepo.addChatParticipant(targetPath, activeUid)
        chatRepo.resetUnreadCount(targetPath, activeUid)
        
        observeOpponentPresence(targetPath)
        setPresence(targetPath, true)
        observeTypingStatus(targetPath)

        chatRepo.observeMessages(targetPath, _messageLimit.value)
          .collect { filteredMessages ->
            _firebaseTest.value = filteredMessages.filter { !it.deletedFor.contains(activeUid) }
            if (filteredMessages.any { it.senderUid != activeUid }) {
                chatRepo.resetUnreadCount(targetPath, activeUid)
            }
            if (filteredMessages.any { !it.read && it.senderUid != activeUid }) {
              chatRepo.markMessagesAsRead(targetPath, activeUid)
            }
          }
      } catch (e: Exception) {
        if (e !is CancellationException) logService.logNonFatalCrash(e)
      }
    }
  }

  fun clearCurrentChatPath() {
    val path = activeChatIdForNotifications
    val myUid = chatRepo.currentUid
    if (path != null && myUid != null) {
      screenModelScope.launch { 
          chatRepo.resetUnreadCount(path, myUid)
          setUserTyping(false)
          chatRepo.setUserOffline(path, myUid) 
      }
    }
    activeChatIdForNotifications = null
    messageSubscriptionJob?.cancel()
    _firebaseTest.value = emptyList()
    _isOpponentOnline.value = false
    _isOpponentTyping.value = false
  }

  fun handleSendMessage(baseUIState: BaseUIState) {
      val path = activeChatIdForNotifications ?: return
      val myUid = firebaseService.uid ?: return
      val currentMessageText = _messageText.value
      if (currentMessageText.isBlank()) return

      val isResident = baseUIState.userRole == UserRole.StandardUser
      val (displayName, displayAddr) = if (isResident) {
          (baseUIState.nanim?.takeIf { it.isNotBlank() && it != "Мешканець" } ?: "Жилець") to (baseUIState.address ?: "")
      } else {
          (baseUIState.osbb ?: "") to " "
      }

      writeToDatabaseInternal(
          chatId = path,
          senderUid = myUid,
          senderDisplayedName = displayName,
          senderLogoUrl = firebaseService.photoUrl,
          senderAddress = displayAddr,
          fromAdmin = !isResident,
          imageUrl = null, fileUrl = null, fileName = null,
          recipientTokens = _recipientTokens.value,
          onComplete = { clearAiSuggestion() }
      )
  }

  private fun writeToDatabaseInternal(
    chatId: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    fromAdmin: Boolean,
    imageUrl: String?,
    fileUrl: String?,
    fileName: String?,
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
            type = if (imageUrl != null) "IMAGE" else if (fileUrl != null) "FILE" else "TEXT",
            imageUrl = imageUrl,
            fileUrl = fileUrl,
            fileName = fileName,
            timestamp = currentTimeMillis(),
            fromAdmin = fromAdmin
        )
        chatRepo.sendMessage(chatId, message)
        
        chatRepo.incrementUnreadForParticipants(chatId, senderUid)
        
        _messageText.value = ""
        onComplete()
      } finally {
        _isLoadingAfterSending.value = false
      }
    }
  }

  fun uploadFileAndSendMessage(
      filePath: String,
      chatId: String? = null,
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
      val targetChatId = chatId ?: activeChatIdForNotifications ?: getChatPath(role, osbbId, addressId)
      launchCatching {
          _isLoadingAfterSending.value = true
          try {
              val isImage = filePath.startsWith("data:image", ignoreCase = true) || 
                            filePath.lowercase().let { it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg") }
              
              val bytes = if (isImage) chatRepo.compressImage(filePath) else chatRepo.readFileAsBytes(filePath)
              if (bytes.isEmpty()) return@launchCatching
              
              val originalName = _selectedFileName.value
              val finalFileName = if (!originalName.isNullOrBlank()) originalName 
                                  else if (filePath.startsWith("data:")) "photo_${currentTimeMillis()}.jpg"
                                  else filePath.split("/").lastOrNull() ?: "file"
              
              val storagePath = "chat_images/chats/${osbbId}/${addressId}/${currentTimeMillis()}_$finalFileName"
              val url = chatRepo.uploadFile(bytes, storagePath)

              writeToDatabaseInternal(
                  chatId = targetChatId,
                  senderUid = senderUid,
                  senderDisplayedName = senderDisplayedName,
                  senderLogoUrl = senderLogoUrl,
                  senderAddress = senderAddress,
                  fromAdmin = role != UserRole.StandardUser,
                  imageUrl = if (isImage) url else null,
                  fileUrl = if (!isImage) url else null,
                  fileName = finalFileName,
                  recipientTokens = recipientTokens,
                  onComplete = { 
                      _selectedImagePath.value = null
                      _selectedFileName.value = null
                      onComplete() 
                  }
              )
          } finally {
              _isLoadingAfterSending.value = false
          }
      }
  }

  private var lastTrackedRole: UserRole? = null
  private var lastTrackedOsbbId: Long? = null
  private var lastTrackedApartmentsCount: Int = -1

  fun trackUserIdentifiersWithRole(role: UserRole, osbbId: Long, apartments: List<ApartmentEntity> = emptyList()) {
    val myUid = chatRepo.currentUid ?: ""
    if (myUid.isBlank()) return

    // ОПТИМІЗАЦІЯ: Для мешканця важлива кількість квартир, для адміна - тільки роль та ID організації
    val isResident = role == UserRole.StandardUser
    val shouldUpdate = lastTrackedRole != role || 
                       lastTrackedOsbbId != osbbId || 
                       (isResident && lastTrackedApartmentsCount != apartments.size)

    if (!shouldUpdate) return
    
    lastTrackedRole = role
    lastTrackedOsbbId = osbbId
    lastTrackedApartmentsCount = apartments.size

    activeTrackerJob?.cancel()
    subscribeToUnreadCountGlobal(myUid)

    if (isResident) {
      val residentKeys = mutableListOf<String>()
      apartments.forEach { apt ->
        residentKeys.add("OSBB_${apt.osmdId ?: 0L}_${apt.addressId}")
        residentKeys.add("WATER_SERVICE_${Constants.WATER_SERVICE_ID}_${apt.addressId}")
        residentKeys.add("WARM_SERVICE_${Constants.WARM_SERVICE_ID}_${apt.addressId}")
        residentKeys.add("GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_${apt.addressId}")
      }
      _userIdentifiersWithRole.value = residentKeys
      if (residentKeys.isNotEmpty()) subscribeToLastMessages(residentKeys)
      return
    }

    val targetPrefix = when (role) {
      UserRole.VodokanalUser -> "WATER_SERVICE_${Constants.WATER_SERVICE_ID}_"
      UserRole.YtkeUser      -> "WARM_SERVICE_${Constants.WARM_SERVICE_ID}_"
      UserRole.TboUser       -> "GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_"
      UserRole.OsbbUser      -> "OSBB_${osbbId}_"
      else                   -> "OSBB_${osbbId}_"
    }

    activeTrackerJob = screenModelScope.launch {
      println("[YkisLogKMP.Chat]: Запуск відстеження для $role (Prefix: $targetPrefix)")
      
      // ФІКС ДЛЯ WEB: Спроби ініціалізації з невеликою затримкою для App Check
      var success = false
      var attempts = 0
      while (!success && attempts < 3) {
          try {
              chatRepo.observeChatKeys(targetPrefix).first().let { keys ->
                  if (keys.isNotEmpty() || attempts == 2) { // На останній спробі приймаємо що є
                      success = true
                  }
              }
          } catch (e: Exception) {
              println("[YkisLogKMP.Chat_WARN]: Спроба $attempts не вдалася: ${e.message}")
          }
          if (!success) {
              delay(2000)
              attempts++
          }
      }

      chatRepo.observeChatKeys(targetPrefix).collect { chatKeys ->
          println("[YkisLogKMP.Chat]: Отримано ${chatKeys.size} ключів чату")
          _userIdentifiersWithRole.value = chatKeys
          if (chatKeys.isNotEmpty()) {
            subscribeToLastMessages(chatKeys)
            getUsers(chatKeys)
          }
      }
    }
  }

  private fun subscribeToUnreadCountGlobal(myUid: String) {
    unreadSubscriptionJob?.cancel()
    unreadSubscriptionJob = launchCatching(snackbar = false) {
      chatRepo.observeUnreadCounts(myUid).collect { countMap ->
        _unreadCounts.value = countMap
        applyAppBadgeCount(countMap.filter { it.key.contains("_") }.values.sum())
      }
    }
  }

  private fun subscribeToLastMessages(chatKeys: List<String>) {
    chatKeys.forEach { chatId ->
      if (chatId.isBlank() || lastMessageListeners.containsKey(chatId)) return@forEach
      lastMessageListeners[chatId] = screenModelScope.launch {
        chatRepo.observeLastMessage(chatId).collect { message ->
          if (message != null) _lastMessages.update { it + (chatId to message) }
        }
      }
    }
  }

  fun getUsers(chatKeys: List<String>) {
    if (chatKeys.isEmpty()) return
    launchCatching(snackbar = false) {
      val addressIds = chatKeys.mapNotNull { it.split("_").last().toLongOrNull() }.distinct()
      val fetchedProfiles = mutableListOf<UserEntity>()
      addressIds.forEach { addrId ->
          chatRepo.fetchUserByAddressId(addrId)?.let { fetchedProfiles.add(it) }
      }
      _rawFetchedProfiles.value = fetchedProfiles
    }
  }

  fun setSelectedImagePath(path: String?, fileName: String? = null) {
    _selectedImagePath.value = path
    _selectedFileName.value = fileName
  }

  fun setSelectedMessage(message: MessageEntity?) { _selectedMessage.value = message }
  fun setUserTyping(isTyping: Boolean) {
    activeChatIdForNotifications?.let { path ->
      screenModelScope.launch { chatRepo.setTypingStatus(path, chatRepo.currentUid ?: "", isTyping) }
    }
  }

  private fun setPresence(path: String, isOnline: Boolean) {
    val myUid = chatRepo.currentUid ?: return
    screenModelScope.launch { if (isOnline) chatRepo.setUserOnline(path, myUid) else chatRepo.setUserOffline(path, myUid) }
  }

  private fun observeOpponentPresence(path: String) {
    presenceJob?.cancel()
    presenceJob = screenModelScope.launch {
      chatRepo.observePresence(path).collect { presenceMap ->
          val myUid = chatRepo.currentUid
          _isOpponentOnline.value = presenceMap.filter { it.key != myUid }.values.any { it }
      }
    }
  }

  private fun observeTypingStatus(path: String) {
    typingStatusJob?.cancel()
    typingStatusJob = screenModelScope.launch {
      chatRepo.observeTyping(path).collect { typingMap ->
          val myUid = chatRepo.currentUid
          _isOpponentTyping.value = typingMap.filter { it.key != myUid }.values.any { it }
      }
    }
  }

  fun updateMessage(newText: String) {
    val msg = _editingMessage.value ?: return
    activeChatIdForNotifications?.let { path ->
      launchCatching {
        chatRepo.updateMessage(path, msg.id, mapOf("text" to newText, "edited" to true))
        _editingMessage.value = null
        _messageText.value = ""
      }
    }
  }

  fun startEditing(m: MessageEntity) { _editingMessage.value = m; _messageText.value = m.text }
  fun showDeleteConfirmation(m: MessageEntity) { _messageToDelete.value = m }
  fun dismissDeleteDialog() { _messageToDelete.value = null }
  fun confirmDeletion() {
    val msg = _messageToDelete.value ?: return
    activeChatIdForNotifications?.let { path ->
      launchCatching { chatRepo.removeMessage(path, msg.id); dismissDeleteDialog() }
    }
  }

  fun deleteForMe(messageId: String) {
    activeChatIdForNotifications?.let { path ->
      chatRepo.currentUid?.let { myUid ->
        launchCatching { chatRepo.deleteMessageForUser(path, messageId, myUid) }
      }
    }
  }

  fun analyzePhotoWithGemini(path: String, address: String?) {
    screenModelScope.launch {
      _isAssistantLoading.value = true
      try {
        val result = chatRepo.analyzeMeterImage("Проаналізуй фото лічильника для адреси ${address ?: "м. Южне"}", chatRepo.readFileAsBytes(path))
        _assistantResponse.value = result
      } catch (e: Exception) { logService.logNonFatalCrash(e) } 
      finally { _isAssistantLoading.value = false }
    }
  }

  fun askAssistant(prompt: String, role: UserRole, address: String) {
    screenModelScope.launch {
      _isAssistantLoading.value = true
      try {
        val result = chatRepo.askAiAssistant("Роль: $role, Адреса: $address. Питання: $prompt")
        _assistantResponse.value = result.getOrNull()
      } catch (e: Exception) { logService.logNonFatalCrash(e) }
      finally { _isAssistantLoading.value = false }
    }
  }

  fun applyAiHint() { _assistantResponse.value?.let { _messageText.value = it }; _assistantResponse.value = null }
  fun clearAiSuggestion() { _assistantResponse.value = null }
  fun startForwarding(m: MessageEntity) { _forwardingMessage.value = m }
  fun cancelForwarding() { _forwardingMessage.value = null }
  fun stopAllListeners() {
      messageSubscriptionJob?.cancel(); typingStatusJob?.cancel(); presenceJob?.cancel()
      activeTrackerJob?.cancel(); unreadSubscriptionJob?.cancel()
      lastMessageListeners.values.forEach { it.cancel() }; lastMessageListeners.clear()
  }

  fun confirmForwardToService(service: ContentDetail, baseState: BaseUIState, targetUser: UserEntity? = null) {
    val msg = _forwardingMessage.value ?: return
    val myUid = chatRepo.currentUid ?: return
    launchCatching {
      try {
        val targetPath = if (baseState.userRole == UserRole.StandardUser) {
           val sId = when(service) {
             ContentDetail.WATER_SERVICE -> Constants.WATER_SERVICE_ID
             ContentDetail.WARM_SERVICE -> Constants.WARM_SERVICE_ID
             ContentDetail.GARBAGE_SERVICE -> Constants.GARBAGE_SERVICE_ID
             else -> baseState.osbbId ?: 0L
           }
           val pref = when(service) {
             ContentDetail.WATER_SERVICE -> "WATER_SERVICE"
             ContentDetail.WARM_SERVICE -> "WARM_SERVICE"
             ContentDetail.GARBAGE_SERVICE -> "GARBAGE_SERVICE"
             else -> "OSBB"
           }
           "${pref}_${sId}_${baseState.addressId}"
        } else {
           val user = targetUser ?: return@launchCatching
           getChatPath(baseState.userRole, baseState.osbbId ?: 0L, user.addressId)
        }
        chatRepo.sendMessage(targetPath, MessageEntity(senderUid = myUid, text = msg.text, imageUrl = msg.imageUrl, timestamp = currentTimeMillis(), isForwarded = true))
        chatRepo.incrementUnreadForParticipants(targetPath, myUid)
        cancelForwarding(); SnackbarManager.showMessage(Res.string.success_send_message)
      } catch (e: Exception) { }
    }
  }

  fun loadMoreMessages() {
    val params = currentParams ?: return
    _messageLimit.value += 25
    readFromDatabase(params.first, params.second, params.third)
  }

  override fun onDispose() { stopAllListeners(); super.onDispose() }
}
