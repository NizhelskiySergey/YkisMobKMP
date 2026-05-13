package com.ykis.ykismobkmp.core.utils

import io.github.aakira.napier.Napier

object Log {
  fun d(tag: String, msg: String) {
    Napier.d(message = msg, tag = tag)
  }

  fun e(tag: String, msg: String) {
    Napier.e(message = msg, tag = tag)
  }

  fun i(tag: String, msg: String) {
    Napier.i(message = msg, tag = tag)
  }

  fun w(tag: String, msg: String) {
    Napier.w(message = msg, tag = tag)
  }
}
