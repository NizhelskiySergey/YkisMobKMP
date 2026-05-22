package com.ykis.ykismobkmp.core.utils

import kotlinx.browser.window

/**
 * Реализация закрытия вкладки для JS / Web таргета.
 * ИСПРАВЛЕНО: Префикс логов переведен на YkisLogKMP.
 */
actual fun closeApplication() {
  println("[YkisLogKMP.PlatformCloseApp]: Кроссплатформенне закриття вкладки в браузері через JS API")
  try {
    window.close()
  } catch (e: Exception) {
    println("[YkisLogKMP.PlatformCloseApp_WARN]: Сбой вызова window.close() (заблокировано политикой браузера): ${e.message}")
  }
}
