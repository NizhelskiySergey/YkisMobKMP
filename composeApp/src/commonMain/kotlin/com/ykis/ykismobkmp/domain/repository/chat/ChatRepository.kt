package com.ykis.ykismobkmp.domain.repository.chat


import com.ykis.ykismobkmp.core.utils.currentTimeMillis
import com.ykis.ykismobkmp.core.utils.wrapForFirebase
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.entity.*
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.domain.services.toEntity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import dev.gitlive.firebase.database.ChildEvent
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * [ChatRepository] — єдине джерело даних для чатів та ШІ.
 */
class ChatRepository(
  private val _firestore: FirebaseFirestore?,
  private val _realtime: FirebaseDatabase?,
  private val _storage: FirebaseStorage?,
  private val aiManager: GeminiAiManager
) {
  private val className = "ChatRepository"

  private val firestore get() = _firestore ?: throw IllegalStateException("Firestore not available")
  val realtime get() = _realtime ?: throw IllegalStateException("Realtime Database not available")
  val storage get() = _storage ?: throw IllegalStateException("Storage not available")

  val currentUid: String?
    get() = try { Firebase.auth.currentUser?.uid } catch (e: Exception) { null }

  // Вспомогательная функция для конвертации Long в Double для Web
  private fun safeNum(num: Long): Any {
    return if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
        num.toDouble()
    } else {
        num
    }
  }

  suspend fun fetchUsersByIds(ids: List<String>): List<UserEntity> = coroutineScope {
    if (ids.isEmpty() || _firestore == null) return@coroutineScope emptyList()
    
    val distinctIds = ids.distinct().filter { it.isNotBlank() }.take(20)
    
    try {
      val deferreds = distinctIds.map { uid ->
        async {
          try {
            val doc = firestore.collection("users").document(uid).get()
            if (doc.exists) {
                // Використовуємо UserFirebase для безпечного парсингу типів даних
                doc.data<com.ykis.ykismobkmp.domain.services.UserFirebase>().toEntity().copy(uid = doc.id)
            } else null
          } catch (e: Exception) {
            null
          }
        }
      }
      deferreds.awaitAll().filterNotNull()
    } catch (e: Exception) {
      emptyList()
    }
  }
  
  fun observeTyping(chatId: String): Flow<Map<String, Boolean>> {
    if (_realtime == null) return flow { emit(emptyMap()) }
    return realtime.reference("presence/$chatId")
      .valueEvents
      .map { snapshot ->
        snapshot.children.associate { child ->
          val uid = child.key ?: ""
          val isTyping = try {
             val typingChild = child.child("typing")
             if (typingChild.exists) typingChild.value<Boolean?>() ?: false else false
          } catch (e: Exception) { false }
          uid to isTyping
        }
      }
  }

  suspend fun setTypingStatus(chatId: String, uid: String, isTyping: Boolean) {
    if (_realtime == null) return
    try {
      val updates = mapOf("typing" to isTyping)
      realtime.reference("presence/$chatId/$uid").updateChildren(updates)
    } catch (e: Exception) { }
  }

  fun observeChatKeys(prefix: String): Flow<List<String>> {
    if (_realtime == null) return flowOf(emptyList())
    
    return realtime.reference("chat_access")
      .valueEvents
      .map { snapshot ->
        val keys = snapshot.children
          .mapNotNull { it.key }
          .filter { it.startsWith(prefix) }
          .sortedDescending()
        println("[YkisLogKMP.ChatRepository]: observeChatKeys для '$prefix' повернув ${keys.size} ключів")
        keys
      }
      .catch { e -> 
        println("[YkisLogKMP.ChatRepository_ERROR]: Помилка observeChatKeys для '$prefix': ${e.message}")
        // Не емітимо пустий список, щоб не затирати стан при помилці зв'язку або AppCheck
      }
  }

  suspend fun fetchAdminsByOsbb(osbbId: Long): List<UserEntity> {
    if (_firestore == null) return emptyList()
    return try {
      // Виконуємо пошук за всіма можливими типами даних (Firestore дуже строгий до типів)
      val q1 = firestore.collection("users").where { "osbbId" equalTo osbbId }.get()            // Long
      val q2 = firestore.collection("users").where { "osbbId" equalTo osbbId.toDouble() }.get() // Double (Web)
      val q3 = firestore.collection("users").where { "osbbId" equalTo osbbId.toString() }.get() // String
      
      val combined = (q1.documents + q2.documents + q3.documents).distinctBy { it.id }
      
      combined.mapNotNull { doc ->
        try { 
            doc.data<com.ykis.ykismobkmp.domain.services.UserFirebase>().toEntity().copy(uid = doc.id) 
        } catch (e: Exception) { 
            println("[ChatRepository]: Помилка парсингу адміна ${doc.id}: ${e.message}")
            null 
        }
      }.filter { it.userRole != UserRole.StandardUser && it.userRole != UserRole.Unknown }
    } catch (e: Exception) { 
      println("[ChatRepository.fetchAdmins_ERROR]: ${e.message}")
      emptyList() 
    }
  }

  suspend fun fetchUserByAddressId(addressId: Long): UserEntity? {
    if (_firestore == null) return null
    return try {
      val targetId = safeNum(addressId)
      val resultNum = firestore.collection("users").where { "addressId" equalTo targetId }.get()
      val resultStr = firestore.collection("users").where { "addressId" equalTo addressId.toString() }.get()
      val doc = (resultNum.documents + resultStr.documents).firstOrNull()
      doc?.let { 
          it.data<com.ykis.ykismobkmp.domain.services.UserFirebase>().toEntity().copy(uid = it.id) 
      }
    } catch (e: Exception) { null }
  }

  fun observeUnreadCounts(myUid: String): Flow<Map<String, Int>> {
    if (_realtime == null) return flow { emit(emptyMap()) }
    return realtime.reference("unread_counters/$myUid")
      .valueEvents
      .map { snapshot ->
        snapshot.children.associate { 
            val count = try {
                it.value<Double?>()?.toInt() ?: 0
            } catch (e: Exception) {
                try { it.value<Int?>() ?: 0 } catch (e2: Exception) { 0 }
            }
            it.key!! to count 
        }
      }
  }

  suspend fun resetUnreadCount(chatId: String, myUid: String) {
    if (_realtime == null) return
    try {
      realtime.reference("unread_counters/$myUid/$chatId").setValue(0)
    } catch (e: Exception) { }
  }

  suspend fun incrementUnreadForUids(chatId: String, uids: List<String>) {
    if (_realtime == null || uids.isEmpty()) return
    
    try {
      val distinctUids = uids.distinct()
      
      distinctUids.forEach { uid ->
        val presenceRef = realtime.reference("presence/$chatId/$uid/online")
        val isOnline = try {
            val snap = presenceRef.valueEvents.first()
            if (snap.exists) snap.value<Boolean?>() ?: false else false
        } catch (e: Exception) { false }
        
        if (!isOnline) {
            val unreadRef = realtime.reference("unread_counters/$uid/$chatId")
            unreadRef.setValue(dev.gitlive.firebase.database.ServerValue.increment(1.0))
        }
      }
    } catch (e: Exception) {
      println("[YkisLogKMP.ChatCounter_ERROR]: ${e.message}")
    }
  }

  suspend fun incrementUnreadForParticipants(chatId: String, senderUid: String) {
    if (_realtime == null) return
    try {
      val participantsSnapshot = realtime.reference("chat_access/$chatId").valueEvents.first()
      val recipientUids = participantsSnapshot.children
        .mapNotNull { it.key }
        .filter { it != senderUid }
        .distinct()
      
      if (recipientUids.isNotEmpty()) {
          incrementUnreadForUids(chatId, recipientUids)
      }
    } catch (e: Exception) {
      println("[YkisLogKMP.ChatCounter_ERROR]: ${e.message}")
    }
  }

  suspend fun fetchAllUsersByAddressId(addressId: Long): List<UserEntity> {
    if (_firestore == null) return emptyList()
    return try {
      val targetId = safeNum(addressId)
      val resultNum = firestore.collection("users").where { "addressId" equalTo targetId }.get()
      val resultStr = firestore.collection("users").where { "addressId" equalTo addressId.toString() }.get()
      val combined = (resultNum.documents + resultStr.documents).distinctBy { it.id }
      combined.mapNotNull { doc -> 
          try {
              doc.data<com.ykis.ykismobkmp.domain.services.UserFirebase>().toEntity().copy(uid = doc.id)
          } catch (e: Exception) { null }
      }
    } catch (e: Exception) { emptyList() }
  }

  fun sendGlobalNotification(title: String, body: String, osbbId: Long = 0L, imageUrl: String? = null) {
    // ПОРОЖНЬО: Тепер пуші розсилаються автоматично через Cloud Functions (Firestore Trigger)
    // як і в чаті. Це усуває проблеми з CORS та правами доступу в браузері.
    println("[ChatRepository]: Оголошення '$title' збережено. Очікування автоматичної розсилки сервером для OSBB: $osbbId")
  }

  suspend fun publishAnnouncement(announcement: AnnouncementEntity): Result<Unit> {
    if (_firestore == null) return Result.failure(Exception("Firestore not ready"))
    return try {
      val col = firestore.collection("announcements")
      val timestamp = currentTimeMillis()
      
      val dataMap = mutableMapOf<String, Any?>(
          "title" to announcement.title,
          "message" to announcement.message,
          "osbbId" to safeNum(announcement.osbbId),
          "timestamp" to safeNum(timestamp),
          "imageUrl" to announcement.imageUrl,
          "authorUid" to announcement.authorUid,
          "authorName" to announcement.authorName,
          "authorRole" to announcement.authorRole,
          "fileUrl" to announcement.fileUrl,
          "fileName" to announcement.fileName,
          "isPriority" to announcement.isPriority
      )
      col.add(dataMap)
      
      sendGlobalNotification(announcement.title, announcement.message.take(100), announcement.osbbId, announcement.imageUrl)
      Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
    if (_firestore == null || announcementId.isBlank()) return Result.failure(Exception("Firestore not ready or empty ID"))
    return try {
      firestore.collection("announcements").document(announcementId).delete()
      Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
  }

  fun observeAnnouncements(osbbId: Long): Flow<List<AnnouncementEntity>> {
    if (_firestore == null) return flow { emit(emptyList()) }
    return firestore.collection("announcements")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            // ИСПРАВЛЕНО: Защищенный парсинг каждого отдельного объявления
            doc.data<AnnouncementEntity>().copy(id = doc.id)
          } catch (e: Exception) {
            println("[ChatRepository_WARN]: Пропуск некоректного оголошення ${doc.id}: ${e.message}")
            null
          }
        }.filter { 
            val itemOsbbId = it.osbbId
            itemOsbbId == 0L || itemOsbbId == osbbId 
        }.sortedByDescending { it.timestamp }
      }
      .catch { e -> 
        println("[ChatRepository_ERROR]: Помилка потоку оголошень: ${e.message}")
        emit(emptyList()) 
      }
  }

  fun observeMessages(chatUid: String, limit: Int = 50): Flow<List<MessageEntity>> {
    if (_realtime == null) return flow { emit(emptyList()) }
    return realtime.reference("chats/$chatUid")
      .limitToLast(limit)
      .valueEvents
      .map { snapshot ->
        snapshot.children.mapNotNull { child ->
          try {
            child.value<MessageEntity>()
          } catch (e: Exception) {
            null
          }
        }.sortedBy { it.timestamp }
      }
      .catch { emit(emptyList()) }
  }

  fun observeLastMessage(chatUid: String): Flow<MessageEntity?> {
    if (_realtime == null) return flow { emit(null) }
    return realtime.reference("chats/$chatUid")
      .limitToLast(1)
      .valueEvents
      .map { snapshot -> snapshot.children.lastOrNull()?.value<MessageEntity>() }
      .catch { emit(null) }
  }

  fun observePresence(chatId: String): Flow<Map<String, Boolean>> {
    if (_realtime == null) return flow { emit(emptyMap()) }
    return realtime.reference("presence/$chatId")
      .valueEvents
      .map { snapshot ->
        snapshot.children.associate { child ->
          val uid = child.key ?: ""
          val isOnline = try {
            val onlineChild = child.child("online")
            if (onlineChild.exists) onlineChild.value<Boolean?>() ?: false else false
          } catch (e: Exception) { false }
          uid to isOnline
        }
      }
      .catch { emit(emptyMap()) }
  }

  suspend fun sendMessage(path: String, message: MessageEntity): Result<Unit> {
    if (_realtime == null) return Result.failure(Exception("Realtime not ready"))
    return try {
      val ref = realtime.reference("chats/$path")
      val key = if (message.id.isBlank() || message.id == "init") ref.push().key ?: "" else message.id
      
      if (com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)) {
          // ИСПРАВЛЕНО НАМЕРТВО: Используем чистую структуру без Kotlin-списков
          val dataMap = mutableMapOf<String, Any?>(
              "id" to key,
              "senderUid" to message.senderUid,
              "senderDisplayedName" to message.senderDisplayedName,
              "senderLogoUrl" to message.senderLogoUrl,
              "senderAddress" to message.senderAddress,
              "text" to message.text,
              "type" to message.type,
              "imageUrl" to message.imageUrl,
              "fileUrl" to message.fileUrl,
              "fileName" to message.fileName,
              "timestamp" to safeNum(message.timestamp),
              "read" to message.read,
              "edited" to message.edited,
              "fromAdmin" to message.fromAdmin,
              "forwarded" to message.isForwarded,
              "imageWidth" to message.imageWidth,
              "imageHeight" to message.imageHeight
          )
          ref.child(key).setValue(dataMap)
      } else {
          ref.child(key).setValue(message.copy(id = key))
      }

      Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
  }

  suspend fun isChatBranchExists(path: String): Boolean {
    if (_realtime == null) return false
    return try {
      val snapshot = realtime.reference("chats/$path").valueEvents.first()
      snapshot.exists
    } catch (e: Exception) { false }
  }

  suspend fun markMessagesAsRead(chatId: String, myUid: String) {
    if (_realtime == null) return
    try {
      val ref = realtime.reference("chats/$chatId")
      val snapshot = ref.limitToLast(50).valueEvents.first()
      val updates = mutableMapOf<String, Any?>()
      snapshot.children.forEach { child ->
        val senderUid = child.child("senderUid").value<String?>()
        val isRead = child.child("read").value<Boolean?>() ?: false
        if (!isRead && senderUid != myUid && senderUid != null) {
          val msgKey = child.key
          if (msgKey != null) updates["$msgKey/read"] = true
        }
      }
      if (updates.isNotEmpty()) ref.updateChildren(updates)
    } catch (e: Exception) { }
  }

  suspend fun isFileExists(storagePath: String): Boolean {
    if (_storage == null) return false
    return try {
      storage.reference(storagePath).getDownloadUrl()
      true
    } catch (e: Exception) {
      false
    }
  }

  suspend fun uploadFile(imageData: ByteArray, storagePath: String): String {
    if (_storage == null) throw Exception("Storage not ready")
    val ref = storage.reference(storagePath)
    ref.putData(imageData.wrapForFirebase())
    val downloadUrl = ref.getDownloadUrl()
    println("[YkisLogKMP.Storage]: Файл завантажено! Шлях у консолі: $storagePath")
    println("[YkisLogKMP.Storage]: Пряме посилання: $downloadUrl")
    return downloadUrl
  }

  suspend fun deleteFileFromStorage(url: String) {
    if (_storage == null) return
    try { storage.reference(url).delete() } catch (e: Exception) { }
  }

  suspend fun setUserOnline(chatId: String, uid: String) {
    if (_realtime == null) return
    val sessionId = currentTimeMillis().toString()
    val ref = realtime.reference("presence/$chatId/$uid/sessions/$sessionId")
    ref.setValue(true)
    ref.onDisconnect().removeValue()
    realtime.reference("presence/$chatId/$uid/online").setValue(true)
    realtime.reference("presence/$chatId/$uid/online").onDisconnect().removeValue()
  }

  suspend fun setUserOffline(chatId: String, uid: String) {
    if (_realtime == null) return
    realtime.reference("presence/$chatId/$uid").removeValue()
  }

  suspend fun updateMessage(path: String, msgId: String, updates: Map<String, Any?>) {
    if (_realtime == null) return
    realtime.reference("chats/$path/$msgId").updateChildren(updates)
  }

  suspend fun removeMessage(path: String, msgId: String) {
    if (_realtime == null) return
    realtime.reference("chats/$path/$msgId").removeValue()
  }

  suspend fun removeChatBranch(path: String) {
    if (_realtime == null) return
    realtime.reference("chats/$path").removeValue()
    realtime.reference("presence/$path").removeValue()
    realtime.reference("chat_access/$path").removeValue()
  }

  suspend fun addChatParticipant(chatId: String, uid: String) {
    if (_realtime == null) return
    try {
      realtime.reference("chat_access/$chatId/$uid").setValue(true)
    } catch (e: Exception) { }
  }

  /**
   * [subscribeToChats] — Масова підписка користувача на список чатів.
   * Використовується адмінами для авто-реєстрації в нових гілках.
   */
  suspend fun subscribeToChats(chatIds: List<String>, uid: String) {
    if (_realtime == null || chatIds.isEmpty()) return
    try {
        val updates = mutableMapOf<String, Any?>()
        chatIds.forEach { id ->
            updates["chat_access/$id/$uid"] = true
        }
        realtime.reference().updateChildren(updates)
    } catch (e: Exception) {
        println("[ChatRepository.subscribeToChats_ERROR]: ${e.message}")
    }
  }

  /**
   * [subscribeUsersToChat] — Реєстрація списку користувачів в одному чаті.
   */
  suspend fun subscribeUsersToChat(uids: List<String>, chatId: String) {
    if (_realtime == null || uids.isEmpty()) return
    try {
        val updates = mutableMapOf<String, Any?>()
        uids.forEach { uid ->
            updates[uid] = true
        }
        // Записуємо напряму у гілку чату
        realtime.reference("chat_access/$chatId").updateChildren(updates)
    } catch (e: Exception) {
        println("[ChatRepository.subscribeUsersToChat_ERROR]: ${e.message}")
    }
  }

  suspend fun removeChatParticipant(chatId: String, uid: String) {
    if (_realtime == null) return
    try { realtime.reference("chat_access/$chatId/$uid").removeValue() } catch (e: Exception) { }
  }

  suspend fun compressImage(path: String): ByteArray = platformCompressImage(path)
  suspend fun readFileAsBytes(path: String): ByteArray = platformReadFileAsBytes(path)
  suspend fun askAiAssistant(prompt: String) = aiManager.askAssistant(prompt)
  suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray) = aiManager.analyzeMeterImage(prompt, imageData)

  suspend fun deleteMessageForUser(chatPath: String, messageId: String, uid: String): Result<Unit> {
    if (_realtime == null) return Result.failure(Exception("Realtime not ready"))
    return try {
      val ref = realtime.reference("chats/$chatPath/$messageId/deletedFor")
      val snapshot = ref.valueEvents.first()
      val currentList = snapshot.value<List<String>?>() ?: emptyList()
      if (!currentList.contains(uid)) {
        val newList = currentList + uid
        ref.setValue(newList)
      }
      Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
  }
}

expect suspend fun platformCompressImage(path: String): ByteArray
expect suspend fun platformReadFileAsBytes(path: String): ByteArray
