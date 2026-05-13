package com.ykis.ykismobkmp.core.utils


sealed class Resource<T>(
  val data: T? = null,
  val message: String? = null,
  // В KMP мы не используем Int для ресурсов.
  // Если нужно передавать именно ключ перевода, используем String или StringRes из KMP
  val errorKey: String? = null
) {
  class Success<T>(data: T?) : Resource<T>(data)

  class Error<T>(
    message: String? = null,
    errorKey: String? = null,
    data: T? = null
  ) : Resource<T>(data, message, errorKey)

  class Loading<T>(data: T? = null) : Resource<T>(data)
}
