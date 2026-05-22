package com.ykis.ykismobkmp.core.utils


import kotlin.system.exitProcess

/**
 * Реализация принудительного закрытия приложения для JVM (Desktop) таргета.
 * ИСПРАВЛЕНО: Префикс логов переведен на YkisLogKMP.
 */
actual fun closeApplication() {
  println("[YkisLogKMP.PlatformCloseApp]: Примусове завершення процесу на JVM Desktop")
  exitProcess(0)
}
