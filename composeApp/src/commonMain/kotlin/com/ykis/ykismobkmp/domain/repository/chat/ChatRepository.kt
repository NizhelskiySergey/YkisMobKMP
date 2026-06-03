package com.ykis.ykismobkmp.domain.repository.chat


import com.ykis.ykismobkmp.core.utils.Log
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
import dev.gitlive.firebase.database.ChildEvent
import dev.gitlive.firebase.database.FirebaseDatabase

/**
 * [ChatRepository] — единственный источник данных для чатов и ИИ.
 * Использует GitLive Firebase SDK для мультиплатформенности.
 */
class ChatRepository(
  private val firestore: FirebaseFirestore,
  val realtime: FirebaseDatabase,
  val storage: FirebaseStorage,
  private val functions: FirebaseFunctions,
  private val aiManager: GeminiAiManager
) {
  private val className = "ChatRepository"

  val currentUid: String?
    get() = Firebase.auth.currentUser?.uid

  // --- 1. FIRESTORE (ПРОФИЛИ) ---

  suspend fun fetchUsersByIds(ids: List<String>): List<UserEntity> {
    if (ids.isEmpty()) return emptyList()
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
  /**
   * [ChatRepository.observeTyping] — поток статусов печати участников чата.
   * Использует расширения GitLive для парсинга Snapshot в Map.
   */
  fun observeTyping(chatId: String): Flow<Map<String, Boolean>> {
    return realtime.reference("presence/$chatId")
      .valueEvents
      .map { snapshot ->
        snapshot.children.associate { child ->
          val uid = child.key ?: ""
          // Достаем поле typing максимально надежным способом
          val isTyping = try {
             // Использование .child().exists для проверки наличия поля в GitLive SDK
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
    try {
      // Используем updateChildren, чтобы не затереть поле "online"
      val updates = mapOf("typing" to isTyping)
      realtime.reference("presence/$chatId/$uid").updateChildren(updates)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.setTypingStatus]: ${e.message}")
    }
  }
  /**
   * [ChatRepository.observeChatKeys] — Получение списка веток чатов по заданному префиксу.
   * Используется диспетчерами для фильтрации своих чатов (Water, Osbb и т.д.).
   */
  fun observeChatKeys(prefix: String): Flow<List<String>> {
    return realtime.reference("chats")
      .orderByKey()
      .startAt(prefix)
      .endAt(prefix + "\uf8ff") // Спецсимвол для захвата всех веток, начинающихся с префикса
      .valueEvents
      .map { snapshot ->
        // Извлекаем только ключи (ID чатов)
        snapshot.children.mapNotNull { it.key }
      }
  }

  suspend fun fetchAdminsByOsbb(osbbId: Long): List<UserEntity> {
    // ИСПРАВЛЕНО: Используем стабильный SerialName для точного совпадения с полем в Firestore
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

  suspend fun sendChatNotification(data: Map<String, Any?>) {
    try {
      println("[$className.sendChatNotification]: Вызов Cloud Function 'sendChatNotification'...")
      functions.httpsCallable("sendChatNotification").invoke(data)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.sendChatNotification]: Error -> ${e.message}")
    }
  }

  // --- 2. REALTIME DATABASE (СООБЩЕНИЯ) ---

  fun observeMessages(chatUid: String): Flow<List<MessageEntity>> {
    return realtime.reference("chats/$chatUid")
      .childEvents()
      .scan(emptyList<MessageEntity>()) { accumulator, event ->
        val message = try {
          event.snapshot.value<MessageEntity>()
        } catch (e: Exception) {
          Log.e("YkisLog", "[$className.observeMessages]: Parse error")
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
    return realtime.reference("chats/$chatUid")
      .limitToLast(1)
      .valueEvents
      .map { snapshot ->
        snapshot.children.lastOrNull()?.value<MessageEntity>()
      }
  }

  suspend fun sendMessage(path: String, message: MessageEntity): Result<Unit> {
    return try {
      val ref = realtime.reference("chats/$path")
      val key = if (message.id.isBlank() || message.id == "init") {
        ref.push().key ?: ""
      } else {
        message.id
      }

      ref.child(key).setValue(message.copy(id = key))
      Log.d("YkisLog", "[$className.sendMessage]: Success to $path")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.sendMessage]: Error -> ${e.message}")
      Result.failure(e)
    }
  }


  suspend fun isChatBranchExists(path: String): Boolean {
    return try {
      // Вместо проблемного .get() используем поток и берем первый эмит
      val snapshot = realtime.reference("chats/$path").valueEvents.first()
      snapshot.exists
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.isChatBranchExists]: ${e.message}")
      false
    }
  }

  suspend fun markMessagesAsRead(chatId: String, myUid: String) {
    try {
      val ref = realtime.reference("chats/$chatId")
      // Получаем данные (используем valueEvents.first() если .get() не виден)
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
      if (updates.isNotEmpty()) {
        ref.updateChildren(updates)
        Log.d("YkisLog", "[$className.markRead]: Success")
      }
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.markRead]: Error -> ${e.message}")
    }
  }
  suspend fun uploadFile(imageData: ByteArray, storagePath: String): String {
    val ref = storage.reference(storagePath)

    // Твое расширение решает проблему конструктора Data
    val firebaseData = imageData.wrapForFirebase()

    // Выполняем загрузку
    ref.putData(firebaseData)

    // Получаем публичную ссылку для сохранения в Realtime DB
    return ref.getDownloadUrl()
  }
  suspend fun deleteFileFromStorage(url: String) {
    try {
      storage.getReferenceFromUrl(url).delete()
      Log.d("YkisLog", "[$className.deleteFile]: Deleted successfully")
    } catch (e: Exception) {
      Log.e("YkisLog", "[$className.deleteFile]: Error -> ${e.message}")
    }
  }

  suspend fun setUserOnline(chatId: String, uid: String) {
    val ref = realtime.reference("presence/$chatId/$uid")
    // Устанавливаем объект целиком при входе
    ref.setValue(mapOf("online" to true, "typing" to false))
    ref.onDisconnect().removeValue()
  }

  suspend fun setUserOffline(chatId: String, uid: String) {
    realtime.reference("presence/$chatId/$uid").removeValue()
  }

  suspend fun updateMessage(path: String, msgId: String, updates: Map<String, Any?>) {
    realtime.reference("chats/$path/$msgId").updateChildren(updates)
  }

  suspend fun removeMessage(path: String, msgId: String) {
    realtime.reference("chats/$path/$msgId").removeValue()
  }

  suspend fun removeChatBranch(path: String) {
    realtime.reference("chats/$path").removeValue()
  }
  suspend fun compressImage(path: String): ByteArray = platformCompressImage(path)
  suspend fun readFileAsBytes(path: String): ByteArray = platformReadFileAsBytes(path)

  suspend fun askAiAssistant(prompt: String) = aiManager.askAssistant(prompt)

  suspend fun analyzeMeterImage(prompt: String, imageData: ByteArray) =
    aiManager.analyzeMeterImage(prompt, imageData)
  /**
   * [ChatRepository.deleteMessageForUser] — Добавление UID в список удаливших сообщение.
   */
  /**
   * [ChatRepository.deleteMessageForUser] — Добавление UID в список удаливших сообщение.
   */
  suspend fun deleteMessageForUser(chatPath: String, messageId: String, uid: String): Result<Unit> {
    return try {
      val ref = realtime.reference("chats/$chatPath/$messageId/deletedFor")

      // Используем твое исправление: valueEvents.first() вместо get()
      val snapshot = ref.valueEvents.first()

      // Извлекаем список UID, которые уже удалили это сообщение
      val currentList = snapshot.value<List<String>?>() ?: emptyList()

      if (!currentList.contains(uid)) {
        val newList = currentList + uid
        ref.setValue(newList)
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("YkisLog", "[ChatRepository.deleteForMe]: ${e.message}")
      Result.failure(e)
    }
  }



}
expect suspend fun platformCompressImage(path: String): ByteArray
expect suspend fun platformReadFileAsBytes(path: String): ByteArray



