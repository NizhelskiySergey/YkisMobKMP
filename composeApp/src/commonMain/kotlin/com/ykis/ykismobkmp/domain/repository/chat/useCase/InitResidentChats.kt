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

  operator fun invoke(
    scope: CoroutineScope,
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

    scope.launch(Dispatchers.Default) {
      serviceMap.forEach { (prefix, sysId) ->
        if (prefix == "OSBB" && sysId == 0L) {
            println("[YkisLogKMP.$className.$methodName]: Пропуск чату ОСББ (ID=0)")
            return@forEach 
        }

        val chatPath = "${prefix}_${sysId}_${addressId}"
        println("[YkisLogKMP.$className.$methodName]: Перевірка гілки: $chatPath")

        try {
          // 1. Реєструємо користувача
          chatRepo.addChatParticipant(chatPath, uid)

          // 2. Перевіряємо існування
          val isExists = chatRepo.isChatBranchExists(chatPath)

          if (!isExists) {
            println("[YkisLogKMP.$className.$methodName]: Створення НОВОГО чату $chatPath")

            val cleanNanim = if (nanim.isBlank() || nanim == "Мешканець") "Жилець" else nanim

            val message = MessageEntity(
                id = "",
                senderUid = uid,
                text = "Вітаю! Чат з ${if (prefix == "OSBB") "ОСББ" else "службою"} активовано.",
                senderDisplayedName = cleanNanim,
                senderAddress = addressText,
                timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis(),
                read = false
            )

            chatRepo.sendMessage(path = chatPath, message = message)
            
            // 3. Сповіщення адмінів
            val adminUids = chatRepo.fetchAdminsByOsbb(sysId).map { it.uid }.filter { it != uid }
            if (adminUids.isNotEmpty()) {
                chatRepo.incrementUnreadForUids(chatPath, adminUids)
            }
          } else {
            println("[YkisLogKMP.$className.$methodName]: Чат $chatPath вже активовано.")
          }
        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: Помилка для $chatPath: ${e.message}")
        }
      }
    }
  }
}
