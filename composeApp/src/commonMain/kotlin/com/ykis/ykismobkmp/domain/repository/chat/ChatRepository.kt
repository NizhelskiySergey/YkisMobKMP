package com.ykis.ykismobkmp.domain.repository.chat


import com.ykis.ykismobkmp.core.utils.currentTimeMillis
import com.ykis.ykismobkmp.core.utils.wrapForFirebase
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.entity.*
import com.ykis.ykismobkmp.domain.services.UserRole
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

  suspend fun fetchUsersByIds(ids: List<String>): List<UserEntity> = coroutineScope {
    if (ids.isEmpty() || _firestore == null) return@coroutineScope emptyList()
    
    val distinctIds = ids.distinct().take(20)
    
    try {
      val deferreds = distinctIds.map { uid ->
        async {
          try {
            val doc = firestore.collection("users").document(uid).get()
            if (doc.exists) {
                doc.data<UserEntity>().copy(uid = doc.id)
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
    return realtime.reference("chats")
      .orderByKey()
      .startAt(prefix)
      .endAt(prefix + "\uf8ff")
      .valueEvents
      .map { snapshot ->
        snapshot.children.mapNotNull { it.key }
      }
  }

  suspend fun fetchAdminsByOsbb(osbbId: Long): List<UserEntity> {
    if (_firestore == null) return emptyList()
    return try {
      val q1 = firestore.collection("users").where { "osbbId" equalTo osbbId }.get()
      val q2 = firestore.collection("users").where { "osbbId" equalTo osbbId.toString() }.get()
      val combined = (q1.documents + q2.documents).distinctBy { it.id }
      combined.mapNotNull { doc ->
        try { doc.data<UserEntity>().copy(uid = doc.id) } catch (e: Exception) { null }
      }.filter { it.userRole != UserRole.StandardUser && it.userRole != UserRole.Unknown }
    } catch (e: Exception) { emptyList() }
  }

  suspend fun fetchUserByAddressId(addressId: Long): UserEntity? {
    if (_firestore == null) return null
    return try {
      val resultNum = firestore.collection("users").where { "addressId" equalTo addressId }.get()
      val resultStr = firestore.collection("users").where { "addressId" equalTo addressId.toString() }.get()
      val doc = (resultNum.documents + resultStr.documents).firstOrNull()
      doc?.let { it.data<UserEntity>().copy(uid = it.id) }
    } catch (e: Exception) { null }
  }

  fun observeUnreadCounts(myUid: String): Flow<Map<String, Int>> {
    if (_realtime == null) return flow { emit(emptyMap()) }
    return realtime.reference("unread_counters/$myUid")
      .valueEvents
      .map { snapshot ->
        snapshot.children.associate { it.key!! to (it.value<Int?>() ?: 0) }
      }
  }

  suspend fun resetUnreadCount(chatId: String, myUid: String) {
    if (_realtime == null) return
    try {
      realtime.reference("unread_counters/$myUid/$chatId").setValue(0)
    } catch (e: Exception) { }
  }

  suspend fun incrementUnreadForUids(chatId: String, uids: List<String>) {
    if (_realtime == null || uids.isEmpty()) {
        println("[YkisLogKMP.ChatRepository]: Инкремент отменен (Realtime null или список UID пуст)")
        return
    }
    try {
      val distinctUids = uids.distinct()
      println("[YkisLogKMP.ChatRepository]: [BADGE_START] Инкремент для чата $chatId. Получатели: $distinctUids")
      
      distinctUids.forEach { uid ->
        val userPresenceRef = realtime.reference("presence/$chatId/$uid")
        val snapshot = userPresenceRef.valueEvents.first()
        val isOnline = if (snapshot.exists) snapshot.child("online").value<Boolean?>() ?: false else false
        
        if (!isOnline) {
            val unreadRef = realtime.reference("unread_counters/$uid/$chatId")
            val currentSnapshot = unreadRef.valueEvents.first()
            val current = if (currentSnapshot.exists) currentSnapshot.value<Int?>() ?: 0 else 0
            
            println("[YkisLogKMP.ChatRepository]: [BADGE_WRITE] UID $uid: $current -> ${current + 1}")
            unreadRef.setValue(current + 1)
        } else {
            println("[YkisLogKMP.ChatRepository]: [BADGE_SKIP] UID $uid в сети, бэйдж не нужен.")
        }
      }
    } catch (e: Exception) { 
        println("[YkisLogKMP.ChatRepository_ERROR]: Ошибка инкремента: ${e.message}")
    }
  }

  suspend fun incrementUnreadForParticipants(chatId: String, senderUid: String) {
    if (_realtime == null) return
    try {
      val participantsSnapshot = realtime.reference("chat_access/$chatId").valueEvents.first()
      val uids = participantsSnapshot.children.mapNotNull { it.key }.filter { it != senderUid }.distinct()
      if (uids.isNotEmpty()) incrementUnreadForUids(chatId, uids)
    } catch (e: Exception) { }
  }

  suspend fun fetchAllUsersByAddressId(addressId: Long): List<UserEntity> {
    if (_firestore == null) return emptyList()
    return try {
      val resultNum = firestore.collection("users").where { "addressId" equalTo addressId }.get()
      val resultStr = firestore.collection("users").where { "addressId" equalTo addressId.toString() }.get()
      val combined = (resultNum.documents + resultStr.documents).distinctBy { it.id }
      combined.mapNotNull { doc -> try { doc.data<UserEntity>().copy(uid = doc.id) } catch (e: Exception) { null } }
    } catch (e: Exception) { emptyList() }
  }

  suspend fun sendGlobalNotification(title: String, body: String, osbbId: Long = 0L, imageUrl: String? = null) {
    try {
      val data = mutableMapOf<String, Any?>(
        "title" to title, "body" to body, "osbbId" to osbbId, "type" to "ANNOUNCEMENT"
      )
      imageUrl?.let { data["imageUrl"] = it }
      Firebase.functions.httpsCallable("sendGlobalNotification")(data)
    } catch (e: Exception) {
      println("[YkisLogKMP.$className.sendGlobalNotification_ERROR]: ${e.message}")
    }
  }

  suspend fun publishAnnouncement(announcement: AnnouncementEntity): Result<Unit> {
    if (_firestore == null) return Result.failure(Exception("Firestore not ready"))
    return try {
      val col = firestore.collection("announcements")
      val timestamp = currentTimeMillis()
      val finalAnnouncement = announcement.copy(timestamp = timestamp)
      col.add(finalAnnouncement)
      sendGlobalNotification(
        title = finalAnnouncement.title,
        body = finalAnnouncement.message.take(100),
        osbbId = finalAnnouncement.osbbId,
        imageUrl = finalAnnouncement.imageUrl
      )
      Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
    if (_firestore == null) return Result.failure(Exception("Firestore not ready"))
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
        snapshot.documents.map { doc ->
          doc.data<AnnouncementEntity>().copy(id = doc.id)
        }.filter { it.osbbId == 0L || it.osbbId == osbbId }.sortedByDescending { it.timestamp }
      }
      .catch { emit(emptyList()) }
  }

  fun observeMessages(chatUid: String, limit: Int = 20): Flow<List<MessageEntity>> {
    if (_realtime == null) return flow { emit(emptyList()) }
    return realtime.reference("chats/$chatUid")
      .limitToLast(limit)
      .childEvents()
      .scan(emptyList<MessageEntity>()) { accumulator, event ->
        val message = try { event.snapshot.value<MessageEntity>() } catch (e: Exception) { return@scan accumulator }
        when (event.type) {
          ChildEvent.Type.ADDED -> (accumulator + message).sortedBy { it.timestamp }
          ChildEvent.Type.CHANGED -> accumulator.map { if (it.id == message.id) message else it }
          ChildEvent.Type.REMOVED -> accumulator.filter { it.id != message.id }
          else -> accumulator
        }
      }
  }

  fun observeLastMessage(chatUid: String): Flow<MessageEntity?> {
    if (_realtime == null) return flow { emit(null) }
    return realtime.reference("chats/$chatUid")
      .limitToLast(1)
      .valueEvents
      .map { snapshot -> snapshot.children.lastOrNull()?.value<MessageEntity>() }
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
  }

  suspend fun sendMessage(path: String, message: MessageEntity): Result<Unit> {
    if (_realtime == null) return Result.failure(Exception("Realtime not ready"))
    return try {
      val ref = realtime.reference("chats/$path")
      val key = if (message.id.isBlank() || message.id == "init") ref.push().key ?: "" else message.id
      ref.child(key).setValue(message.copy(id = key))
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

  suspend fun uploadFile(imageData: ByteArray, storagePath: String): String {
    if (_storage == null) throw Exception("Storage not ready")
    val ref = storage.reference(storagePath)
    ref.putData(imageData.wrapForFirebase())
    return ref.getDownloadUrl()
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
