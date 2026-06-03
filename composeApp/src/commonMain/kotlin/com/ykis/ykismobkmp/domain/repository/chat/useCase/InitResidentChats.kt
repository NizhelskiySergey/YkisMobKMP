package com.ykis.ykismobkmp.domain.repository.chat.useCase

import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.domain.entity.MessageEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [InitResidentChats] — Сценарий инициализации четырех базовых веток чатов ЖКХ при первом добавлении квартиры.
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
    println("[YkisLogKMP.$className.$methodName]: [START] Активация 4 коммунальных линий чата для л/с: $addressId")

    val serviceMap = mapOf(
      "OSBB"            to osbbId,
      "WATER_SERVICE"   to Constants.WATER_SERVICE_ID,
      "WARM_SERVICE"    to Constants.WARM_SERVICE_ID,
      "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
    )

    scope.launch(Dispatchers.Default) {
      serviceMap.forEach { (prefix, sysId) ->
        val chatPath = "${prefix}_${sysId}_${addressId}_$uid"
        val welcomeText = "Вітаю! Чат активовано."
        try {
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
          println("[YkisLogKMP.$className.$methodName]: Комната чата $chatPath успешно активирована")
        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: Ошибка активации ветки $chatPath: ${e.message}")
        }
      }
    }
  }
}
