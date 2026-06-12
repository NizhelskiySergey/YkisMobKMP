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

  private val _messageLimit = MutableStateFlow(50)
  val messageLimit: StateFlow<Int> = _messageLimit.asStateFlow()
  
  private var currentParams: Triple<UserRole, Long, Long>? = null

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

  val activeChatPath: String? get() = currentChatPath

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
      // ИСПРАВЛЕНО: Обрабатываем только валидные ключи из 3-х и более частей
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
    
    if (currentChatPath == targetPath && messageSubscriptionJob?.isActive == true) return

    println("[YkisLogKMP]: [READ_START] Путь: $targetPath")

    messageSubscriptionJob?.cancel()
    messageSubscriptionJob = screenModelScope.launch {
      try {
        val activeUid = chatRepo.currentUid ?: return@launch
        currentChatPath = targetPath
        activeChatIdForNotifications = targetPath
        
        chatRepo.addChatParticipant(targetPath, activeUid)
        chatRepo.resetUnreadCount(targetPath, activeUid)
        
        observeOpponentPresence(targetPath)
        setPresence(targetPath, true)
        observeTypingStatus(targetPath)
        firebaseService.clearNotifications(targetPath)

        screenModelScope.launch {
            try {
                val parts = targetPath.split("_")
                val effectiveOrgId = if (parts.size >= 3) parts[parts.size - 2].toLongOrNull() ?: 0L else 0L

                val targetUids = if (role == UserRole.StandardUser) {
                    chatRepo.fetchAdminsByOsbb(effectiveOrgId).map { it.uid }
                } else {
                    val addressIdFromPath = parts.last().toLongOrNull() ?: 0L
                    chatRepo.fetchAllUsersByAddressId(addressIdFromPath).map { it.uid }
                }
                
                _recipientUids.value = targetUids
                
                if (targetUids.isNotEmpty()) {
                    val users = chatRepo.fetchUsersByIds(targetUids)
                    val tokens = users.flatMap { it.tokens }.distinct()
                    _recipientTokens.value = tokens
                }
            } catch (e: Exception) { }
        }

        chatRepo.observeMessages(targetPath, _messageLimit.value)
          .collect { filteredMessages ->
            _firebaseTest.value = filteredMessages.filter { !it.deletedFor.contains(activeUid) }
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
      val user = _selectedUser.value
      val role = baseUIState.userRole
      val path = currentChatPath ?: return
      val myUid = firebaseService.uid
      val currentMessageText = _messageText.value
      if (currentMessageText.isBlank() || myUid == null) return

      val isResident = role == UserRole.StandardUser
      
      // ИСПРАВЛЕНО: senderDisplayedName - это ФАМИЛИЯ, senderAddress - это АДРЕС
      val (displayName, displayAddr) = if (isResident) {
          val surname = baseUIState.nanim ?: ""
          val cleanSurname = if (surname.isNotBlank() && surname != "Мешканець") surname else "Жилець"
          cleanSurname to baseUIState.address
      } else {
          baseUIState.osbb to " " // Для админа адрес не нужен в заголовке пуша
      }

      writeToDatabaseInternal(
          chatId = path,
          senderUid = myUid,
          senderDisplayedName = displayName,
          senderLogoUrl = firebaseService.photoUrl,
          senderAddress = displayAddr,
          fromAdmin = !isResident,
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
    fromAdmin: Boolean,
    imageUrl: String?,
    fileUrl: String?,
    fileName: String?,
    recipientTokens: List<String>,
    recipientUids: List<String> = emptyList(),
    onComplete: () -> Unit
  ) {
    println("[YkisLogKMP]: [WRITE_START] Чат: $chatId, Msg: ${_messageText.value}")
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
            timestamp = currentTimeMillis(),
            fromAdmin = fromAdmin
        )
        chatRepo.sendMessage(chatId, message)
        
        val recipients = _recipientUids.value
        if (recipients.isNotEmpty()) {
            chatRepo.incrementUnreadForUids(chatId, recipients)
        }
        
        _messageText.value = ""
        onComplete()
      } finally {
        _isLoadingAfterSending.value = false
      }
    }
  }

  fun uploadFileAndSendMessage(
      filePath: String,
      chatId: String? = null, // Добавляем необязательный параметр
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
      // ИСПРАВЛЕНО: Приоритетно используем переданный chatId, затем текущий, затем реконструируем
      val targetChatId = chatId ?: currentChatPath ?: getChatPath(role, osbbId, addressId)
      
      println("[YkisLogKMP]: [UPLOAD_START] Файл: $filePath, Чат: $targetChatId, Role: $role")

      launchCatching {
          _isLoadingAfterSending.value = true
          try {
              val bytes = chatRepo.readFileAsBytes(filePath)
              if (bytes.isEmpty()) {
                  println("[YkisLogKMP_ERROR]: Файл пуст или не прочитан: $filePath")
                  return@launchCatching
              }
              
              val isImage = filePath.lowercase().let { 
                  it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp") 
              }
              val fileName = filePath.split("/").lastOrNull() ?: "file_${currentTimeMillis()}"
              val ext = if (isImage) "jpg" else filePath.substringAfterLast(".", "bin")
              
              // Путь в Storage: /chat_files/ID_ОСББ/ID_Квартиры/время.расширение
              val storagePath = "chat_files/${osbbId}/${addressId}/${currentTimeMillis()}.$ext"
              println("[YkisLogKMP]: [STORAGE_PATH] $storagePath")
              
              val url = chatRepo.uploadFile(bytes, storagePath)
              println("[YkisLogKMP]: [UPLOAD_SUCCESS] URL: $url")

              writeToDatabaseInternal(
                  chatId = targetChatId,
                  senderUid = senderUid,
                  senderDisplayedName = senderDisplayedName,
                  senderLogoUrl = senderLogoUrl,
                  senderAddress = senderAddress,
                  fromAdmin = role != UserRole.StandardUser,
                  imageUrl = if (isImage) url else null,
                  fileUrl = if (!isImage) url else null,
                  fileName = fileName,
                  recipientTokens = recipientTokens,
                  recipientUids = _recipientUids.value,
                  onComplete = { 
                      _selectedImagePath.value = null
                      onComplete() 
                  }
              )
          } catch (e: Exception) {
              println("[YkisLogKMP_ERROR]: Ошибка при загрузке/отправке: ${e.message}")
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
    val myUid = chatRepo.currentUid ?: ""
    activeTrackerJob?.cancel()
    subscribeToUnreadCountGlobal(myUid)

    if (role == UserRole.StandardUser) {
      val residentKeys = mutableListOf<String>()
      apartments.forEach { apt ->
        residentKeys.add("OSBB_${apt.osmdId ?: 0L}_${apt.addressId}")
        residentKeys.add("WATER_SERVICE_${Constants.WATER_SERVICE_ID}_${apt.addressId}")
        residentKeys.add("WARM_SERVICE_${Constants.WARM_SERVICE_ID}_${apt.addressId}")
        residentKeys.add("GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_${apt.addressId}")
      }
      _userIdentifiersWithRole.value = residentKeys
      if (residentKeys.isNotEmpty()) {
        subscribeToLastMessages(residentKeys)
      } else {
        _lastMessages.value = emptyMap()
      }
      return
    }

    val prefix = _selectedServicePrefix.value ?: "OSBB"
    val targetPrefix = when (role) {
      UserRole.VodokanalUser -> "WATER_SERVICE_${Constants.WATER_SERVICE_ID}_"
      UserRole.YtkeUser      -> "WARM_SERVICE_${Constants.WARM_SERVICE_ID}_"
      UserRole.TboUser       -> "GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_"
      UserRole.OsbbUser      -> "OSBB_${osbbId}_"
      else                   -> "${prefix}_${osbbId}_"
    }

    activeTrackerJob = screenModelScope.launch {
      chatRepo.observeChatKeys(targetPrefix).collect { chatKeys ->
          _userIdentifiersWithRole.value = chatKeys
          if (chatKeys.isNotEmpty()) {
            subscribeToLastMessages(chatKeys)
            getUsers(chatKeys)
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
        val validChats = countMap.filter { (key, _) -> key.contains("_") }
        val total = validChats.values.sum()
        applyAppBadgeCount(total)
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
      val addressIdsToFetch = chatKeys.mapNotNull { key -> 
          val parts = key.split("_")
          if (parts.size >= 3) parts.last().toLongOrNull() else null 
      }.distinct()

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
        
        cancelForwarding()
        SnackbarManager.showMessage(Res.string.success_send_message)
      } catch (e: Exception) { }
    }
  }

  fun loadMoreMessages() {
    val params = currentParams ?: return
    _messageLimit.value += 25
    readFromDatabase(params.first, params.second, params.third)
  }

  override fun onDispose() {
    stopAllListeners()
    super.onDispose()
  }
}
