package com.ykis.ykismobkmp.core.utils

import kotlin.js.Date
import kotlin.js.json

actual fun formatDateFull(timestamp: Long): String {
  val date = Date(timestamp.toDouble())

  // Создаем объект настроек и приводим его к нужному типу через unsafeCast
  val options = json(
    "day" to "numeric",
    "month" to "long",
    "year" to "numeric"
  ).unsafeCast<Date.LocaleOptions>()

  // Теперь передаем строку локали и объект опций
  return date.toLocaleDateString("uk-UA", options)
}
