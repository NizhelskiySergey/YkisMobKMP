package com.ykis.ykismobkmp.core.utils


import java.text.SimpleDateFormat
import java.util.*

actual fun formatDateFull(timestamp: Long): String {
  val date = Date(timestamp)
  // Использование Locale.forLanguageTag или Locale.Builder вместо устаревшего конструктора
  val locale = Locale.Builder().setLanguage("uk").setRegion("UA").build()
  val sdf = SimpleDateFormat("d MMMM yyyy", locale)
  return sdf.format(date)
}
