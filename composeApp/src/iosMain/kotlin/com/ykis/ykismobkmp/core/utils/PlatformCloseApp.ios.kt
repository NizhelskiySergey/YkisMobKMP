package com.ykis.ykismobkmp.core.utils

import platform.posix.exit

actual fun closeApplication() {
  println("[YkisLogKMP.PlatformCloseApp]: Принудительное завершение процесса на iOS/Mac")
  exit(0)
}
