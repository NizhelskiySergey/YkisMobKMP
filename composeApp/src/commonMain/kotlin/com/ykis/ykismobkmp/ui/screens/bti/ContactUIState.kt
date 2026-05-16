package com.ykis.ykismobkmp.ui.screens.bti

import kotlinx.serialization.Serializable

/**
 * [ContactUIState] — Кроссплатформенное состояние формы изменения контактных данных БТИ.
 * Полностью типизировано под Long-идентификатор для бесшовной стыковки с СУБД SQLDelight 2.x.
 */
@Serializable
data class ContactUIState(
  // ИСПРАВЛЕНО: addressId переведен из Int на тип Long согласно сквозному КМР-стандарту проекта
  val addressId: Long = 0L,
  val address: String = "",
  val email: String = "",
  val phone: String = ""
)
