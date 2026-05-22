package com.ykis.ykismobkmp.core.utils

import android.os.Process
import kotlin.system.exitProcess

actual fun closeApplication() {
  println("[YkisLogKMP.PlatformCloseApp]: Принудительное завершение процесса на Android")
  exitProcess(0)
}
