package com.ykis.ykismobkmp.core.utils


import platform.Foundation.*

actual fun formatDateFull(timestamp: Long): String {
  val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
  val formatter = NSDateFormatter().apply {
    dateFormat = "d MMMM yyyy"
    locale = NSLocale("uk_UA")
  }
  return formatter.stringFromDate(date)
}
