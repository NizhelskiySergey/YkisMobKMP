package com.ykis.ykismobkmp.domain.repository.chat.useCase

import com.ykis.ykismobkmp.core.Constants
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [DeleteResidentChats] — Сценарий удаления четырех базовых веток чатов ЖКХ при удалении квартиры.
 */
class DeleteResidentChats(
  private val chatRepo: ChatRepository
) {
  private val className = "DeleteResidentChats"

  operator fun invoke(
    scope: CoroutineScope,
    uid: String,
    osbbId: Long,
    addressId: Long
  ) {
    val methodName = "invoke"
    println("[YkisLogKMP.$className.$methodName]: [START] Удаление 4 коммунальных линий чата для л/с: $addressId")

    val serviceMap = mapOf(
      "OSBB"            to osbbId,
      "WATER_SERVICE"   to Constants.WATER_SERVICE_ID,
      "WARM_SERVICE"    to Constants.WARM_SERVICE_ID,
      "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
    )

    scope.launch(Dispatchers.Default) {
      serviceMap.forEach { (prefix, sysId) ->
        val chatPath = "${prefix}_${sysId}_${addressId}_$uid"
        try {
          chatRepo.removeChatBranch(chatPath)
          println("[YkisLogKMP.$className.$methodName]: Ветка чата $chatPath успешно удалена")
        } catch (e: Exception) {
          println("[YkisLogKMP.$className.$methodName]: Ошибка удаления ветки $chatPath: ${e.message}")
        }
      }
    }
  }
}
