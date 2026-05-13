package com.ykis.ykismobkmp.core.utils


import kotlinx.browser.document

actual fun applyAppBadgeCount(count: Int) {
  val currentTitle = document.title.replace(Regex("""\(\d+\)\s"""), "")
  if (count > 0) {
    document.title = "($count) $currentTitle"
  } else {
    document.title = currentTitle
  }
}
