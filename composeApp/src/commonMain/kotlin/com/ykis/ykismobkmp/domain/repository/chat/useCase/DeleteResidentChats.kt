package com.ykis.ykismobkmp.domain.repository.chat.useCase

import com.ykis.ykismobkmp.core.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [DeleteResidentChats] — Сценарий удаления четырех базовых веток чатов ЖКХ при удалении квартиры.
 */
class DeleteResidentChats {
  private val className = "DeleteResidentChats"

  operator fun invoke(
    scope: CoroutineScope,
    uid: String,
    osbbId: Long,
    addressId: Long
  ) {
    val methodName = "invoke"
    println("[YkisLogKMP.$className.$methodName]: [START] Отвязка коммунальных линий чата для л/с: $addressId")

    val serviceMap = mapOf(
      "OSBB"            to osbbId,
      "WATER_SERVICE"   to Constants.WATER_SERVICE_ID,
      "WARM_SERVICE"    to Constants.WARM_SERVICE_ID,
      "GARBAGE_SERVICE" to Constants.GARBAGE_SERVICE_ID
    )

    scope.launch(Dispatchers.Default) {
      serviceMap.forEach { (prefix, _) ->
        // ИСПРАВЛЕНО: Поскольку мы решили не удалять ветки и не помечать их (так как у пользователя
        // могут быть другие активные устройства), здесь мы просто логируем событие отвязки.
        // Безопасность пушей теперь гарантируется проверкой наличия квартиры в RootNavGraph.
        println("[YkisLogKMP.$className.$methodName]: Л/С $addressId отвязан от устройства для линии $prefix")
      }
    }
  }
}
