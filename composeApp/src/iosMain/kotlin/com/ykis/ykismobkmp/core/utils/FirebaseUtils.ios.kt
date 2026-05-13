package com.ykis.ykismobkmp.core.utils


import dev.gitlive.firebase.storage.Data
import platform.Foundation.NSData
import platform.Foundation.create
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.wrapForFirebase(): Data {
  val nsData = this.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
  }
  return Data(nsData)
}
