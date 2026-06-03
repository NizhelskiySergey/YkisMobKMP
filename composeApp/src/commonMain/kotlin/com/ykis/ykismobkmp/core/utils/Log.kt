package com.ykis.ykismobkmp.core.utils

import io.github.aakira.napier.Napier

/**
 * [Log] — Единый диспетчер логирования проекта YkisMobKMP.
 * ИСПРАВЛЕНО: Теперь на релизе достаточно изменить флаг isDebug на false, 
 * и все логи во всем приложении будут мгновенно отключены!
 */
object Log {
  // МЕНЯТЬ ЗДЕСЬ ПЕРЕД РЕЛИЗОМ:
  private const val isDebug = true 

  fun d(msg: String, tag: String = "YkisLogKMP") {
    if (isDebug) {
      Napier.d(message = msg, tag = tag)
      println("[$tag]: $msg") // Дублируем в println для удобства в KMP
    }
  }

  fun e(msg: String, tag: String = "YkisLogKMP", throwable: Throwable? = null) {
    if (isDebug) {
      Napier.e(message = msg, tag = tag, throwable = throwable)
      println("[$tag ERROR]: $msg ${throwable?.message ?: ""}")
    }
  }

  fun i(msg: String, tag: String = "YkisLogKMP") {
    if (isDebug) {
      Napier.i(message = msg, tag = tag)
      println("[$tag]: $msg")
    }
  }

  fun w(msg: String, tag: String = "YkisLogKMP") {
    if (isDebug) {
      Napier.w(message = msg, tag = tag)
      println("[$tag WARNING]: $msg")
    }
  }
}
