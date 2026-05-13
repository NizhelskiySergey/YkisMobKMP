package com.ykis.ykismobkmp.domain.repository.chat


actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
  // В браузере нет прямого доступа к файловой системе по пути String
  return byteArrayOf()
}

actual suspend fun platformCompressImage(path: String): ByteArray {
  return byteArrayOf()
}
