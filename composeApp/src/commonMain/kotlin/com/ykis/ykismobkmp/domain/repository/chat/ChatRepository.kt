package com.ykis.ykismobkmp.domain.repository.chat


import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.currentTimeMillis
import com.ykis.ykismobkmp.core.utils.wrapForFirebase
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.entity.*
import com.ykis.ykismobkmp.domain.services.UserRole
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.firestore.FieldPath.Companion.documentId
import dev.gitlive.firebase.functions.FirebaseFunctions
import dev.gitlive.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import dev.gitlive.firebase.database.ChildEvent
import dev.gitlive.firebase.database.FirebaseDatabase

/**
 * [ChatRepository] — единственный источник данных для чатов и ИИ.
 * ИСПРАВЛЕНО: Безопасное использование компонентов Firebase для предотвращения крашей на iOS/Desktop.
 */
class ChatRepository(
  private val _firestore: FirebaseFirestore?,
  private val _realtime: FirebaseDatabase?,
  private val _storage: FirebaseStorage?,
  private val _functions: FirebaseFunctions?,
  private val aiManager: GeminiAiManager
) {
  private val className = "ChatRepository"

  // Геттеры с проверкой на null, чтобы не падать при отсутствии SDK
  private val firestore get() = _firestore ?: throw IllegalStateException("Firestore not available")
  val realtime get() = _realtime ?: throw IllegalStateException("Realtime Database not available")
  val storage get() = _storage ?: throw IllegalStateException("Storage not available")
  private val functions get() = _functions ?: throw IllegalStateException("Functions not available")

  val currentUid: String?
    get() = try { Firebase.auth.currentUser?.uid } catch (e: Exception) { null }

  // --- 1. FIRESTORE (ПРОФИЛИ) ---

  suspend fun fetchUsersByIds(ids: List<String>): List<UserEntity> {
    if (ids.isEmpty() || _firestore == null) return emptyList()
    val idsToFetch = ids.distinct().take(30)

    return try {
      val result = firestore.collection("users")
        .where { documentId contains idsToFetch }
        .get()

      result.documents.map { doc ->
        mapToUserEntity(doc.id, doc.data())
      }
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.fetchUsersByIds]: Error -> ${e.message}")
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
             if (typingChild.exists) {
                 typingChild.value<Boolean?>() ?: false
             } else {
                 false
             }
          } catch (e: Exception) {
             false
          }
          uid to isTyping
        }
      }
  }

  suspend fun setTypingStatus(chatId: String, uid: String, isTyping: Boolean) {
    if (_realtime == null) return
    try {
      val updates = mapOf("typing" to isTyping)
      realtime.reference("presence/$chatId/$uid").updateChildren(updates)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.setTypingStatus]: ${e.message}")
    }
  }

  fun observeChatKeys(prefix: String): Flow<List<String>> {
    if (_realtime == null) return flowOf(emptyList())
    // ИСПРАВЛЕНО: Убираем orderByKey(), так как на iOS это вызывает конфликт с startAt/endAt.
    // По умолчанию Realtime DB и так сортирует по ключам.
    return realtime.reference("chats")
      .startAt(prefix)
      .endAt(prefix + "\uf8ff")
      .valueEvents
      .map { snapshot ->
        snapshot.children.mapNotNull { it.key }
      }
  }

  suspend fun fetchAdminsByOsbb(osbbId: Long): List<UserEntity> {
    if (_firestore == null) return emptyList()
    val adminRoles = listOf(
      UserRole.VodokanalUser.getSerialName(),
      UserRole.YtkeUser.getSerialName(),
      UserRole.TboUser.getSerialName(),
      UserRole.OsbbUser.getSerialName()
    )

    return try {
      val snapshot = firestore.collection("users")
        .where {
          "osbbId" equalTo osbbId
          "userRole" contains adminRoles
        }
        .get()

      snapshot.documents.map { doc ->
        mapToUserEntity(doc.id, doc.data())
      }
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.fetchAdminsByOsbb]: Error -> ${e.message}")
      emptyList()
    }
  }

  suspend fun fetchUserByAddressId(addressId: Long): UserEntity? {
    if (_firestore == null) return null
    return try {
      val result = firestore.collection("users")
        .where { "addressId" equalTo addressId }
        .get()
      result.documents.firstOrNull()?.let { mapToUserEntity(it.id, it.data()) }
    } catch (e: Exception) {
      null
    }
  }

  suspend fun fetchUserListForService(serviceName: String): List<UserEntity> {
      if (_firestore == null) return emptyList()
      return try {
          val snapshot = firestore.collection("users").get()
          snapshot.documents.map { mapToUserEntity(it.id, it.data()) }
      } catch (e: Exception) {
          emptyList()
      }
  }

  fun observeUnreadCounts(chatPaths: List<String>, myUid: String): Flow<Map<String, Int>> {
    if (_realtime == null || chatPaths.isEmpty()) return flow { emit(emptyMap()) }
    
    val flows = chatPaths.map { path ->
      realtime.reference("chats/$path")
        .valueEvents
        .map { snapshot ->
          val count = snapshot.children.count { child ->
            val senderUid = child.child("senderUid").value<String?>()
            val isRead = child.child("read").value<Boolean?>() ?: false
            senderUid != myUid && !isRead
          }
          path to count
        }
    }
    
    return combine(flows) { it.toMap() }
  }

  suspend fun sendChatNotification(data: Map<String, Any?>) {
    if (_functions == null) return
    try {
      functions.httpsCallable("sendChatNotification").invoke(data)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.sendChatNotification]: Error -> ${e.message}")
    }
  }

  suspend fun sendGlobalNotification(title: String, body: String, osbbId: Long = 0L, imageUrl: String? = null) {
    if (_functions == null) return
    try {
      val data = mutableMapOf<String, Any?>(
        "title" to title,
        "body" to body,
        "osbbId" to osbbId,
        "type" to "ANNOUNCEMENT"
      )
      imageUrl?.let { data["imageUrl"] = it }
      functions.httpsCallable("sendGlobalNotification").invoke(data)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.sendGlobalNotification]: Error -> ${e.message}")
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
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.publishAnnouncement]: Error -> ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
    if (_firestore == null) return Result.failure(Exception("Firestore not ready"))
    return try {
      firestore.collection("announcements").document(announcementId).delete()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.deleteAnnouncement]: Error -> ${e.message}")
      Result.failure(e)
    }
  }

  fun observeAnnouncements(osbbId: Long): Flow<List<AnnouncementEntity>> {
    if (_firestore == null) return flow { emit(emptyList()) }
    return firestore.collection("announcements")
      .snapshots
      .map { snapshot ->
        snapshot.documents.map { doc ->
          doc.data<AnnouncementEntity>().copy(id = doc.id)
        }.filter { 
          it.osbbId == 0L || it.osbbId == osbbId
        }.sortedByDescending { it.timestamp }
      }
      .catch { e ->
        Log.e("YkisLog", "[$className.observeAnnouncements]: Error: ${e.message}")
        emit(emptyList())
      }
  }

  fun observeMessages(chatUid: String): Flow<List<MessageEntity>> {
    if (_realtime == null) return flow { emit(emptyList()) }
    return realtime.reference("chats/$chatUid")
      .childEvents()
      .scan(emptyList<MessageEntity>()) { accumulator, event ->
        val message = try {
          event.snapshot.value<MessageEntity>()
        } catch (e: Exception) {
          return@scan accumulator
        }
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
      .map { snapshot ->
        snapshot.children.lastOrNull()?.value<MessageEntity>()
      }
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
            if (onlineChild.exists) {
              onlineChild.value<Boolean?>() ?: false
            } else {
              false
            }
          } catch (e: Exception) {
            false
          }
          uid to isOnline
        }
      }
  }

  fun observeChatMetadata(chatId: String): Flow<ChatMetadata> {
    if (_realtime == null) return flow { emit(ChatMetadata()) }
    return realtime.reference("chats/$chatId/_metadata")
      .valueEvents
      .map { snapshot ->
        snapshot.value<ChatMetadata?>() ?: ChatMetadata()
      }
  }


  suspend fun sendMessage(path: String, message: MessageEntity): Result<Unit> {
    if (_realtime == null) return Result.failure(Exception("Realtime not ready"))
    return try {
      val ref = realtime.reference("chats/$path")
      val key = if (message.id.isBlank() || message.id == "init") {
        ref.push().key ?: ""
      } else {
        message.id
      }
      ref.child(key).setValue(message.copy(id = key))
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun isChatBranchExists(path: String): Boolean {
    if (_realtime == null) return false
    return try {
      val snapshot = realtime.reference("chats/$path").valueEvents.first()
      snapshot.exists
    } catch (e: Exception) {
      false
    }
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
          if (msgKey != null) {
            updates["$msgKey/read"] = true
          }
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
    try {
      storage.getReferenceFromUrl(url).delete()
    } catch (e: Exception) { }
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
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

expect suspend fun platformCompressImage(path: String): ByteArray
expect suspend fun platformReadFileAsBytes(path: String): ByteArray
