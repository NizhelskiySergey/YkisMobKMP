package com.ykis.ykismobkmp.ui.screens.chat
import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.data.responses.BaseResponse
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import com.ykis.ykismobkmp.ui.screens.service.list.TotalServiceDebt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.vodokanal
import ykismobkmp.composeapp.generated.resources.ytke_short
import ykismobkmp.composeapp.generated.resources.yzhtrans

private const val className = "ChatModelsKt"

@Serializable
data class SendNotificationArguments(
  @SerialName("success") override val success: Int = 0,
  @SerialName("message") override val message: String = "",
  @SerialName("tokens") val recipientTokens: List<String> = emptyList(),
  @SerialName("title") val title: String = "",
  @SerialName("body") val body: String = "",
  @SerialName("chatId") val chatId: String = ""
) : BaseResponse

data class ServiceWithCodeName(
  val name: String = "",
  val codeName: String = ""
)



data class ChatSession(
  val chatId: String,        // Напр: "OSBB_3_1434_UID"
  val residentUid: String,
  val addressId: Int,
  val lastMessage: MessageEntity = MessageEntity(),
  val userProfile: UserEntity? = null
)

class ChatScreenModel(
  private val chatRepo: ChatRepository,
  logService: LogService
) : BaseScreenModel(logService) {

  private val className = "ChatViewModel"
  private val _assistantResponse = MutableStateFlow<String?>(null)
  val assistantResponse = _assistantResponse.asStateFlow()
  private val _quickHint = MutableStateFlow<String?>(null)
  val quickHint = _quickHint.asStateFlow()
  private var lastTypingSentTime = 0L
  private var typingStopJob: Job? = null
  private var typingIndicatorJob: Job? = null
  private var activeTrackerJob: Job? = null
  private val _isOpponentTyping = MutableStateFlow(false)
  val isOpponentTyping = _isOpponentTyping.asStateFlow()
  private val _selectedImagePath = MutableStateFlow<String?>(null)
  val selectedImagePath = _selectedImagePath.asStateFlow()

  private val _selectedService = MutableStateFlow<TotalServiceDebt?>(null)
  val selectedService = _selectedService.asStateFlow()
  private val _firebaseTest = MutableStateFlow<List<MessageEntity>>(emptyList())
  val firebaseTest = _firebaseTest.asStateFlow()
  private val _messageText = MutableStateFlow("")
  val messageText = _messageText.asStateFlow()
  private val _userIdentifiersWithRole = MutableStateFlow<List<String>>(emptyList())
  private val _rawFetchedProfiles = MutableStateFlow<List<UserEntity>>(emptyList())
  private val _lastMessages = MutableStateFlow<Map<String, MessageEntity>>(emptyMap())
  val lastMessages: StateFlow<Map<String, MessageEntity>> = _lastMessages.asStateFlow()
  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()
  val userList: StateFlow<List<UserEntity>> = combine(
    _userIdentifiersWithRole,
    _rawFetchedProfiles,
    _lastMessages,
    _searchQuery
  ) { keys, profiles, lastMsgs, query ->
    Log.d("YkisLog", "[$className.userList]: Recombining. Keys: ${keys.size}, Query: '$query'")
    val fullList = keys.mapNotNull { key ->
      val parts = key.split("_")
      if (parts.size < 4) return@mapNotNull null
      val uidFromKey = parts.last()
      val addrIdFromKey = parts.getOrNull(parts.size - 2)?.toIntOrNull() ?: 0
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
        user.displayName?.contains(query, ignoreCase = true) == true ||
          user.addressId.toString().contains(query) ||
          user.address.contains(query, ignoreCase = true)
      }
    }
  }.stateIn(
    scope = screenModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )
  private val _selectedUser = MutableStateFlow(UserEntity())
  val selectedUser = _selectedUser.asStateFlow()

  private val _isLoadingAfterSending = MutableStateFlow(false)
  val isLoadingAfterSending = _isLoadingAfterSending.asStateFlow()
  private val _selectedMessage = MutableStateFlow(MessageEntity())
  val selectedMessage = _selectedMessage.asStateFlow()
  private var currentChatPath: String? = null
  private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
  val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()
  private val _isPartnerTyping = MutableStateFlow(false)
  val isPartnerTyping = _isPartnerTyping.asStateFlow()
  private val _recipientTokens = MutableStateFlow<List<String>>(emptyList())
  val recipientTokens = _recipientTokens.asStateFlow()
  private val _messageToDelete = MutableStateFlow<MessageEntity?>(null)
  val messageToDelete = _messageToDelete.asStateFlow()
  private val _editingMessage = MutableStateFlow<MessageEntity?>(null)
  val editingMessage = _editingMessage.asStateFlow()
  private val _forwardingMessage = MutableStateFlow<MessageEntity?>(null)
  val forwardingMessage = _forwardingMessage.asStateFlow()
  val isForwardingMode = _forwardingMessage
    .map { it != null }
    .stateIn(screenModelScope, SharingStarted.Lazily, false)
  private val _pendingPushChatId = MutableStateFlow<String?>(null)
  val pendingPushChatId = _pendingPushChatId.asStateFlow()
  private val _selectedServicePrefix = MutableStateFlow<String?>(null)
  val selectedServicePrefix = _selectedServicePrefix.asStateFlow()
  private val unreadCountListeners = mutableMapOf<String, Job>()
  private val lastMessageListeners = mutableMapOf<String, Job>()
  private val typingListeners = mutableMapOf<String, Job>()
  fun setSelectedService(prefix: String?) {
    _selectedServicePrefix.value = prefix
  }

  /**
   * [ChatScreenModel.uploadFileAndSendMessage] — Универсальная загрузка медиа (Mac/Android).
   * Больше не требует Context или Uri.
   */
  fun uploadFileAndSendMessage(
    chatUid: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    addressId: Int,
    osbbId: Int,
    role: UserRole,
    recipientTokens: List<String>,
    onComplete: () -> Unit
  ) {
    val methodName = "uploadFile"
    // Берем путь к файлу из нашего KMP стейта
    val filePath = _selectedImagePath.value

    if (filePath.isNullOrBlank()) {
      Log.e("YkisLog", "[$className.$methodName]: [ABORT] Путь к файлу пуст")
      return
    }

    // Наш базовый лоадер и обработчик ошибок
    launchCatching(showLoader = true) {
      try {
        Log.d("YkisLog", "[$className.$methodName]: [START] Target Tokens: ${recipientTokens.size}")

        // 1. ОПРЕДЕЛЯЕМ ТИП ФАЙЛА ПО РАСШИРЕНИЮ (Кроссплатформенно)
        val isImage = filePath.lowercase().let {
          it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
        }
        val extension = if (isImage) "jpg" else filePath.substringAfterLast(".", "file")
        val originalFileName = filePath.substringAfterLast("/")

        // 2. ПОДГОТОВКА ДАННЫХ (Вызов платформенного кода через репозиторий)
        val fileData: ByteArray = if (isImage) {
          // На Android сработает Bitmap.recycle(), на Mac — ImageIO
          chatRepo.compressImage(filePath)
        } else {
          chatRepo.readFileAsBytes(filePath)
        }

        if (fileData.isEmpty()) {
          throw Exception("Файл порожній або недоступний для читання")
        }

        // 3. ФОРМИРОВАНИЕ СИСТЕМНОГО ПУТИ (9999/9998/9997 для служб)
        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> 9999
          UserRole.YtkeUser -> 9998
          UserRole.TboUser -> 9997
          else -> osbbId
        }

        val folder = if (isImage) "chat_images" else "chat_docs"
        // Используем наш кроссплатформенный TimeUtils
        val timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis()
        val storagePath = "$folder/$effectiveOsbbId/$addressId/$timestamp.$extension"

        // 4. ЗАГРУЗКА В FIREBASE STORAGE (GitLive)
        val downloadUrl = chatRepo.uploadFile(fileData, storagePath)
        Log.d("YkisLog", "[$className.$methodName]: [URL_READY] $downloadUrl")

        // 5. ЗАПИСЬ В DATABASE И ОТПРАВКА PUSH
        writeToDatabase(
          chatUid = chatUid,
          senderUid = senderUid,
          senderDisplayedName = senderDisplayedName,
          senderLogoUrl = senderLogoUrl,
          senderAddress = senderAddress,
          addressId = addressId,
          imageUrl = if (isImage) downloadUrl else null,
          fileUrl = if (!isImage) downloadUrl else null,
          fileName = if (!isImage) originalFileName else null,
          osbbId = effectiveOsbbId,
          role = role,
          recipientTokens = recipientTokens,
          onComplete = {
            Log.d("YkisLog", "[$className.$methodName]: [FINISH] Успішно відправлено")
            // Очищаем стейт после успеха
            _selectedImagePath.value = null
            _messageText.value = ""
            clearAiSuggestion()
            onComplete()
          }
        )

      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: [CRITICAL_ERROR] ${e.message}")
        SnackbarManager.showMessage("Помилка завантаження: перевірте з'єднання")
        logService.logNonFatalCrash(e)
      }
    }
  }

  /**
   * [ChatScreenModel.cancelForwarding] — Отмена режима пересылки сообщения.
   */
  fun cancelForwarding() {
    _forwardingMessage.value = null
    // Обновляем стейт, чтобы UI скрыл панель пересылки
    _uiState.update { it.copy(isForwarding = false) }
    Log.d("YkisLog", "[$className.cancelForwarding]: Forwarding mode cancelled")
  }

  /**
   * [ChatScreenModel.startForwarding] — Вход в режим пересылки.
   */
  fun startForwarding(message: MessageEntity) {
    _forwardingMessage.value = message
    _uiState.update { it.copy(isForwarding = true) }
    Log.d("YkisLog", "[$className.startForwarding]: Message ${message.id} ready to forward")
  }

  /**
   * [ChatScreenModel.sendForwardedMessage] — Пересылка сообщения в другой чат.
   * Использует чистые Coroutines вместо коллбэков Firebase.
   */
  private fun sendForwardedMessage(targetChatId: String) {
    val methodName = "sendForwardedMessage"
    val messageToForward = _forwardingMessage.value ?: return
    val myUid = chatRepo.currentUid ?: ""
    val myName = _uiState.value.displayName ?: "Користувач"

    Log.d(
      "YkisLog",
      "[$className.$methodName]: START. Forwarding ${messageToForward.id} -> $targetChatId"
    )

    // Используем наш базовый launchCatching (он заменит viewModelScope и лоадер)
    launchCatching(showLoader = true) {
      try {
        // 1. Подготовка копии сообщения (Кроссплатформенно)
        val forwardedMsg = messageToForward.copy(
          id = "", // Репозиторий сам создаст push().key если id пустой
          senderUid = myUid,
          timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis(),
          read = false,
          isForwarded = true,
          senderDisplayedName = myName
        )

        Log.d(
          "YkisLog",
          "[$className.$methodName]: PREPARE. Type: ${forwardedMsg.type} | Media: ${forwardedMsg.imageUrl != null}"
        )

        // 2. Запись в Firebase через репозиторий (suspend вызов)
        val result = chatRepo.sendMessage(targetChatId, forwardedMsg)

        // 3. Обработка результата
        if (result.isSuccess) {
          Log.d("YkisLog", "[$className.$methodName]: SUCCESS")
          cancelForwarding() // Сбрасываем режим пересылки
          SnackbarManager.showMessage("Повідомлення переслано")
        } else {
          throw result.exceptionOrNull() ?: Exception("Unknown error")
        }

      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: FAILED -> ${e.message}")
        SnackbarManager.showMessage("Помилка пересилання")
        logService.logNonFatalCrash(e)
      }
    }
  }
  /**
   * [ChatScreenModel.deleteForMe] — Скрыть сообщение только для текущего пользователя.
   */
  fun deleteForMe(messageId: String) {
    val methodName = "deleteForMe"
    val myUid = chatRepo.currentUid ?: return
    val path = currentChatPath ?: return

    launchCatching {
      Log.d("YkisLog", "[$className.$methodName]: Hiding message $messageId for $myUid")

      // Вызываем репозиторий для обновления списка deletedFor
      val result = chatRepo.deleteMessageForUser(path, messageId, myUid)

      if (result.isSuccess) {
        Log.d("YkisLog", "[$className.$methodName]: Success")
      } else {
        Log.e("YkisLog", "[$className.$methodName]: Failed")
        SnackbarManager.showMessage("Помилка видалення")
      }
    }
  }

  fun onServiceSelectedForResident(servicePrefix: String) {
    Log.d("YkisLog", "[$className.onServiceSelectedForResident]: Service -> $servicePrefix")
    // 1. Запоминаем службу
    setSelectedService(servicePrefix)
    // 2. Переключаем режим списка (из BaseUIState)
    // В ScreenModel мы обновляем состояние через наследника BaseUIState
    _uiState.update { it.copy(listMode = ListMode.APARTMENTS) }
  }

  /**
   * Получение счетчика непрочитанных для конкретной квартиры жильца.
   */
  fun getUnreadCountForApartment(addrId: Int): Int {
    val prefix = _selectedServicePrefix.value ?: return 0
    val myUid = chatRepo.currentUid ?: return 0
    val fullPath = "${prefix}_${addrId}_$myUid"
    return unreadCounts.value[fullPath] ?: 0
  }

  fun selectUserByUid(uid: String) {
    val user = userList.value.find { it.uid == uid }
    if (user != null) {
      _selectedUser.value = user
      Log.d("YkisLog", "[$className.selectUserByUid]: [PUSH_SYNC] User found: ${user.displayName}")
    } else {
      Log.w("YkisLog", "[$className.selectUserByUid]: [PUSH_SYNC] User $uid not loaded yet")
    }
  }

  fun setPendingPushChatId(id: String?) {
    _pendingPushChatId.value = id
  }

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  /**
   * Подписка на уведомления всех квартир жильца (ОСББ + Горслужбы).
   */
  fun subscribeToAllMyApartments(uid: String, osbbId: Int, apartments: List<Int>) {
    if (uid.isBlank()) {
      Log.e("YkisLog", "[$className.subscribeToAllMyApartments]: ABORT. UID is empty")
      return
    }
    val allChatKeys = mutableListOf<String>()
    apartments.forEach { addrId ->
      // Ветка ОСББ
      allChatKeys.add("OSBB_${osbbId}_${addrId}_$uid")
      // Ветки городских служб (Системные ID: 9999, 9998, 9997)
      allChatKeys.add("WATER_SERVICE_9998_${addrId}_$uid")
      allChatKeys.add("WARM_SERVICE_9997_${addrId}_$uid")
      allChatKeys.add("GARBAGE_SERVICE_9999_${addrId}_$uid")
    }
    Log.d("YkisLog", "[$className.subscribeToAllMyApartments]: START. Keys: ${allChatKeys.size}")
    if (allChatKeys.isNotEmpty()) {
      // Этот метод будет реализован в блоке работы с Firebase
      subscribeToUnreadCount(allChatKeys)
    }
  }

  /**
   * Логика пересылки сообщения в выбранную службу.
   */
  fun confirmForwardToService(
    service: ContentDetail,
    baseState: BaseUIState,
    targetUser: UserEntity? = null
  ) {
    // 1. ОПРЕДЕЛЯЕМ ПРЕФИКС И СИСТЕМНЫЙ ID
    val (servicePrefix, systemId) = when (service) {
      ContentDetail.OSBB -> "OSBB" to (baseState.osmdId ?: baseState.osbbId)
      ContentDetail.WATER_SERVICE -> "WATER_SERVICE" to 9998
      ContentDetail.WARM_SERVICE -> "WARM_SERVICE" to 9997
      ContentDetail.GARBAGE_SERVICE -> "GARBAGE_SERVICE" to 9999
      else -> service.name to 0
    }
    val chatId = if (baseState.userRole == UserRole.StandardUser) {
      // 2. Логика ЖИТЕЛЯ
      if (baseState.addressId == 0) {
        Log.e("YkisLog", "[$className.confirmForwardToService]: ABORT. addressId is 0")
        return
      }
      "${servicePrefix}_${systemId}_${baseState.addressId}_${baseState.uid}"
    } else {
      // 3. Логика АДМИНА
      val tUser = targetUser ?: _selectedUser.value
      if (tUser.uid.isBlank()) {
        Log.e("YkisLog", "[$className.confirmForwardToService]: ERROR. Target user undefined")
        return
      }
      val targetAddrId = tUser.addressId
      if (targetAddrId == 0) {
        Log.e("YkisLog", "[$className.confirmForwardToService]: ABORT. Target addressId is 0")
        return
      }
      val finalSysId = if (service == ContentDetail.OSBB) {
        tUser.osbbId ?: systemId
      } else {
        systemId
      }
      "${servicePrefix}_${finalSysId}_${targetAddrId}_${tUser.uid}"
    }
    Log.d("YkisLog", "[$className.confirmForwardToService]: TARGET -> $chatId")
    // 4. ОТПРАВКА (реализуем в блоке записи в БД)
    sendForwardedMessage(chatId)
  }

  fun cancelEditing() {
    Log.d("YkisLog", "[$className.cancelEditing]: Editing cancelled")
    _editingMessage.value = null
    _messageText.value = ""
  }

  fun startEditing(message: MessageEntity) {
    Log.d("YkisLog", "[$className.startEditing]: Message ID: ${message.id}")
    _editingMessage.value = message
    _messageText.value = message.text ?: "" // Переносим текст в поле ввода
  }

  /**
   * [ChatScreenModel.onMessageTextChanged] — управление вводом и статусом "печатает..."
   */
  fun onMessageTextChanged(newText: String) {
    val oldText = _messageText.value
    _messageText.value = newText

    // Обновляем статус печати только если мы не в режиме редактирования старого сообщения
    if (_editingMessage.value == null) {
      val now =
        com.ykis.ykismobkmp.core.utils.currentTimeMillis() // КМП аналог System.currentTimeMillis()

      if (newText.isNotBlank()) {
        // 1. Начало печати или интервал обновления (4 сек)
        if (oldText.isBlank() || (now - lastTypingSentTime > 4000)) {
          lastTypingSentTime = now
          setTypingStatus(true)
        }

        // 2. Таймер остановки: если замерли на 2.5 сек — гасим статус
        typingStopJob?.cancel()
        typingStopJob = screenModelScope.launch {
          kotlinx.coroutines.delay(2500)
          setTypingStatus(false)
          lastTypingSentTime = 0L
        }
      } else {
        // 3. Текст стерт — гасим немедленно
        typingStopJob?.cancel()
        if (oldText.isNotBlank()) {
          setTypingStatus(false)
          lastTypingSentTime = 0L
        }
      }
    }
  }

  /**
   * [ChatScreenModel.updateMessage] — сохранение исправленного сообщения в Firebase.
   */
  fun updateMessage(newText: String) {
    val methodName = "updateMessage"
    val msg = _editingMessage.value ?: return
    val path = currentChatPath

    if (path.isNullOrBlank()) {
      Log.e("YkisLog", "[$className.$methodName]: ERROR - currentChatPath is null")
      return
    }
    // Используем наш базовый launchCatching для обработки сетевых ошибок
    launchCatching(showLoader = true) {
      Log.d("YkisLog", "[$className.$methodName]: START. MsgId: ${msg.id}")
      val updates = mapOf(
        "text" to newText,
        "edited" to true
      )
      // Выполняем обновление через репозиторий (GitLive Firebase)
      chatRepo.updateMessage(path, msg.id, updates)
      Log.d("YkisLog", "[$className.$methodName]: SUCCESS")
      cancelEditing()
      SnackbarManager.showMessage("Повідомлення змінено")
    }
  }

  /**
   * Упрощенный метод подтверждения (если нужен быстрый вызов без лоадера)
   */
  fun confirmEdit(newText: String) {
    val msg = _editingMessage.value ?: return
    val path = currentChatPath ?: return

    screenModelScope.launch {
      try {
        chatRepo.updateMessage(path, msg.id, mapOf("text" to newText, "edited" to true))
        _editingMessage.value = null
        _messageText.value = ""
      } catch (e: Exception) {
        logService.logNonFatalCrash(e)
      }
    }
  }

  /**
   * [ChatScreenModel.setTypingIndicator] — управление моим статусом "печатает" в Firebase.
   * Использует debounce-логику: статус сбрасывается через 2 секунды тишины.
   */
  fun setTypingIndicator(chatId: String, isTyping: Boolean) {
    val methodName = "setTypingIndicator"
    val myUid = chatRepo.currentUid ?: return

    // Если мы только начали печатать (или продолжаем), обновляем статус в БД
    if (isTyping) {
      // Отменяем предыдущий таймер сброса
      typingIndicatorJob?.cancel()

      // Запускаем корутину для установки и последующего сброса
      typingIndicatorJob = screenModelScope.launch {
        try {
          // 1. Ставим статус "печатает"
          chatRepo.setTypingStatus(chatId, myUid, true)

          // 2. Ждем 2 секунды после последнего нажатия клавиши
          kotlinx.coroutines.delay(2000)

          // 3. Сбрасываем статус
          chatRepo.setTypingStatus(chatId, myUid, false)
          Log.d("YkisLog", "[$className.$methodName]: Typing status reset for $chatId")
        } catch (e: Exception) {
          Log.e("YkisLog", "[$className.$methodName]: Error -> ${e.message}")
        }
      }
    } else {
      // Если принудительно вызвали остановку (например, сообщение отправлено)
      typingIndicatorJob?.cancel()
      screenModelScope.launch {
        chatRepo.setTypingStatus(chatId, myUid, false)
      }
    }
  }

  /**
   * [ChatScreenModel.setTypingStatus] - Sets the "typing..." indicator in Realtime DB.
   */
  /**
   * [ChatScreenModel.setTypingStatus] — Обновление моего статуса печати в БД.
   */
  fun setTypingStatus(isTyping: Boolean) {
    val methodName = "setTypingStatus"
    val myUid = chatRepo.currentUid ?: return
    val path = currentChatPath ?: return // Используем сохраненный путь ветки

    if (path.isBlank()) return

    screenModelScope.launch {
      try {
        if (isTyping) {
          // Вызываем метод репозитория (тот самый План Б через Ktor/Firebase)
          chatRepo.setTypingStatus(chatId = path, uid = myUid, isTyping = true)
          Log.d("YkisLog", "[$className.$methodName]: ON for path $path")
        } else {
          chatRepo.setTypingStatus(chatId = path, uid = myUid, isTyping = false)
          Log.d("YkisLog", "[$className.$methodName]: OFF for path $path")
        }
      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: Error: ${e.message}")
        // Не критично для логгера, просто сетевая заминка
      }
    }
  }

  /**
   * [ChatScreenModel.subscribeToUnreadCount] - Monitors badges via Flow.
   * Replaces the old ValueEventListener with reactive KMP flows.
   */
  /**
   * [ChatScreenModel.subscribeToUnreadCount] — динамическое отслеживание непрочитанных в списке чатов.
   * Использует универсальный observeMessages и сохраняет Job для очистки.
   */
  fun subscribeToUnreadCount(chatKeys: List<String>) {
    val methodName = "subscribeToUnreadCount"
    val myUid = chatRepo.currentUid ?: return

    Log.d("YkisLog", "[$className.$methodName]: Keys to check: ${chatKeys.size}")

    chatKeys.forEach { chatId ->
      // Пропускаем пустые ID или те, на которые уже подписаны
      if (chatId.isBlank() || unreadCountListeners.containsKey(chatId)) return@forEach

      Log.i("YkisLog", "[$className.$methodName]: New watcher for $chatId")

      // Сохраняем Job, чтобы Voyager мог его отменить при dispose
      val job = screenModelScope.launch {
        // Используем наш существующий метод репозитория
        chatRepo.observeMessages(chatId)
          .map { messages ->
            // Считаем сообщения: не я автор + не прочитано
            messages.count { it.senderUid != myUid && !it.read }
          }
          .collect { count ->
            _unreadCounts.update { current ->
              val newMap = current + (chatId to count)
              Log.d("YkisLog", "[$className.$methodName]: $chatId -> Badge: $count")
              newMap
            }
            // TODO: updateSystemIconBadge() — через expect/actual для Mac/Android
          }
      }
      unreadCountListeners[chatId] = job
    }
  }

  override fun onDispose() {
    val methodName = "onDispose"
    Log.d("YkisLog", "[$className.$methodName]: START - Releasing resources")

    // 1. Очистка трекера поиска чатов (если там внутри корутины — тоже отменяем)
    cleanupTracker()

    // 2. Сброс текущего пути (важно, чтобы пуши не считались "прочитанными")
    currentChatPath = null

    // 3. Массовая отмена всех Job.
    // В KMP/Voyager отмена Job — это единственный способ остановить Flow от Firebase.

    // Очищаем счетчики непрочитанных
    unreadCountListeners.values.forEach { it.cancel() } // it уже Job, если мапа объявлена правильно
    unreadCountListeners.clear()

    // Очищаем превью последних сообщений
    lastMessageListeners.values.forEach { it.cancel() }
    lastMessageListeners.clear()

    // Очищаем статусы "Печатает..."
    typingListeners.values.forEach { it.cancel() }
    typingListeners.clear()

    Log.i("YkisLog", "[$className.$methodName]: SUCCESS - All Firebase watchers and Jobs cancelled")

    // 4. Важно вызвать super в конце, чтобы Voyager завершил screenModelScope
    super.onDispose()
  }


  /**
   * [ChatScreenModel.askAssistant] - Request to Gemini (through repository).
   */
  /**
   * [ChatScreenModel.askAssistant] — Запрос к ИИ-помощнику.
   * Использует Gemini через репозиторий (Ktor-реализация).
   */
  fun askAssistant(prompt: String) {
    val methodName = "askAssistant"
    if (prompt.isBlank()) return

    Log.d("YkisLog", "[$className.$methodName]: Prompt: $prompt")

    // Используем наш базовый launchCatching с лоадером
    launchCatching(showLoader = true) {
      val roleName = when (_uiState.value.userRole) {
        UserRole.VodokanalUser -> "диспетчера Водоканалу"
        UserRole.OsbbUser -> "голови ОСББ"
        else -> "мешканця квартири"
      }

      val addressInfo = _uiState.value.address ?: ""

      // Формируем системную инструкцию (можно вынести в ресурсы или константы)
      val fullPrompt = """
            Ви — помічник у мобільному додатку сфери ЖКГ. 
            Ви відповідаєте від імені $roleName. 
            Контекст квартири: $addressInfo.
            Запит користувача: $prompt
        """.trimIndent()
      // Вызов репозитория (уже настроенного на Ktor)
      val result = chatRepo.askAiAssistant(fullPrompt)
      result.onSuccess { response ->
        Log.d("YkisLog", "[$className.$methodName]: Success")
        // Обновляем StateFlow с ответом
        _assistantResponse.value = response
      }.onFailure { error ->
        Log.e("YkisLog", "[$className.$methodName]: AI Error -> ${error.message}")
        _assistantResponse.value = "Помилка ШІ: ${error.message}"
        logService.logNonFatalCrash(error)
      }
    }
  }

  /**
   * [ChatScreenModel.subscribeToLastMessages] — подписка на превью последних сообщений в списке чатов.
   */
  fun subscribeToLastMessages(chatKeys: List<String>) {
    val methodName = "subscribeToLastMessages"

    chatKeys.forEach { chatId ->
      if (chatId.isBlank() || lastMessageListeners.containsKey(chatId)) return@forEach

      Log.d("YkisLog", "[$className.$methodName]: WATCH -> $chatId")

      // Запускаем реактивный поток через репозиторий (GitLive Firebase Flow)
      val job = screenModelScope.launch {
        chatRepo.observeLastMessage(chatId).collect { message ->
          if (message != null) {
            _lastMessages.update { it + (chatId to message) }
            Log.d(
              "YkisLog",
              "[$className.$methodName]: UPDATE -> $chatId: ${message.text?.take(20)}"
            )
          }
        }
      }
      lastMessageListeners[chatId] = job
    }
  }

  /**
   * [ChatScreenModel.markMessagesAsRead] — пометка входящих сообщений прочитанными.
   */
  fun markMessagesAsRead(chatId: String) {
    val methodName = "markMessagesAsRead"
    val myUid = chatRepo.currentUid ?: return
    if (chatId.isBlank()) return

    // Используем launchCatching для обработки сетевых ошибок
    launchCatching(snackbar = false) {
      Log.d("YkisLog", "[$className.$methodName]: DB_START -> $chatId")

      // Выполняем через NonCancellable, чтобы навигация не прервала запись в БД
      kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        chatRepo.markMessagesAsRead(chatId, myUid)

        // Сбрасываем локальный счетчик непрочитанных для этого чата
        _unreadCounts.update { it + (chatId to 0) }
        Log.i("YkisLog", "[$className.$methodName]: SUCCESS -> Status updated")

        // Здесь будет вызов обновления бейджа иконки (expect/actual)
        // updateSystemIconBadge()
      }
    }
  }

  /**
   * [ChatScreenModel.showDeleteConfirmation] — подготовка к удалению.
   */
  fun showDeleteConfirmation(message: MessageEntity) {
    Log.d("YkisLog", "[$className.showDeleteConfirmation]: Ready to delete ${message.id}")
    _messageToDelete.value = message
  }

  /**
   * [ChatScreenModel.dismissDeleteDialog] — отмена удаления.
   */
  fun dismissDeleteDialog() {
    _messageToDelete.value = null
  }

  /**
   * [ChatScreenModel.confirmDeletion] — физическое удаление данных из Storage и Realtime DB.
   */
  fun confirmDeletion() {
    val methodName = "confirmDeletion"
    val message = _messageToDelete.value
    val path = currentChatPath

    if (path.isNullOrBlank() || message == null) {
      Log.e("YkisLog", "[$className.$methodName]: ERROR - Data missing")
      return
    }

    launchCatching(showLoader = true) {
      Log.d("YkisLog", "[$className.$methodName]: START -> ID: ${message.id}")

      // 1. Очистка файлов в Firebase Storage (если есть)
      message.imageUrl?.let {
        Log.d("YkisLog", "[$className.$methodName]: Deleting Image from Storage")
        chatRepo.deleteFileFromStorage(it)
      }
      message.fileUrl?.let {
        Log.d("YkisLog", "[$className.$methodName]: Deleting File from Storage")
        chatRepo.deleteFileFromStorage(it)
      }

      // 2. Удаление записи из БД
      chatRepo.removeMessage(path, message.id)

      Log.d("YkisLog", "[$className.$methodName]: SUCCESS")

      _messageToDelete.value = null
      SnackbarManager.showMessage("Повідомлення видалено")
    }
  }
  // Добавь параметр address
  // ... (ChatScreenModel continuation)

  /**
   * [ChatScreenModel.analyzePhotoWithGemini] — Analyzes the meter photo using AI.
   * @param imagePath Path to the local file (String) instead of Uri.
   */
  /**
   * [ChatScreenModel.analyzePhotoWithGemini] — Анализ фото счетчика через ИИ.
   * Использует кроссплатформенный путь к файлу и метод репозитория.
   */
  fun analyzePhotoWithGemini(imagePath: String, address: String) {
    val methodName = "analyzePhotoWithGemini"
    Log.d("YkisLog", "[$className.$methodName]: [START] Address: $address")

    launchCatching(showLoader = true) {
      try {
        // 1. Читаем байты изображения (используем ранее созданный метод)
        val imageData: ByteArray = chatRepo.readFileAsBytes(imagePath)

        if (imageData.isEmpty()) {
          Log.e("YkisLog", "[$className.$methodName]: [ABORT] Image data is empty")
          return@launchCatching
        }

        val prompt = """
                Ти — помічник системи ЖКГ. На фото — лічильник води за адресою: $address. 
                Твоє завдання:
                1. Знайти серійний номер лічильника.
                2. Знайти поточні показання (тільки цілі числа).
                3. Виведи результат суворо у форматі: 
                   Адреса: $address. Лічильник № [номер]. Показники: [число].
                
                Якщо на фото не лічильник — напиши: "Не вдалося розпізнати дані, спробуйте зробити фото чіткіше".
                Відповідай тільки українською мовою.
            """.trimIndent()

        Log.d("YkisLog", "[$className.$methodName]: Sending to Gemini via Ktor...")

        // 2. ВЫЗОВ РЕПОЗИТОРИЯ (синхронизировано с ChatRepository.analyzeMeterImage)
        val response = chatRepo.analyzeMeterImage(prompt, imageData)

        if (!response.isNullOrBlank()) {
          Log.d("YkisLog", "[$className.$methodName]: [SUCCESS]")
          _assistantResponse.value = response

          // Автоматическая вставка текста в поле ввода, если оно пустое
          if (_messageText.value.isBlank()) {
            _messageText.value = response
          }
        } else {
          Log.w("YkisLog", "[$className.$methodName]: [EMPTY] AI returned nothing")
          SnackbarManager.showMessage("ШІ не зміг розпізнати дані")
        }
      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: [ERROR] ${e.message}")
        SnackbarManager.showMessage("Помилка розпізнавання: перевірте підключення")
        logService.logNonFatalCrash(e)
      }
    }
  }

  /**
   * [ChatScreenModel.applyAiHint] — Inserts text from AI into the input field.
   */
  fun applyAiHint() {
    val textToApply = assistantResponse.value ?: quickHint.value
    if (!textToApply.isNullOrBlank()) {
      Log.d("YkisLog", "[$className.applyAiHint]: Text applied to input")
      _messageText.value = textToApply
      clearAiSuggestion()
    }
  }

  /**
   * [ChatScreenModel.clearAiSuggestion] — Clears all hints.
   */
  fun clearAiSuggestion() {
    _assistantResponse.value = null
    _quickHint.value = null
  }

  /**
   * [ChatScreenModel.initResidentChats] — Initial activation of 4 chat branches for a new resident.
   */
  fun initResidentChats(
    uid: String,
    osbbId: Int,
    addressId: Int,
    addressText: String,
    nanim: String
  ) {
    val methodName = "initResidentChats"
    Log.d("YkisLog", "[$className.$methodName]: [START] Initializing 4 branches")

    val serviceMap = mapOf(
      "OSBB" to osbbId,
      "WATER_SERVICE" to 9999,
      "WARM_SERVICE" to 9998,
      "GARBAGE_SERVICE" to 9997
    )

    screenModelScope.launch {
      serviceMap.forEach { (prefix, sysId) ->
        val chatPath = "${prefix}_${sysId}_${addressId}_$uid"
        val welcomeText = "Вітаю! Чат активовано."

        try {
          // Uses the repository for clean writing to the database (GitLive)
          chatRepo.sendMessage(
            path = chatPath,
            message = MessageEntity(
              id = "", // ID is generated by the repository via push()
              senderUid = uid,
              text = welcomeText,
              senderDisplayedName = nanim,
              senderAddress = addressText,
              timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis(),
              read = false
            )
          )
          Log.d("YkisLog", "[$className.$methodName]: Branch $chatPath initialized")
        } catch (e: Exception) {
          Log.e("YkisLog", "[$className.$methodName]: ERROR for $chatPath: ${e.message}")
        }
      }
    }
  }

  fun writeToDatabase(
    chatUid: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    addressId: Int,
    imageUrl: String?,
    fileUrl: String? = null,
    fileName: String?,
    osbbId: Int,
    role: UserRole,
    onComplete: () -> Unit,
    recipientTokens: List<String>
  ) {
    val methodName = "writeToDatabase"

    // 1. Валидация контента
    if (_messageText.value.isBlank() && imageUrl == null && fileUrl == null) {
      Log.d("YkisLog", "[$className.$methodName]: CANCEL - Empty message")
      return
    }

    launchCatching(showLoader = false) {
      // 2. Определение целевого UID (Золотой фонд логики Админа)
      val finalTargetUid = if (role != UserRole.StandardUser) {
        chatUid.ifBlank { _selectedUser.value.uid.ifBlank { "" } }
      } else {
        null
      }

      // 3. Определение системного ID предприятия
      val effectiveOsbbId = when (role) {
        UserRole.VodokanalUser -> 9999
        UserRole.YtkeUser -> 9998
        UserRole.TboUser -> 9997
        else -> osbbId
      }

      // 4. Генерация унифицированного пути чата
      val chatId = getChatPath(
        role = role,
        osbbId = effectiveOsbbId,
        addressId = addressId,
        targetUserUid = finalTargetUid
      )

      currentChatPath = chatId
      Log.d("YkisLog", "[$className.$methodName]: START. Path: $chatId")

      // 5. Определение отображаемого имени (KMP Res вместо application.getString)
      val finalDisplayName = if (role != UserRole.StandardUser) {
        try {
          when (role) {
            UserRole.VodokanalUser -> org.jetbrains.compose.resources.getString(Res.string.vodokanal)
            UserRole.YtkeUser -> org.jetbrains.compose.resources.getString(Res.string.ytke_short)
            UserRole.TboUser -> org.jetbrains.compose.resources.getString(Res.string.yzhtrans)
            UserRole.OsbbUser -> "Адміністратор ОСББ"
            else -> "Адміністратор"
          }
        } catch (e: Exception) {
          "Адміністратор"
        }
      } else {
        senderDisplayedName.substringAfter("|").trim()
      }

      val displayText =
        if (fileUrl != null && _messageText.value.isBlank()) "[Файл]" else _messageText.value

      // 6. Формирование сущности сообщения
      val messageEntity = MessageEntity(
        id = "", // ID сгенерирует репозиторий через push()
        senderUid = senderUid,
        text = displayText,
        type = when {
          imageUrl != null -> "IMAGE"
          fileUrl != null -> "FILE"
          else -> "TEXT"
        },
        senderLogoUrl = senderLogoUrl,
        senderDisplayedName = finalDisplayName,
        senderAddress = senderAddress,
        imageUrl = imageUrl,
        fileUrl = fileUrl,
        fileName = fileName,
        timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis(),
        read = false
      )

      _isLoadingAfterSending.value = true

      // 7. Запись в БД через репозиторий
      val result = chatRepo.sendMessage(chatId, messageEntity)

      _isLoadingAfterSending.value = false

      if (result.isSuccess) {
        Log.d("YkisLog", "[$className.$methodName]: SUCCESS")
        _messageText.value = ""
        _selectedImagePath.value = null // Очистка KMP пути
        clearAiSuggestion()
        onComplete()

        // Здесь логика отправки Push-уведомлений через KtorApiService
        // sendPushNotification(recipientTokens, ...)
      } else {
        Log.e("YkisLog", "[$className.$methodName]: ERROR -> ${result.exceptionOrNull()?.message}")
        SnackbarManager.showMessage("Помилка відправки")
      }
    }
  }

  /**
   * [ChatScreenModel.observeTypingStatus] — отслеживание статуса "печатает..." в реальном времени.
   */
  private fun observeTypingStatus(chatId: String) {
    val methodName = "observeTypingStatus"
    val myUid = chatRepo.currentUid ?: return

    // 1. Отменяем старую подписку для этого чата, если она была
    typingListeners[chatId]?.cancel()

    // 2. Запускаем новую корутину
    val job = screenModelScope.launch {
      Log.d("YkisLog", "[$className.$methodName]: START watching typing for $chatId")

      // В Firebase Realtime это ветка, например: presence/chatId/uid/typing
      chatRepo.observeTyping(chatId)
        .collect { typingMap ->
          // Фильтруем, чтобы не видеть свой собственный статус
          val someoneIsTyping = typingMap.filterKeys { it != myUid }.values.any { it == true }

          // Обновляем UI стейт
          _uiState.update { it.copy(isOpponentTyping = someoneIsTyping) }

          if (someoneIsTyping) {
            Log.d("YkisLog", "[$className.$methodName]: Someone is typing in $chatId...")
          }
        }
    }

    // Сохраняем для автоматической очистки в onDispose
    typingListeners[chatId] = job
  }

  /**
   * [ChatScreenModel.setPresence] — Установка статуса Online/Offline.
   */
  fun setPresence(chatId: String, isOnline: Boolean) {
    val methodName = "setPresence"
    val myUid = chatRepo.currentUid ?: return

    screenModelScope.launch {
      try {
        if (isOnline) {
          Log.i("YkisLog", "[$className.$methodName]: ON for $chatId")
          chatRepo.setUserOnline(chatId, myUid)
        } else {
          Log.i("YkisLog", "[$className.$methodName]: OFF for $chatId")
          chatRepo.setUserOffline(chatId, myUid)
        }
      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: FAIL -> ${e.message}")
      }
    }
  }
  /**
   * [ChatScreenModel.readFromDatabase] — Основной метод загрузки истории чата.
   * Заменяет старый механизм слушателей на реактивный Flow.
   */
  /**
   * [ChatScreenModel.readFromDatabase] — Подписка на сообщения чата.
   * Использует реактивные потоки GitLive и управляет состоянием экрана.
   */
  fun readFromDatabase(role: UserRole, senderUid: String, osbbId: Int, addressId: Int) {
    val methodName = "readFromDatabase"
    Log.i("YkisLog", "[$className.$methodName]: EXTERNAL_CALL. Addr: $addressId")

    // Используем screenModelScope (Voyager), чтобы подписка жила вместе с экраном
    screenModelScope.launch {
      try {
        // 1. Ожидание авторизации (UID)
        var activeUid = chatRepo.currentUid
        var attempts = 0
        while (activeUid == null && attempts < 20) {
          attempts++
          kotlinx.coroutines.delay(200)
          activeUid = chatRepo.currentUid
        }

        if (activeUid == null) {
          Log.e("YkisLog", "[$className.$methodName]: ABORT. UID not found after timeout")
          return@launch
        }

        // 2. Определение системного OSBB ID (9999/9998/9997)
        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> 9999
          UserRole.YtkeUser -> 9998
          UserRole.TboUser -> 9997
          else -> osbbId
        }

        // 3. Формирование пути к ветке чата
        // Внутри ChatScreenModel.kt
        val targetPath = getChatPath(
          role = role,
          osbbId = effectiveOsbbId,
          addressId = addressId,
          targetUserUid = if (role != UserRole.StandardUser) senderUid else null // ЗАМЕНИЛИ residentUid на targetUserUid
        )


        // 4. Проверка на дубликат подписки
        if (currentChatPath == targetPath && _firebaseTest.value.isNotEmpty()) {
          Log.d("YkisLog", "[$className.$methodName]: SKIP. Path $targetPath is already active")
          setPresence(targetPath, true)
          markMessagesAsRead(targetPath)
          return@launch
        }

        Log.i("YkisLog", "[$className.$methodName]: INIT. Subscribing to: $targetPath")
        currentChatPath = targetPath

        // Очищаем старый список перед подпиской
        _firebaseTest.value = emptyList()

        // 5. ЗАПУСК ПОТОКА ДАННЫХ (Flow из ChatRepository)
        chatRepo.observeMessages(targetPath)
          .map { messages ->
            // Фильтруем (скрываем удаленные для пользователя) и сортируем по времени
            messages.filter { msg ->
              val deletedList = msg.deletedFor
              !deletedList.contains(activeUid)
            }.sortedBy { it.timestamp }
          }
          .collect { filteredMessages ->
            Log.d(
              "YkisLog",
              "[$className.$methodName]: DATA_RECEIVED. Size: ${filteredMessages.size}"
            )

            // Обновляем UI стейт (или MutableStateFlow)
            _firebaseTest.value = filteredMessages

            // Системные действия при получении данных
            markMessagesAsRead(targetPath)
            setPresence(targetPath, true)

            // Подписка на статус "Печатает..." для этой ветки
            observeTypingStatus(targetPath)
          }

      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: CRITICAL_ERROR. ${e.message}")
        logService.logNonFatalCrash(e)
        // Здесь можно добавить показ Snackbar через Manager
      }
    }
  }

  /**
   * [ChatScreenModel.clearCurrentChatPath] — Полная очистка контекста при выходе.
   */
  fun clearCurrentChatPath() {
    val methodName = "clearCurrentChatPath"
    val chatId = currentChatPath

    if (chatId == null) {
      Log.d("YkisLog", "[$className.$methodName]: SKIP. Path already null")
      return
    }
    Log.d("YkisLog", "[$className.$methodName]: START. Cleaning: $chatId")

    currentChatPath = null
    _isPartnerTyping.value = false

    // Сброс статусов в Firebase
    setTypingStatus(false)
    setPresence(chatId, false)

    Log.i("YkisLog", "[$className.$methodName]: SUCCESS. Context cleared")
  }

  /**
   * [ChatScreenModel.deleteChatThreads] — Удаление всех 4-х веток чата (для удаления квартиры).
   */
  fun deleteChatThreads(uid: String, osbbId: Int, addressId: Int) {
    val methodName = "deleteChatThreads"
    Log.d("YkisLog", "[$className.$methodName]: INPUT. UID: $uid, Addr: $addressId")

    // Используем NonCancellable, чтобы удаление не прервалось при закрытии экрана
    screenModelScope.launch(kotlinx.coroutines.NonCancellable) {
      val chatKeys = listOf(
        "OSBB_${osbbId}_${addressId}_$uid",
        "WATER_SERVICE_9999_${addressId}_$uid",
        "WARM_SERVICE_9998_${addressId}_$uid",
        "GARBAGE_SERVICE_9997_${addressId}_$uid"
      )
      chatKeys.forEach { path ->
        try {
          Log.d("YkisLog", "[$className.$methodName]: Removing path: $path")
          chatRepo.removeChatBranch(path)
        } catch (e: Exception) {
          Log.e("YkisLog", "[$className.$methodName]: ERROR for $path: ${e.message}")
        }
      }
      Log.d("YkisLog", "[$className.$methodName]: Cleanup finished")
    }
  }
  /**
   * Отслеживает список активных чатов (ID пользователей) для конкретной роли админа.
   * Например: админ ОСББ 105 увидит только ветки, начинающиеся на "OSBB_105_".
   */
  /**
   * [ChatScreenModel.trackUserIdentifiersWithRole] — Мониторинг появления новых веток чатов (для Админа).
   * Использует диапазон ключей для фильтрации в Realtime Database.
   */
  /**
   * [ChatScreenModel.trackUserIdentifiersWithRole] — мониторинг списка чатов для диспетчера/админа.
   */
  fun trackUserIdentifiersWithRole(role: UserRole, osbbId: Int?) {
    val methodName = "trackUserIdentifiers"
    Log.d("YkisLog", "[$className.$methodName]: ENTRY. Role: $role | OsbbId: $osbbId")

    // Жителям (StandardUser) трекер не нужен, они видят только свои чаты
    if (role == UserRole.StandardUser) {
      Log.d("YkisLog", "[$className.$methodName]: CANCEL - Resident does not need tracker")
      cleanupTracker()
      return
    }

    // 1. Формируем префикс диапазона согласно системным ID (9999/9998/9997)
    val targetPrefix = when (role) {
      UserRole.VodokanalUser -> "WATER_SERVICE_9999_"
      UserRole.YtkeUser -> "WARM_SERVICE_9998_"
      UserRole.TboUser -> "GARBAGE_SERVICE_9997_"
      UserRole.OsbbUser -> "OSBB_${osbbId ?: 0}_"
      else -> "UNKNOWN_"
    }

    // 2. Очистка старой подписки перед запуском новой
    cleanupTracker()

    // 3. Запуск нового реактивного мониторинга через ScreenModelScope
    activeTrackerJob = screenModelScope.launch {
      Log.d("YkisLog", "[$className.$methodName]: ACTIVE - Watching prefix '$targetPrefix'")

      // Используем репозиторий для получения Flow списка веток чатов
      chatRepo.observeChatKeys(targetPrefix)
        .collect { chatKeys ->
          Log.d("YkisLog", "[$className.$methodName]: DATA - Found ${chatKeys.size} keys")

          val currentKeys = _userIdentifiersWithRole.value
          val keysIdentical = chatKeys.sorted() == currentKeys.sorted()
          val uiPopulated = userList.value.isNotEmpty()

          _userIdentifiersWithRole.value = chatKeys

          if (chatKeys.isNotEmpty()) {
            // Очищаем старые Job-ы подписок перед обновлением (предотвращаем утечки на Mac)
            lastMessageListeners.values.forEach { it.cancel() }
            lastMessageListeners.clear()

            unreadCountListeners.values.forEach { it.cancel() }
            unreadCountListeners.clear()

            // Запускаем новые подписки на превью последних сообщений и счетчики непрочитанных
            subscribeToUnreadCount(chatKeys)
            subscribeToLastMessages(chatKeys)

            // Если ключи изменились или список пуст — загружаем профили пользователей
            if (!keysIdentical || !uiPopulated) {
              getUsers()
            }
          } else {
            Log.w("YkisLog", "[$className.$methodName]: EMPTY - No chats found")
            _rawFetchedProfiles.value = emptyList()
            _unreadCounts.value = emptyMap()
          }

          // Обновление иконки приложения (через expect/actual)
          updateSystemIconBadge()
        }
    }
  }


  /**
   * [ChatScreenModel.updateSystemIconBadge] — Обновление бейджа на иконке приложения.
   * В KMP реализуется через expect/actual хелпер.
   */
  private fun updateSystemIconBadge() {
    val methodName = "updateSystemIconBadge"
    try {
      val totalCount = _unreadCounts.value.values.sum()
      Log.d("YkisLog", "[$className.$methodName]: Count -> $totalCount")

      // Вызов кроссплатформенного метода (реализуем в платформенном слое)
      com.ykis.ykismobkmp.core.utils.applyAppBadgeCount(totalCount)

    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.$methodName]: ERROR -> ${e.message}")
    }
  }

  private fun cleanupTracker() {
    activeTrackerJob?.cancel()
    activeTrackerJob = null
  }

  /**
   * [ChatScreenModel.getUsers] — Загрузка профилей пользователей по списку UID.
   */
  fun getUsers() {
    val methodName = "getUsers"
    val chatKeys = _userIdentifiersWithRole.value

    if (chatKeys.isEmpty()) {
      Log.d("YkisLog", "[$className.$methodName]: CANCEL - No keys")
      return
    }

    launchCatching(snackbar = false) {
      Log.d("YkisLog", "[$className.$methodName]: START")

      // 1. Извлекаем уникальные UID (последняя часть ключа)
      val uidsToFetch = chatKeys.map { it.substringAfterLast("_") }
        .filter { it.isNotEmpty() }
        .distinct()

      Log.d("YkisLog", "[$className.$methodName]: Fetching profiles for: $uidsToFetch")

      // 2. Запрос к репозиторию
      val fetchedProfiles = chatRepo.fetchUsersByIds(uidsToFetch)

      // 3. Обновление стейта (спровоцирует пересчет combine в userList)
      _rawFetchedProfiles.value = fetchedProfiles
      Log.d(
        "YkisLog",
        "[$className.$methodName]: SUCCESS - Loaded ${fetchedProfiles.size} profiles"
      )
    }
  }


  // ... (продолжение ChatScreenModel)

  /**
   * [ChatScreenModel.subscribeToResidentCounters] — Активация 4-х служб и сбор токенов админов.
   * Вызывается при первом входе жильца в раздел чатов.
   */
  fun subscribeToResidentCounters(
    uid: String,
    osbbId: Int,
    addressId: Int,
    addressText: String = "",
    nanim: String = ""
  ) {
    val methodName = "subscribeToResidentCounters"

    // 1. УНИФИЦИРОВАННЫЕ КЛЮЧИ (Золотой фонд)
    val chatKeys = listOf(
      "OSBB_${osbbId}_${addressId}_$uid",
      "WATER_SERVICE_9999_${addressId}_$uid",
      "WARM_SERVICE_9998_${addressId}_$uid",
      "GARBAGE_SERVICE_9997_${addressId}_$uid"
    )

    Log.d("YkisLog", "[$className.$methodName]: START. Addr: $addressId")

    // Инициализация пустых веток приветственным сообщением
    screenModelScope.launch {
      chatKeys.forEach { chatPath ->
        try {
          val exists = chatRepo.isChatBranchExists(chatPath)
          if (!exists) {
            Log.d("YkisLog", "[$className.$methodName]: INIT_USER_MSG -> $chatPath")

            val welcomeText = if (addressText.isNotEmpty()) {
              "$addressText (о/р $addressId). Чат активовано."
            } else {
              "Чат активовано."
            }

            // В KMP (GitLive) используем обычную Entity.
            // ServerValue.TIMESTAMP заменим на текущее время устройства (стандарт KMP)
            val welcomeMsg = MessageEntity(
              id = "init",
              senderUid = uid,
              senderDisplayedName = nanim,
              senderAddress = addressText,
              text = welcomeText,
              timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis(),
              read = false,
              type = "TEXT"
            )

            chatRepo.sendMessage(chatPath, welcomeMsg)
          }
        } catch (e: Exception) {
          Log.e("YkisLog", "[$className.$methodName]: ERROR path $chatPath: ${e.message}")
        }
      }
    }

    // 2. СБОР ТОКЕНОВ АДМИНОВ ДЛЯ НОТИФИКАЦИЙ
    launchCatching(snackbar = false) {
      try {
        val admins = chatRepo.fetchAdminsByOsbb(osbbId)
        val adminTokens = admins.flatMap { it.tokens }.distinct()
        _recipientTokens.value = adminTokens
        Log.d("YkisLog", "[$className.$methodName]: TOKENS -> ${adminTokens.size} collected")
      } catch (e: Exception) {
        Log.e("YkisLog", "[$className.$methodName]: TOKENS_ERROR -> ${e.message}")
      }
    }

    // 3. ПОДПИСКА НА БЕЙДЖИ
    subscribeToUnreadCount(chatKeys)
  }

  /**
   * [ChatScreenModel.setSelectedService] — Установка выбранной службы.
   * Влияет на фильтрацию списка и формирование путей Firebase.
   */
  fun setSelectedService(totalServiceDebt: TotalServiceDebt?) {
    val methodName = "setSelectedService"

    if (totalServiceDebt == null) {
      _selectedService.value = null
      _selectedServicePrefix.value = null
      Log.d("YkisLog", "[$className.$methodName]: RESET")
      return
    }

    _selectedService.value = totalServiceDebt

    // Префикс берется из Enum (например, WATER_SERVICE)
    val serviceCode = totalServiceDebt.contentDetail.name
    _selectedServicePrefix.value = serviceCode

    Log.d("YkisLog", "[$className.$methodName]: SELECT -> ${totalServiceDebt.name} ($serviceCode)")
  }

  /**
   * [ChatScreenModel.addChatListener] — Динамическое обновление превью сообщения.
   * Replaced: В KMP лучше использовать subscribeToLastMessages, но сохраняем для совместимости.
   */
  fun addChatListener(
    chatUid: String,
    onLastMessageChange: (MessageEntity) -> Unit
  ) {
    screenModelScope.launch {
      Log.d("YkisLog", "[$className.addChatListener]: Subscribing to -> $chatUid")

      chatRepo.observeLastMessage(chatUid)
        .collect { latestMessage ->
          val message = latestMessage ?: MessageEntity(text = "Немає повідомлень")
          Log.d("YkisLog", "[$className.addChatListener]: Update for $chatUid")
          onLastMessageChange(message)
        }
    }
  }


  /**
   * Комплексный метод: сжимает фото, загружает его в Storage и отправляет сообщение в чат.
   */
  /**
   * [ChatScreenModel.uploadFileAndSendMessage] — Загрузка медиафайла и отправка ссылки в чат.
   * Очищено от Android-зависимостей. Работает на Mac и Android.
   */

  fun setSelectedImagePath(path: String?) {
    _selectedImagePath.value = path
    // Логируем для отладки на Mac
    Log.d("YkisLog", "[$className.setSelectedImagePath]: Path updated to: $path")
  }


  /**
   * Удаляет сообщение из Realtime Database.
   * Использует ту же логику формирования ID чата, что и при чтении/записи.
   */


  fun setSelectedMessage(message: MessageEntity) {
    _selectedMessage.value = message
  }


  // ChatScreenModel.kt
  fun getChatPath(
    role: UserRole,
    osbbId: Int,
    addressId: Int,
    targetUserUid: String?
  ): String {
    val methodName = "ChatViewModel.getChatPath"
    val myUid = chatRepo.currentUid ?: ""
    val residentUid = targetUserUid ?: myUid
    val serviceInState = selectedService.value?.name

    Log.d(
      "YkisLog",
      "$methodName: [INPUT_PARAMS] Role: $role | In_OSBB: $osbbId | In_Addr: $addressId | Target: $targetUserUid"
    )

    // 1. Определение префикса и системного ID
    val (prefix, sysId) = when {
      role == UserRole.VodokanalUser || serviceInState == "WATER_SERVICE" -> {
        Log.d("YkisLog", "$methodName: [MATCH] Detected WATER_SERVICE")
        "WATER_SERVICE" to 9999
      }

      role == UserRole.YtkeUser || serviceInState == "WARM_SERVICE" -> {
        Log.d("YkisLog", "$methodName: [MATCH] Detected WARM_SERVICE")
        "WARM_SERVICE" to 9998
      }

      role == UserRole.TboUser || serviceInState == "GARBAGE_SERVICE" -> {
        Log.d("YkisLog", "$methodName: [MATCH] Detected GARBAGE_SERVICE")
        "GARBAGE_SERVICE" to 9997
      }

      else -> {
        Log.d("YkisLog", "$methodName: [MATCH] Defaulting to OSBB")
        "OSBB" to osbbId
      }
    }

    // 2. Сборка финального пути
    // Если addressId все еще 9997/9999, значит проблема в вызывающем методе (getUsers или openChat)
    val path = "${prefix}_${sysId}_${addressId}_$residentUid"

    if (addressId == sysId) {
      Log.e(
        "YkisLog",
        "$methodName: [CRITICAL_WARNING] AddrID ($addressId) is IDENTICAL to SysID ($sysId)!"
      )
    }

    Log.d(
      "YkisLog",
      "$methodName: [RESULT] $path | Role: $role | Final_SysID: $sysId | Final_AddrID: $addressId"
    )

    return path
  }

  // ... (продолжение ChatScreenModel)

  /**
   * [ChatScreenModel.openChatWithUser] — Инициализация контекста чата для Админа.
   */
  fun openChatWithUser(
    user: UserEntity,
    currentRole: UserRole,
    currentOsbbId: Int
  ) {
    val methodName = "openChatWithUser"
    val realAddressId = user.addressId

    Log.d("YkisLog", "--- [$className.$methodName] ---")
    Log.d("YkisLog", "Target: ${user.displayName} | Role: $currentRole")

    // 1. Устанавливаем собеседника
    _selectedUser.value = user

    // 2. Логика системного ID для служб (Золотой фонд)
    val effectiveOsbbId = when (currentRole) {
      UserRole.VodokanalUser -> 9999
      UserRole.YtkeUser -> 9998
      UserRole.TboUser -> 9997
      else -> currentOsbbId
    }

    // 3. Формируем путь (используем нашу KMP функцию)
    val chatId = getChatPath(
      role = currentRole,
      osbbId = effectiveOsbbId,
      addressId = realAddressId,
      targetUserUid = user.uid
    )

    // 4. Сброс счетчиков и статусы
    _unreadCounts.update { it + (chatId to 0) }
    markMessagesAsRead(chatId)
    observeTypingStatus(chatId)

    // 5. Запуск загрузки истории чата
    readFromDatabase(
      role = currentRole,
      senderUid = user.uid,
      osbbId = effectiveOsbbId,
      addressId = realAddressId
    )
  }
}



