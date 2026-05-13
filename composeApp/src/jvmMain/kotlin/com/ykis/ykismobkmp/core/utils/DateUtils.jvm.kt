package com.ykis.ykismobkmp.core.utils


import java.text.SimpleDateFormat
import java.util.*

actual fun formatDateFull(timestamp: Long): String {
  val date = Date(timestamp)
  // Используем украинскую локаль для корректного отображения месяцев на Mac
  val sdf = SimpleDateFormat("d MMMM yyyy", Locale("uk", "UA"))
  return sdf.format(date)
}

