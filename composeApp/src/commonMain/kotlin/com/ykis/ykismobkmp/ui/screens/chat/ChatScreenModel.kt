package com.ykis.ykismobkmp.ui.screens.chat

import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.core.Constants
import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.applyAppBadgeCount
import com.ykis.ykismobkmp.core.utils.currentTimeMillis
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.entity.UserEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.screens.ledger.list.TotalServiceDebt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "ChatViewModel"

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
  private var activeTrackerJob: Job? = null
  private var messageSubscriptionJob: Job? = null // ИСПРАВЛЕНО: Ссылка на текущую подписку сообщений

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

  /**
   * [userList] — Синхронизированный список чатов-квартир для жильцов и администраторов ОСББ.
   */
  val userList: StateFlow<List<UserEntity>> = combine(
    _userIdentifiersWithRole,
    _rawFetchedProfiles,
    _lastMessages,
    _searchQuery
  ) { keys, profiles, lastMsgs, query ->
    Log.i("Рекомбінація списку кімнат чату. Ключів: ${keys.size}, Пошук: '$query'", tag = className)

    val fullList = keys.mapNotNull { key ->
      val parts = key.split("_")
      if (parts.size < 4) return@mapNotNull null

      val uidFromKey = parts.last()
      // Исправлено: парсинг ID лицевого счета переведен на надежный toLongOrNull()
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

  private val _selectedUser = MutableStateFlow(UserEntity())
  val selectedUser = _selectedUser.asStateFlow()

  private val _isLoadingAfterSending = MutableStateFlow(false)
  val isLoadingAfterSending = _isLoadingAfterSending.asStateFlow()

  private val _selectedMessage = MutableStateFlow(MessageEntity())
  val selectedMessage = _selectedMessage.asStateFlow()

  private var currentChatPath: String? = null

  private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
  val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

  // ИСПРАВЛЕНО: Глобальный маппинг статусов печати для всех видимых чатов (бейдж в списке)
  private val _globalTypingStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val globalTypingStatuses = _globalTypingStatuses.asStateFlow()

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
   * [uploadFileAndSendMessage] — Пакетный метод загрузки медиа/документов в облачное хранилище ЮКІС
   * и последующей фиксации транзакции сообщения в распределенной базе данных.
   *
   * ПОЯСНЕНИЕ: Метод сжимает картинки через репозиторий, определяет целевую папку на основе роли
   * (Водоканал, Теплосеть, ЖЕК/ОСББ) и отправляет Push-токены получателям для генерации уведомлений на смартфоне.
   */
  fun uploadFileAndSendMessage(
    chatUid: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    addressId: Long, // Жесткий КМР Long-стандарт Единого Хаба
    osbbId: Long,    // Жесткий КМР Long-стандарт Единого Хаба
    role: UserRole,
    recipientTokens: List<String>,
    onComplete: () -> Unit
  ) {
    val methodName = "uploadFileAndSendMessage"
    val filePath = _selectedImagePath.value

    if (filePath.isNullOrBlank()) {
      println("[YkisLogKMP.$className.$methodName]: [ABORT] Путь к медиа-файлу пуст")
      return
    }

    // ИСПРАВЛЕНО: Мгновенно блокируем интерфейс, чтобы избежать дубликатов при повторных нажатиях
    _isLoadingAfterSending.value = true

    launchCatching(showLoader = false) {
      try {
        println("[YkisLogKMP.$className.$methodName]: [START] Target Tokens для PUSH-сигналов: ${recipientTokens.size}")

        // Кроссплатформенное определение типа контента по расширению файла
        val isImage = filePath.lowercase().let {
          it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
        }
        val extension = if (isImage) "jpg" else filePath.substringAfterLast(".", "file")
        val originalFileName = filePath.substringAfterLast("/")

        // Атомарное чтение бинарных данных на уровне файловой системы операционной системы (Android/iOS)
        val fileData: ByteArray = if (isImage) {
          chatRepo.compressImage(filePath)
        } else {
          chatRepo.readFileAsBytes(filePath)
        }

        if (fileData.isEmpty()) {
          throw Exception("Файл порожній або недоступний для читання на рівні ОС")
        }

        // Виртуальный маппинг системных идентификаторов для городских коммунальных предприятий г. Южного
        val effectiveOsbbId = when (role) {
          UserRole.VodokanalUser -> Constants.WATER_SERVICE_ID
          UserRole.YtkeUser      -> Constants.WARM_SERVICE_ID
          UserRole.TboUser       -> Constants.GARBAGE_SERVICE_ID
          else                   -> osbbId
        }

        val folder = if (isImage) "chat_images" else "chat_docs"
        val timestamp = currentTimeMillis()

        // Формируем бронебойный изолированный путь к файлу в облаке, исключая коллизии имен пользователей
        val storagePath = "$folder/$effectiveOsbbId/$addressId/$timestamp.$extension"
        val downloadUrl = chatRepo.uploadFile(fileData, storagePath)

        println("[YkisLogKMP.$className.$methodName]: [URL_READY] Медіа успішно завантажено в хмару. Посилання: $downloadUrl")

        // Запись транзакции сообщения. Все типы данных жестко синхронизированы на Long!
        // Любые опасные ToInt() касты, вызывавшие Integer Overflow, полностью ликвидированы!
        writeToDatabase(
          chatUid = chatUid,
          senderUid = senderUid,
          senderDisplayedName = senderDisplayedName,
          senderLogoUrl = senderLogoUrl,
          senderAddress = senderAddress,
          addressId = addressId, // Передаем чистый Long
          imageUrl = if (isImage) downloadUrl else null,
          fileUrl = if (!isImage) downloadUrl else null,
          fileName = if (!isImage) originalFileName else null,
          osbbId = effectiveOsbbId, // Передаем чистый Long
          role = role,
          recipientTokens = recipientTokens,
          onComplete = {
            println("[YkisLogKMP.$className.$methodName]: [FINISH] Сообщение с файлом успешно зафиксировано в базе")

            // Атомарно очищаем стейты ввода и подсказок Gemini AI после успешной транзакции
            _selectedImagePath.value = null
            _messageText.value = ""
            clearAiSuggestion()
            _isLoadingAfterSending.value = false // Разблокируем интерфейс
            onComplete()
          }
        )
      } catch (e: Exception) {
        _isLoadingAfterSending.value = false // Разблокируем при ошибке
        println("[YkisLogKMP.$className.$methodName]: [CRITICAL_ERROR] Перехвачен сбой корутины загрузки: ${e.message}")
        SnackbarManager.showMessage("Ошибка загрузки: проверьте соединение с сетью")
        logService.logNonFatalCrash(e) // Автоматический КМР лог в Crashlytics
      }
    }
  }

  /**
   * [cancelForwarding] — Выход из режима пересылки сообщений.
   * ПОЯСНЕНИЕ: Обнуление переменной автоматически переключает зависимый StateFlow поток isForwardingMode.
   */
  fun cancelForwarding() {
    _forwardingMessage.value = null
    Log.i("Режим пересилання повідомлень скасовано.", tag = className)
  }

  /**
   * [startForwarding] — Активация буфера пересылки конкретного сообщения.
   */
  fun startForwarding(message: MessageEntity) {
    _forwardingMessage.value = message
    Log.i("Повідомлення з ID: ${message.id} додано в буфер пересилання.", tag = className)
  }

  /**
   * [sendForwardedMessage] — Копирование и отправка пересылаемого сообщения в целевую комнату чата.
   */
  private fun sendForwardedMessage(targetChatId: String) {
    val methodName = "sendForwardedMessage"
    val messageToForward = _forwardingMessage.value ?: return
    val myUid = chatRepo.currentUid ?: ""

    // Подтягиваем имя текущей сессии абонента из базового UI-состояния
    val myName = _rawFetchedProfiles.value.find { it.uid == myUid }?.displayName ?: "Користувач ЮКІС"

    println("[YkisLogKMP.$className.$methodName]: [START] Пересилання сообщения ${messageToForward.id} -> до кімнати: $targetChatId")

    launchCatching(showLoader = true) {
      try {
        // Создаем клон сообщения с обновлением временных меток и установкой флага пересылки (isForwarded)
        val forwardedMsg = messageToForward.copy(
          id = "",
          senderUid = myUid,
          timestamp = currentTimeMillis(),
          read = false,
          isForwarded = true,
          senderDisplayedName = myName
        )

        println("[YkisLogKMP.$className.$methodName]: [PREPARE] Тип контенту: ${forwardedMsg.type} | Наявність медіа: ${forwardedMsg.imageUrl != null}")

        val result = chatRepo.sendMessage(targetChatId, forwardedMsg)
        if (result.isSuccess) {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Повідомлення успішно переслано.")
          cancelForwarding() // Сбрасываем режим пересылки
          SnackbarManager.showMessage("Повідомлення переслано")
        } else {
          throw result.exceptionOrNull() ?: Exception("Unknown КМР Firebase Network Error")
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [FAILED] Помилка корутини пересилання: ${e.message}")
        SnackbarManager.showMessage("Помилка пересилання")
        logService.logNonFatalCrash(e)
      }
    }
  }

  /**
   * [deleteForMe] — Локальное скрытие сообщения из персональной ленты пользователя.
   * ПОЯСНЕНИЕ: Сообщение помечается скрытым в ветке конкретного UID, оставаясь видимым для оппонента.
   */
  fun deleteForMe(messageId: String) {
    val methodName = "deleteForMe"
    val myUid = chatRepo.currentUid ?: return
    val path = currentChatPath ?: return

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: Приховування повідомлення $messageId для поточного користувача: $myUid")
      val result = chatRepo.deleteMessageForUser(path, messageId, myUid)
      if (result.isSuccess) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Повідомлення успішно приховано з локальної стрічки.")
      } else {
        println("[YkisLogKMP.$className.$methodName]: [FAILED] Помилка транзакції приховування повідомлення.")
        SnackbarManager.showMessage("Помилка видалення")
      }
    }
  }


  /**
   * [onServiceSelectedForResident] — Переключение подмодуля чата на список квартир конкретной ЖКХ-службы.
   */
  fun onServiceSelectedForResident(servicePrefix: String) {
    Log.i("Вибрано префікс служби чату -> $servicePrefix", tag = className)
    // 1. Запоминаем выбранный префикс коммунальной компании
    setSelectedService(servicePrefix)

    // Исправлено: Ошибочный _uiState удален. Направление кадра координируется сквозным стейтом
    Log.i("Статус фільтрації оновлено в ОЗУ.", tag = className)
  }

  /**
   * [selectUserByUid] — Фокусировка на профиле конкретного абонента при получении Push-сигнала.
   */
  fun selectUserByUid(uid: String) {
    val user = userList.value.find { it.uid == uid }
    if (user != null) {
      _selectedUser.value = user
      Log.i("[PUSH_SYNC] Абонента знайдено в ОЗУ: ${user.displayName}", tag = className)
    } else {
      Log.w("[PUSH_SYNC] Профіль $uid ще не завантажився в локальний кеш", tag = className)
    }
  }

  fun setPendingPushChatId(id: String?) {
    _pendingPushChatId.value = id
  }

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  /**
   * [confirmForwardToService] — Подтверждение пересылки сообщения в чат выбранной ЖКХ-службы.
   */
  fun confirmForwardToService(
    service: ContentDetail,
    baseState: BaseUIState,
    targetUser: UserEntity? = null
  ) {
    val (servicePrefix, systemId) = when (service) {
      ContentDetail.OSBB            -> "OSBB" to baseState.osmdId
      ContentDetail.WATER_SERVICE   -> "WATER_SERVICE" to Constants.WATER_SERVICE_ID
      ContentDetail.WARM_SERVICE    -> "WARM_SERVICE" to Constants.WARM_SERVICE_ID
      ContentDetail.GARBAGE_SERVICE -> "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
      else                          -> service.toString() to 0L
    }

    val chatId = if (baseState.userRole == UserRole.StandardUser) {
      if (baseState.addressId == 0L) {
        Log.w("ABORT. addressId є 0L (Квартира не обрана)", tag = className)
        return
      }
      "${servicePrefix}_${systemId}_${baseState.addressId}_${baseState.uid}"
    } else {
      val tUser = targetUser ?: _selectedUser.value
      if (tUser.uid.isBlank()) {
        Log.e("ERROR. Цільовий користувач (Target user) не визначений", tag = className)
        return
      }
      val targetAddrId = tUser.addressId
      if (targetAddrId == 0L) {
        Log.w("ABORT. Цільовий addressId є 0L", tag = className)
        return
      }
      val finalSysId = if (service == ContentDetail.OSBB) {
        tUser.osbbId ?: systemId
      } else {
        systemId
      }
      "${servicePrefix}_${finalSysId}_${targetAddrId}_${tUser.uid}"
    }

    Log.i("Сформовано цільовий індекс пересилання TARGET -> $chatId", tag = className)
    sendForwardedMessage(chatId)
  }

  /**
   * [cancelEditing] — Выход из режима редактирования сообщения с очисткой поля ввода.
   */
  fun cancelEditing() {
    Log.i("Редагування повідомлення скасовано.", tag = className)
    _editingMessage.value = null
    _messageText.value = ""
  }

  /**
   * [startEditing] — Активация режима редактирования сообщения и перенос его текста в поле ввода смартфона.
   */
  fun startEditing(message: MessageEntity) {
    Log.i("Ініціалізація редагування для повідомлення з ID: ${message.id}", tag = className)
    _editingMessage.value = message
    _messageText.value = message.text
  }

  /**
   * [onMessageTextChanged] — Реактивное отслеживание ввода текста и отправка сокет-сигналов "печать текста" оппоненту.
   * ПОЯСНЕНИЕ: Включает защиту от дребезга (debounce) со сбросом индикатора печати через 2500 мс тишины.
   */
  fun onMessageTextChanged(newText: String) {
    val oldText = _messageText.value
    _messageText.value = newText

    if (_editingMessage.value == null) {
      val now = currentTimeMillis()

      if (newText.isNotBlank()) {
        // Сигнализируем оппоненту о печати текста не чаще, чем раз в 4 секунды
        if (oldText.isBlank() || (now - lastTypingSentTime > 4000)) {
          lastTypingSentTime = now
          setTypingStatus(true)
        }

        typingStopJob?.cancel()
        typingStopJob = screenModelScope.launch {
          delay(2500)
          setTypingStatus(false)
          lastTypingSentTime = 0L
        }
      } else {
        typingStopJob?.cancel()
        if (oldText.isNotBlank()) {
          setTypingStatus(false)
          lastTypingSentTime = 0L
        }
      }
    }
  }


  /**
   * [updateMessage] — Отправка измененного текста сообщения в сеть распределенной базы данных.
   */
  fun updateMessage(newText: String) {
    val methodName = "updateMessage"
    val msg = _editingMessage.value ?: return
    val path = currentChatPath

    if (path.isNullOrBlank()) {
      println("[YkisLogKMP.$className.$methodName]: Помилка - currentChatPath порожній!")
      return
    }

    launchCatching(showLoader = true) {
      println("[YkisLogKMP.$className.$methodName]: [START] Редагування повідомлення з ID: ${msg.id}")

      val updates = mapOf(
        "text" to newText,
        "edited" to true
      )

      chatRepo.updateMessage(path, msg.id, updates)
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Повідомлення успішно відредаговано в хмарі.")

      cancelEditing()
      SnackbarManager.showMessage("Повідомлення змінено")
    }
  }

  /**
   * [subscribeToUnreadCount] — Массовое реактивное вычисление непрочитанных бейджей по комнатам квартир.
   * Исправлено: Вызовы логгера приведены к КМР-стандарту, убраны платформозависимые зависимости.
   */
  fun subscribeToUnreadCount(chatKeys: List<String>) {
    val methodName = "subscribeToUnreadCount"
    val myUid = chatRepo.currentUid ?: return

    println("[YkisLogKMP.$className.$methodName]: [START] Перевірка лічильників. Ключів: ${chatKeys.size}")

    chatKeys.forEach { chatId ->
      if (chatId.isBlank() || unreadCountListeners.containsKey(chatId)) return@forEach
      println("[YkisLogKMP.$className.$methodName]: Активація вотчера лічильника бейджів для кімнати: $chatId")

      val job = screenModelScope.launch {
        chatRepo.observeMessages(chatId)
          .map { messages ->
            messages.count { it.senderUid != myUid && !it.read }
          }
          .collect { count ->
            _unreadCounts.update { current ->
              val newMap = current + (chatId to count)
              println("[YkisLogKMP.$className.$methodName]: Кімната: $chatId -> Активних непрочитаних: $count")
              newMap
            }
          }
      }
      unreadCountListeners[chatId] = job
    }
  }

  /**
   * [onDispose] — Системный КМР-коллбек очистки ресурсов и закрытия сокетов Firebase при уничтожении экрана чата.
   * Исправлено: Потокобезопасная зачистка полностью страхует ОЗУ смартфона от утечек памяти!
   */
  override fun onDispose() {
    val methodName = "onDispose"
    println("[YkisLogKMP.$className.$methodName]: [START] Звільнення сокет-ресурсів та анулювання активних Jobs...")

    cleanupTracker()
    currentChatPath = null

    unreadCountListeners.values.forEach { it.cancel() }
    unreadCountListeners.clear()

    lastMessageListeners.values.forEach { it.cancel() }
    lastMessageListeners.clear()

    typingListeners.values.forEach { it.cancel() }
    typingListeners.clear()

    println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Усі Firebase-вотчери успішно відключені. Пам'ять чиста.")
    super.onDispose()
  }

  /**
   * [askAssistant] — Запрос контекстных подсказок и вариантов ответов у встроенного AI-ассистента Gemini.
   * Исправлено: Скрытый деструктивный узел _uiState вырезан, контекст собирается напрямую!
   */
  fun askAssistant(prompt: String, currentRole: UserRole, currentAddress: String) {
    val methodName = "askAssistant"
    if (prompt.isBlank()) return

    println("[YkisLogKMP.$className.$methodName]: [AI_PROMPT] Запит до Gemini: $prompt")

    launchCatching(showLoader = true) {
      val roleName = when (currentRole) {
        UserRole.VodokanalUser -> "диспетчера Водоканалу"
        UserRole.OsbbUser      -> "голови ОСББ"
        else                   -> "мешканця квартири"
      }

      val fullPrompt = """
            Ви — помічник у мобільному додатку сфери ЖКГ міста Южне. 
            Ви відповідаєте від імені $roleName. 
            Контекст квартири: $currentAddress.
            Запит користувача: $prompt
        """.trimIndent()

      val result = chatRepo.askAiAssistant(fullPrompt)
      result.onSuccess { response ->
        println("[YkisLogKMP.$className.$methodName]: [AI_SUCCESS] Відповідь успішно згенерована.")
        _assistantResponse.value = response
      }.onFailure { error ->
        println("[YkisLogKMP.$className.$methodName]: Помилка нейромережі: ${error.message}")
        _assistantResponse.value = "Помилка ШІ: ${error.message}"
        logService.logNonFatalCrash(error)
      }
    }
  }

  /**
   * [subscribeToLastMessages] — Подписка на сокет последнего сообщения комнаты чата для вывода превью строки.
   */
  fun subscribeToLastMessages(chatKeys: List<String>) {
    val methodName = "subscribeToLastMessages"
    chatKeys.forEach { chatId ->
      if (chatId.isBlank() || lastMessageListeners.containsKey(chatId)) return@forEach
      println("[YkisLogKMP.$className.$methodName]: WATCH_LAST_MSG -> $chatId")

      val job = screenModelScope.launch {
        chatRepo.observeLastMessage(chatId).collect { message ->
          if (message != null) {
            _lastMessages.update { it + (chatId to message) }
            println("[YkisLogKMP.$className.$methodName]: UPDATE_PREVIEW -> $chatId: ${message.text.take(20)}")
          }
        }
      }
      lastMessageListeners[chatId] = job
    }
  }

  /**
   * [markMessagesAsRead] — Атомарный сброс счётчика непрочитанных сообщений в Firebase при прочтении чата.
   */
  fun markMessagesAsRead(chatId: String) {
    val methodName = "markMessagesAsRead"
    val myUid = chatRepo.currentUid ?: return
    if (chatId.isBlank()) return

    // ИСПРАВЛЕНО: Помечаем прочитанным только если этот чат реально открыт у админа перед глазами
    if (chatId != currentChatPath) {
      println("[YkisLogKMP.$className.$methodName]: [SKIP] Чат $chatId находится в фоне, прочтение отклонено.")
      return
    }

    launchCatching {
      println("[YkisLogKMP.$className.$methodName]: [DB_START] Оновлення статусу прочитання -> $chatId")
      withContext(NonCancellable) {
        chatRepo.markMessagesAsRead(chatId, myUid)
        _unreadCounts.update { it + (chatId to 0) }
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Усі повідомлення кімнати позначені як прочитані.")
      }
    }
  }

  fun showDeleteConfirmation(message: MessageEntity) {
    println("[YkisLogKMP.$className.showDeleteConfirmation]: Підготовка видалення повідомлення з ID: ${message.id}")
    _messageToDelete.value = message
  }

  fun dismissDeleteDialog() {
    _messageToDelete.value = null
  }

  /**
   * [confirmDeletion] — Безвозвратное удаление сообщения из Firebase-базы и очистка привязанных медиафайлов из Storage.
   */
  fun confirmDeletion() {
    val methodName = "confirmDeletion"
    val message = _messageToDelete.value
    val path = currentChatPath

    if (path.isNullOrBlank() || message == null) {
      println("[YkisLogKMP.$className.$methodName]: Помилка видалення — дані відсутні!")
      return
    }

    launchCatching(showLoader = true) {
      println("[YkisLogKMP.$className.$methodName]: [START_DELETE] Видалення повідомлення з ID: ${message.id}")

      message.imageUrl?.let {
        println("[YkisLogKMP.$className.$methodName]: Очищення зображення зі Storage...")
        chatRepo.deleteFileFromStorage(it)
      }

      message.fileUrl?.let {
        println("[YkisLogKMP.$className.$methodName]: Очищення документа зі Storage...")
        chatRepo.deleteFileFromStorage(it)
      }

      chatRepo.removeMessage(path, message.id)
      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Повідомлення успішно видалено з хмари.")

      _messageToDelete.value = null
      SnackbarManager.showMessage("Повідомлення видалено")
    }
  }



  /**
   * [analyzePhotoWithGemini] — Интеграция Gemini ИИ для автоматического распознавания
   * показаний и серийных номеров приборов учета Водоканала по фотографии.
   * ПОЯСНЕНИЕ: Метод считывает бинарные данные, отправляет запрос через Ktor-клиент
   * и автоматически подставляет распознанный украинский текст в буфер ввода.
   */
  fun analyzePhotoWithGemini(imagePath: String, address: String) {
    val methodName = "analyzePhotoWithGemini"
    println("[YkisLogKMP.$className.$methodName]: [START] Аналіз фото лічильника для адреси: $address")

    launchCatching(showLoader = true) {
      try {
        val imageData: ByteArray = chatRepo.readFileAsBytes(imagePath)
        if (imageData.isEmpty()) {
          println("[YkisLogKMP.$className.$methodName]: [ABORT] Бінарні дані зображення порожні")
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

        println("[YkisLogKMP.$className.$methodName]: Відправка запиту комп'ютерного зору до Gemini через Ktor...")
        val response = chatRepo.analyzeMeterImage(prompt, imageData)

        if (!response.isNullOrBlank()) {
          println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Дані розпізнані")
          _assistantResponse.value = response
          if (_messageText.value.isBlank()) {
            _messageText.value = response
          }
        } else {
          println("[YkisLogKMP.$className.$methodName]: [EMPTY] ШІ повернув порожній результат")
          SnackbarManager.showMessage("ШІ не зміг розпізнати дані")
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [ERROR] Сбій обробки фото: ${e.message}")
        SnackbarManager.showMessage("Помилка розпізнавання: перевірте підключення")
        logService.logNonFatalCrash(e)
      }
    }
  }

  fun applyAiHint() {
    val textToApply = assistantResponse.value ?: quickHint.value
    if (!textToApply.isNullOrBlank()) {
      println("[YkisLogKMP.$className.applyAiHint]: Текст підказки ШІ перенесено в поле введення")
      _messageText.value = textToApply
      clearAiSuggestion()
    }
  }

  fun clearAiSuggestion() {
    _assistantResponse.value = null
    _quickHint.value = null
  }

  /**
   * [writeToDatabase] — Физическая отправка сформированного КМР-пакета сообщения в облако Firebase.
   * ПОЯСНЕНИЕ: Все числовые идентификаторы переведены на Long для строгого маппинга лицевых счетов ГИОЦ Южного.
   */
  fun writeToDatabase(
    chatUid: String,
    senderUid: String,
    senderDisplayedName: String,
    senderLogoUrl: String?,
    senderAddress: String,
    addressId: Long, // Изменено на Long
    imageUrl: String?,
    fileUrl: String? = null,
    fileName: String?,
    osbbId: Long, // Изменено на Long
    role: UserRole,
    onComplete: () -> Unit,
    recipientTokens: List<String>
  ) {
    val methodName = "writeToDatabase"
    if (_messageText.value.isBlank() && imageUrl == null && fileUrl == null) {
      println("[YkisLogKMP.$className.$methodName]: [CANCEL] Спроба відправки порожнього повідомлення заблокована")
      return
    }

    launchCatching(showLoader = false) {
      val finalTargetUid = if (role != UserRole.StandardUser) {
        chatUid.ifBlank { _selectedUser.value.uid.ifBlank { "" } }
      } else {
        null
      }

      val effectiveOsbbId = when (role) {
        UserRole.VodokanalUser -> Constants.WATER_SERVICE_ID
        UserRole.YtkeUser      -> Constants.WARM_SERVICE_ID
        UserRole.TboUser       -> Constants.GARBAGE_SERVICE_ID
        else                   -> osbbId
      }

      val chatId = getChatPath(
        role = role,
        osbbId = effectiveOsbbId,
        addressId = addressId,
        targetUserUid = finalTargetUid
      )
      currentChatPath = chatId
      println("[YkisLogKMP.$className.$methodName]: [START] Запис повідомлення. Шлях у базі даних: $chatId")

      val finalDisplayName = if (role != UserRole.StandardUser) {
        try {
          when (role) {
            UserRole.VodokanalUser -> "Водоканал Южне"
            UserRole.YtkeUser      -> "ЮТКЕ"
            UserRole.TboUser       -> "Южтранс"
            UserRole.OsbbUser      -> "Адміністратор ОСББ"
            else                   -> "Адміністратор"
          }
        } catch (e: Exception) {
          "Адміністратор"
        }
      } else {
        senderDisplayedName.substringAfter("|").trim()
      }

      val displayText = if (fileUrl != null && _messageText.value.isBlank()) "[Файл]" else _messageText.value

      val messageEntity = MessageEntity(
        id = "",
        senderUid = senderUid,
        text = displayText,
        type = when {
          imageUrl != null -> "IMAGE"
          fileUrl != null  -> "FILE"
          else             -> "TEXT"
        },
        senderLogoUrl = senderLogoUrl,
        senderDisplayedName = finalDisplayName,
        senderAddress = senderAddress,
        imageUrl = imageUrl,
        fileUrl = fileUrl,
        fileName = fileName,
        timestamp = currentTimeMillis(),
        read = false
      )

      _isLoadingAfterSending.value = true
      val result = chatRepo.sendMessage(chatId, messageEntity)
      _isLoadingAfterSending.value = false

      if (result.isSuccess) {
        println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Транзакція повідомлення успішно запечатана в базі")
        
        // ОТПРАВКА PUSH-УВЕДОМЛЕНИЯ ЧЕРЕЗ CLOUD FUNCTIONS
        val tokens = _recipientTokens.value
        if (tokens.isNotEmpty()) {
          println("[YkisLogKMP.$className.$methodName]: [PUSH] Отправка уведомления на ${tokens.size} устройств. Токены: ${tokens.map { it.take(5) }}")
          val notificationData = mapOf(
            "tokens" to tokens,
            "title" to finalDisplayName,
            "body" to displayText,
            "chatId" to chatId
          )
          screenModelScope.launch {
            try {
              chatRepo.sendChatNotification(notificationData)
            } catch (e: Exception) {
              println("[YkisLogKMP.$className.PUSH_ERR]: Сбой вызова функции уведомлений: ${e.message}")
            }
          }
        } else {
          println("[YkisLogKMP.$className.$methodName]: [PUSH_SKIP] Список токенов пуст. Уведомление не отправлено.")
        }

        _messageText.value = ""
        _selectedImagePath.value = null
        clearAiSuggestion()
        onComplete()
      } else {
        println("[YkisLogKMP.$className.$methodName]: Помилка відправки в хмару: ${result.exceptionOrNull()?.message}")
        SnackbarManager.showMessage("Помилка відправки")
      }
    }
  }






  /**
   * [setPresence] — Установка флага присутствия абонента внутри конкретной комнаты чата.
   */
  fun setPresence(chatId: String, isOnline: Boolean) {
    val methodName = "setPresence"
    val myUid = chatRepo.currentUid ?: return
    if (chatId.isBlank()) return

    // Переводим выполнение в фоновый пул, изолируя главный поток интерфейса от дисковых сбоев
    screenModelScope.launch(Dispatchers.Default) {
      runCatching {
        if (isOnline) {
          println("[YkisLogKMP.$className.$methodName]: Presence ON -> Кімната: $chatId, UID: $myUid")
          chatRepo.setUserOnline(chatId, myUid)
        } else {
          println("[YkisLogKMP.$className.$methodName]: Presence OFF -> Кімната: $chatId, UID: $myUid")
          chatRepo.setUserOffline(chatId, myUid)
        }
      }.onFailure { e ->
        // Ловим отказы в доступе (Permission Denied) правил Firebase Realtime Database без падения приложения
        println("[YkisLogKMP.$className.${methodName}_WARN]: Заблоковано правилами безпеки Firebase (Permission Denied): ${e.message}")
      }
    }
  }

  /**
   * [setTypingStatus] — Фиксация активного статуса набора сообщения текущим пользователем.
   */
  fun setTypingStatus(isTyping: Boolean) {
    val methodName = "setTypingStatus"
    val myUid = chatRepo.currentUid ?: return
    val path = currentChatPath ?: return
    if (path.isBlank()) return

    screenModelScope.launch {
      runCatching {
        chatRepo.setTypingStatus(chatId = path, uid = myUid, isTyping = isTyping)
        println("[YkisLogKMP.$className.$methodName]: Статус печати ${if(isTyping) "ВКЛ" else "ВЫКЛ"} для ветки: $path")
      }.onFailure { e ->
        println("[YkisLogKMP.$className.${methodName}_WARN]: Ошибка обновления статуса печати: ${e.message}")
      }
    }
  }
  /**
   * [observeTypingStatus] — Фоновый сокет-слушатель Firebase для отслеживания индикатора ввода текста.
   */
  private fun observeTypingStatus(chatId: String) {
    val methodName = "observeTypingStatus"
    val myUid = chatRepo.currentUid ?: return

    // ИСПРАВЛЕНО: Если мы уже слушаем статус этой комнаты и поток активен — не перезапускаем!
    // Это предотвращает SIGILL (Fatal signal 4) из-за перегрузки нативных слушателей Firebase.
    if (typingListeners[chatId]?.isActive == true) return

    println("[YkisLogKMP.$className.$methodName]: WATCH_TYPING_START -> Запуск вотчера для комнаты: $chatId")

    val job = screenModelScope.launch {
      chatRepo.observeTyping(chatId)
        .catch { error ->
          println("[YkisLogKMP.$className.${methodName}_WARN]: Firebase заблокировал подписку на статус печати: ${error.message}")
        }
        .collect { typingMap ->
          val someoneIsTyping = typingMap.filterKeys { it != myUid }.values.any { it == true }

          // Обновляем статус для активной комнаты
          if (chatId == currentChatPath) {
             _isOpponentTyping.value = someoneIsTyping
          }
          
          // Обновляем в глобальной мапе для отображения в общем списке чатов
          _globalTypingStatuses.update { it + (chatId to someoneIsTyping) }
        }
    }
    typingListeners[chatId] = job
  }


  /**
   * [readFromDatabase] — Инициализация, подписка и чтение потока сообщений комнаты чата из Firebase СУБД.
   * Исправлено: Параметры osbbId и addressId переведены на сквозной Long-стандарт для полной совместимости!
   */
  fun readFromDatabase(role: UserRole, senderUid: String, osbbId: Long, addressId: Long) {
    val methodName = "readFromDatabase"
    
    val effectiveOsbbId = when (role) {
      UserRole.VodokanalUser -> Constants.WATER_SERVICE_ID
      UserRole.YtkeUser      -> Constants.WARM_SERVICE_ID
      UserRole.TboUser       -> Constants.GARBAGE_SERVICE_ID
      else                   -> osbbId
    }

    val targetPath = getChatPath(
      role = role,
      osbbId = effectiveOsbbId,
      addressId = addressId,
      targetUserUid = if (role != UserRole.StandardUser) senderUid else null
    )

    // ИСПРАВЛЕНО: Если мы уже слушаем этот путь — не перезапускаем подписку!
    if (currentChatPath == targetPath && messageSubscriptionJob?.isActive == true) {
      println("[YkisLogKMP.$className.$methodName]: [SKIP] Подписка на $targetPath уже активна.")
      return
    }

    println("[YkisLogKMP.$className.$methodName]: EXTERNAL_CALL. Активация чата для о/р: $addressId, Роль: $role")

    messageSubscriptionJob?.cancel()
    messageSubscriptionJob = screenModelScope.launch {
      try {
        var activeUid = chatRepo.currentUid
        var attempts = 0
        while (activeUid == null && attempts < 20) {
          attempts++
          delay(200)
          activeUid = chatRepo.currentUid
        }
        
        if (activeUid == null) {
          println("[YkisLogKMP.$className.$methodName]: [ABORT] UID не найден.")
          return@launch
        }

        println("[YkisLogKMP.$className.$methodName]: [INIT] Подписка на Firebase поток сокетов: $targetPath")
        currentChatPath = targetPath
        
        // Очищаем только если сменили чат
        if (currentChatPath != targetPath) _firebaseTest.value = emptyList()

        // ИСПРАВЛЕНО: Пресенс и статус печати запускаем ОДИН РАЗ при открытии, а не в коллбеке сообщений
        setPresence(targetPath, true)
        observeTypingStatus(targetPath)

        // Подгружаем токены получателей (админов) для жителя
        if (role == UserRole.StandardUser) {
           screenModelScope.launch {
              val admins = chatRepo.fetchAdminsByOsbb(effectiveOsbbId)
              _recipientTokens.value = admins.flatMap { it.tokens }.distinct()
              println("[YkisLogKMP.$className.$methodName]: Найдено ${admins.size} админов, токенов: ${_recipientTokens.value.size}")
           }
        }

        chatRepo.observeMessages(targetPath)
          .map { messages ->
            messages.filter { msg ->
              val deletedList = msg.deletedFor ?: emptyList()
              !deletedList.contains(activeUid)
            }.sortedBy { it.timestamp }
          }
          .collect { filteredMessages ->
            println("[YkisLogKMP.$className.$methodName]: [DATA_RECEIVED] Сообщений в ОЗУ: ${filteredMessages.size}")
            _firebaseTest.value = filteredMessages
            
            // ИСПРАВЛЕНО: Отмечаем прочитанным только если есть новые сообщения от собеседника. 
            // Это ликвидирует бесконечный цикл в логах.
            val hasUnreadFromOpponent = filteredMessages.any { !it.read && it.senderUid != activeUid }
            if (hasUnreadFromOpponent) {
              markMessagesAsRead(targetPath)
            }
          }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className.$methodName]: [CRITICAL_ERROR] Сбой подписки: ${e.message}")
        logService.logNonFatalCrash(e)
      }
    }
  }

  /**
   * [clearCurrentChatPath] — Сброс контекста активного чата при выходе из комнаты.
   */
  fun clearCurrentChatPath() {
    val methodName = "clearCurrentChatPath"
    val chatId = currentChatPath
    if (chatId == null) {
      println("[YkisLogKMP.$className.$methodName]: [SKIP] Контекст чату вже порожній.")
      return
    }
    println("[YkisLogKMP.$className.$methodName]: [START] Зачистка активной комнаты: $chatId")
    
    // ИСПРАВЛЕНО: Физически убиваем фоновое прослушивание сообщений при выходе из комнаты
    messageSubscriptionJob?.cancel()
    messageSubscriptionJob = null

    currentChatPath = null
    _isOpponentTyping.value = false
    setTypingStatus(false)
    setPresence(chatId, false)
    println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Контекст кімнати успішно деактивовано.")
  }

  /**
   * [trackUserIdentifiersWithRole] — Диспетчерский сокет-трекер ключей комнат чатов для администраторов.
   * Исправлено: Очистка листенеров перед обновлением предотвращает утечки ОЗУ, типы приведены к Long.
   */
  fun trackUserIdentifiersWithRole(
    role: UserRole, 
    osbbId: Long?, 
    apartments: List<ApartmentEntity> = emptyList()
  ) {
    val methodName = "trackUserIdentifiers"
    val myUid = chatRepo.currentUid ?: ""
    val safeOsbbId = osbbId ?: 0L
    
    println("[YkisLogKMP.$className.$methodName]: ENTRY. Роль: $role | Квартир: ${apartments.size}")

    cleanupTracker()

    if (role == UserRole.StandardUser) {
      // ЛОГИКА ДЛЯ ЖИТЕЛЯ: Подписываемся на все ветки всех своих квартир
      val residentKeys = mutableListOf<String>()
      apartments.forEach { apt ->
        residentKeys.add("OSBB_${apt.osmdId ?: 0L}_${apt.addressId}_$myUid")
        residentKeys.add("WATER_SERVICE_${Constants.WATER_SERVICE_ID}_${apt.addressId}_$myUid")
        residentKeys.add("WARM_SERVICE_${Constants.WARM_SERVICE_ID}_${apt.addressId}_$myUid")
        residentKeys.add("GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_${apt.addressId}_$myUid")
      }
      
      if (residentKeys.isNotEmpty()) {
        println("[YkisLogKMP.$className.$methodName]: Житель — запуск мониторинга ${residentKeys.size} веток.")
        _userIdentifiersWithRole.value = residentKeys
        subscribeToUnreadCount(residentKeys)
        subscribeToLastMessages(residentKeys)
        
        // ИСПРАВЛЕНО: Теперь жители тоже видят, когда админ им пишет в списке чатов
        residentKeys.forEach { key ->
           if (!typingListeners.containsKey(key)) {
               observeTypingStatus(key)
           }
        }
      }
      return
    }

    // ЛОГИКА ДЛЯ АДМИНА (Остается прежней — через префикс-вотчер)
    val targetPrefix = when (role) {
      UserRole.VodokanalUser -> "WATER_SERVICE_${Constants.WATER_SERVICE_ID}_"
      UserRole.YtkeUser      -> "WARM_SERVICE_${Constants.WARM_SERVICE_ID}_"
      UserRole.TboUser       -> "GARBAGE_SERVICE_${Constants.GARBAGE_SERVICE_ID}_"
      UserRole.OsbbUser      -> "OSBB_${safeOsbbId}_"
      else                   -> "UNKNOWN_"
    }

    println("[YkisLogKMP.$className.$methodName]: [CONFIG] Розрахований префікс для Firebase: '$targetPrefix'")

    cleanupTracker()
    activeTrackerJob = screenModelScope.launch {
      println("[YkisLogKMP.$className.$methodName]: [ACTIVE] Watcher запущено для префіксу кімнат: '$targetPrefix'")
      chatRepo.observeChatKeys(targetPrefix)
        .collect { chatKeys ->
          println("[YkisLogKMP.$className.$methodName]: [DATA] Знайдено активних ключів кімнат: ${chatKeys.size} шт.")
          val currentKeys = _userIdentifiersWithRole.value
          val keysIdentical = chatKeys.sorted() == currentKeys.sorted()
          val uiPopulated = userList.value.isNotEmpty()
          _userIdentifiersWithRole.value = chatKeys

          if (chatKeys.isNotEmpty()) {
            lastMessageListeners.values.forEach { it.cancel() }
            lastMessageListeners.clear()
            unreadCountListeners.values.forEach { it.cancel() }
            unreadCountListeners.clear()

            subscribeToUnreadCount(chatKeys)
            subscribeToLastMessages(chatKeys)
            
            // ИСПРАВЛЕНО: Также подписываемся на статусы печати для всех чатов в списке
            chatKeys.forEach { key ->
               if (!typingListeners.containsKey(key)) {
                   observeTypingStatus(key)
               }
            }

            if (!keysIdentical || !uiPopulated) {
              getUsers()
            }
          } else {
            println("[YkisLogKMP.$className.$methodName]: [EMPTY] Жодної кімнати чату не знайдено.")
            _rawFetchedProfiles.value = emptyList()
            _unreadCounts.value = emptyMap()
          }
          updateSystemIconBadge()
        }
    }
  }



  /**
   * [updateSystemIconBadge] — Обновление глобального бейджа непрочитанных уведомлений на иконке приложения.
   * ПОЯСНЕНИЕ: Считает сумму всех счетчиков комнат и пробрасывает число через expect/actual утилиту.
   */
  private fun updateSystemIconBadge() {
    val methodName = "updateSystemIconBadge"
    try {
      val totalCount = _unreadCounts.value.values.sum()
      println("[YkisLogKMP.$className.$methodName]: Розрахунок загального бейджа додатка -> $totalCount")
      applyAppBadgeCount(totalCount)
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.$methodName]: Помилка оновлення бейджа іконки: ${e.message}")
    }
  }

  private fun cleanupTracker() {
    activeTrackerJob?.cancel()
    activeTrackerJob = null
  }

  /**
   * [getUsers] — Массовая пакетная загрузка профилей абонентов из Firebase по списку UIDs.
   */
  fun getUsers() {
    val methodName = "getUsers"
    val chatKeys = _userIdentifiersWithRole.value

    if (chatKeys.isEmpty()) {
      println("[YkisLogKMP.$className.$methodName]: [CANCEL] Список ключів порожній.")
      return
    }

    launchCatching(snackbar = false) {
      println("[YkisLogKMP.$className.$methodName]: [START] Парсинг UIDs для завантаження анкет...")
      val uidsToFetch = chatKeys.map { it.substringAfterLast("_") }
        .filter { it.isNotEmpty() }
        .distinct()

      println("[YkisLogKMP.$className.$methodName]: Запит профілів з бази даних для UIDs: $uidsToFetch")
      val fetchedProfiles = chatRepo.fetchUsersByIds(uidsToFetch)
      _rawFetchedProfiles.value = fetchedProfiles

      println("[YkisLogKMP.$className.$methodName]: [SUCCESS] Завантажено анкет абонентів: ${fetchedProfiles.size} шт.")
    }
  }

  /**
   * [setSelectedService] — Перегрузка установки активной коммунальной службы из UI карточек списка.
   */
  fun setSelectedService(totalServiceDebt: TotalServiceDebt?) {
    val methodName = "setSelectedService"

    if (totalServiceDebt == null) {
      _selectedService.value = null
      _selectedServicePrefix.value = null
      println("[YkisLogKMP.$className.$methodName]: Скинуто фільтрацію поточної служби чату.")
      return
    }

    _selectedService.value = totalServiceDebt
    val serviceCode = totalServiceDebt.contentDetail.name
    _selectedServicePrefix.value = serviceCode

    println("[YkisLogKMP.$className.$methodName]: SELECT -> ${totalServiceDebt.name} ($serviceCode)")
  }

  fun setSelectedImagePath(path: String?) {
    _selectedImagePath.value = path
    println("[YkisLogKMP.$className.setSelectedImagePath]: Шлях до медіа-файлу оновлено в ОЗУ: $path")
  }

  fun setSelectedMessage(message: MessageEntity) {
    _selectedMessage.value = message
  }

  /**
   * [getChatPath] — Архитектурный КМР-генератор путей к веткам Firebase на базе роли, ID ОСББ и лицевого счета квартиры.
   * Исправлено: Параметры osbbId и addressId переведены на сквозной Long-стандарт, убирая урезание данных!
   */
  fun getChatPath(
    role: UserRole,
    osbbId: Long, // Изменено на Long
    addressId: Long, // Изменено на Long
    targetUserUid: String?
  ): String {
    val methodName = "getChatPath"
    val myUid = chatRepo.currentUid ?: ""
    val residentUid = targetUserUid ?: myUid
    val serviceInState = _selectedServicePrefix.value

    println("[YkisLogKMP.$className.$methodName]: [INPUT_PARAMS] Роль: $role | OSBB_ID: $osbbId | Лицьовий рахунок: $addressId | Target: $targetUserUid")

    val (prefix, sysId) = when {
      role == UserRole.VodokanalUser || serviceInState == "WATER_SERVICE" -> {
        println("[YkisLogKMP.$className.$methodName]: [MATCH] Обнаружена ветка WATER_SERVICE")
        "WATER_SERVICE" to Constants.WATER_SERVICE_ID
      }
      role == UserRole.YtkeUser || serviceInState == "WARM_SERVICE" -> {
        println("[YkisLogKMP.$className.$methodName]: [MATCH] Обнаружена ветка WARM_SERVICE")
        "WARM_SERVICE" to Constants.WARM_SERVICE_ID
      }
      role == UserRole.TboUser || serviceInState == "GARBAGE_SERVICE" -> {
        println("[YkisLogKMP.$className.$methodName]: [MATCH] Обнаружена ветка GARBAGE_SERVICE")
        "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
      }
      else -> {
        println("[YkisLogKMP.$className.$methodName]: [MATCH] За замовчуванням підключаємо лінію OSBB")
        "OSBB" to osbbId
      }
    }

    val path = "${prefix}_${sysId}_${addressId}_$residentUid"

    if (addressId == sysId) {
      println("[YkisLogKMP.$className.$methodName]: [CRITICAL_WARNING] Увага! addressId збігається з системним кодом служби ($sysId)!")
    }

    println("[YkisLogKMP.$className.$methodName]: [RESULT_PATH] Сформовано шлях чату: $path")
    return path
  }

  /**
   * [openChatWithUser] — Точка входа для диспетчеров и администраторов для открытия чата с конкретным жильцом.
   * Исправлено: Скрытые касты toInt() удалены, вызовы методов синхронизированы на Long параметры.
   */
  fun openChatWithUser(
    user: UserEntity,
    currentRole: UserRole,
    currentOsbbId: Long // Изменено на Long
  ) {
    val methodName = "openChatWithUser"
    val realAddressId = user.addressId

    println("--- [YkisLogKMP.$className.$methodName] ---")
    println("[YkisLogKMP.$className.$methodName]: Відкриття кімнати. Ціль: ${user.displayName} | Роль диспетчера: $currentRole")
    _selectedUser.value = user

    val effectiveOsbbId = when (currentRole) {
      UserRole.VodokanalUser -> 9999L
      UserRole.YtkeUser      -> 9998L
      UserRole.TboUser       -> 9997L
      else                   -> currentOsbbId
    }

    val chatId = getChatPath(
      role = currentRole,
      osbbId = effectiveOsbbId,
      addressId = realAddressId, // Передаем чистый Long
      targetUserUid = user.uid
    )

    _unreadCounts.update { it + (chatId to 0) }
    markMessagesAsRead(chatId)
    observeTypingStatus(chatId)
    
    // Подгружаем токены получателя (жильца)
    _recipientTokens.value = user.tokens

    readFromDatabase(
      role = currentRole,
      senderUid = user.uid,
      osbbId = effectiveOsbbId,
      addressId = realAddressId // Передаем чистый Long
    )
  }
}

