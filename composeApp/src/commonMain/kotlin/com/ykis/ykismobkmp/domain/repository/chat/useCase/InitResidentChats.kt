package com.ykis.ykismobkmp.domain.repository.chat.useCase

import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [InitResidentChats] — Сценарій ініціалізації базових гілок чатів при додаванні квартири.
 */
class InitResidentChats(
  private val chatRepo: ChatRepository
) {
  private val className = "InitResidentChats"

  suspend operator fun invoke(
    uid: String,
    osbbId: Long,
    addressId: Long,
    addressText: String,
    nanim: String
  ) {
    val methodName = "invoke"
    println("[YkisLogKMP.$className.$methodName]: [START] Активація 4-х ліній чату для о/р: $addressId")

    val serviceMap = mapOf(
      "OSBB"            to osbbId,
      "WATER_SERVICE"   to Constants.WATER_SERVICE_ID,
      "WARM_SERVICE"    to Constants.WARM_SERVICE_ID,
      "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
    )

    serviceMap.forEach { (prefix, sysId) ->
      if (prefix == "OSBB" && sysId == 0L) return@forEach 

      val chatPath = "${prefix}_${sysId}_${addressId}"
      try {
        // 1. Реєструємо САМОГО СЕБЕ (мешканця) - це завжди дозволено і швидко
        chatRepo.addChatParticipant(chatPath, uid)

        // 2. Перевіряємо існування гілки
        // Якщо вона вже є - більше нічого не робимо (адміни вже там або самі додадуться)
        if (!chatRepo.isChatBranchExists(chatPath)) {
          println("[YkisLogKMP.$className]: Гілка $chatPath нова. Пошук адмінів для ініціалізації...")
          
          // Тільки для НОВОГО чату шукаємо адмінів у Firestore
          val adminUids = chatRepo.fetchAdminsByOsbb(sysId).map { it.uid }.filter { it != uid && it.isNotBlank() }
          
          if (adminUids.isNotEmpty()) {
              adminUids.forEach { adminUid ->
                  chatRepo.addChatParticipant(chatPath, adminUid)
              }
          }

          println("[YkisLogKMP.$className]: Створення вітального повідомлення у $chatPath")
          val cleanNanim = if (nanim.isBlank() || nanim == "Мешканець") "Жилець" else nanim
          val message = MessageEntity(
              id = "", senderUid = uid,
              text = "Вітаю! Чат з ${if (prefix == "OSBB") "ОСББ" else "службою"} активовано.",
              senderDisplayedName = cleanNanim, senderAddress = addressText,
              timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis()
          )
          chatRepo.sendMessage(path = chatPath, message = message)
          if (adminUids.isNotEmpty()) chatRepo.incrementUnreadForUids(chatPath, adminUids)
        }
      } catch (e: Exception) {
        println("[YkisLogKMP.$className]: Помилка ініціалізації $chatPath: ${e.message}")
      }
    }
  }
}
