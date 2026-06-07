package com.ykis.ykismobkmp.domain.repository.chat.useCase

import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [InitResidentChats] — Сценарій ініціалізації базових гілок чатів при додаванні квартири.
 * ІСПРАВЛЕНО: Використовується новий формат шляху без UID та перевірка на існуючий чат.
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
    println("[YkisLogKMP.$className.$methodName]: [START] Перевірка та активація 4 ліній чату для л/с: $addressId")

    val serviceMap = mapOf(
      "OSBB"            to osbbId,
      "WATER_SERVICE"   to Constants.WATER_SERVICE_ID,
      "WARM_SERVICE"    to Constants.WARM_SERVICE_ID,
      "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
    )

    scope.launch(Dispatchers.Default) {
      serviceMap.forEach { (prefix, sysId) ->
        // Новий формат шляху: PREFIX_SYSID_ADDRESSID
        val chatPath = "${prefix}_${sysId}_${addressId}"
        
        try {
          // 1. Реєструємо користувача як учасника чату цієї квартири (для прав доступу)
          chatRepo.addChatParticipant(chatPath, uid)

          // 2. Перевіряємо, чи існує вже цей чат в базі
          val isExists = chatRepo.isChatBranchExists(chatPath)
          
          if (!isExists) {
            println("[YkisLogKMP.$className.$methodName]: Чат $chatPath не знайдено. Створення вітального повідомлення.")
            
            val welcomeText = "Вітаю! Чат активовано."
            chatRepo.sendMessage(
              path = chatPath,
              message = MessageEntity(
                id = "",
                senderUid = uid,
                text = welcomeText,
                senderDisplayedName = nanim,
                senderAddress = addressText,
                timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis(),
                read = false
              )
            )
          } else {
            println("[YkisLogKMP.$className.$methodName]: Чат $chatPath вже існує. Пропуск ініціалізації.")
          }
        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: Помилка активації гілки $chatPath: ${e.message}")
        }
      }
    }
  }
}
