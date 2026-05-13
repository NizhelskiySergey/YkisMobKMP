package com.ykis.ykismobkmp.core.utils


import java.text.SimpleDateFormat
import java.util.*

actual fun formatDateFull(timestamp: Long): String {
  val date = Date(timestamp)
  val sdf = SimpleDateFormat("d MMMM yyyy", Locale("uk", "UA"))
  return sdf.format(date)
}
