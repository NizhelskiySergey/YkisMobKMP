package com.ykis.ykismobkmp.core.utils


import kotlin.js.Date

actual fun currentTimeMillis(): Long {
  // В JS Date.now() возвращает миллисекунды как Double
  return Date.now().toLong()
}
