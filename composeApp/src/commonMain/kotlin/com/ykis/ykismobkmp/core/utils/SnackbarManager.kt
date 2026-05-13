package com.ykis.ykismobkmp.core.utils


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

/**
 * [SnackbarManager] — единая точка управления уведомлениями в KMP.
 * Работает через StateFlow, который слушает YkisPamAppState.
 */
object SnackbarManager {
  private val className = "SnackbarManager"

  private val _messages = MutableStateFlow<SnackbarMessage?>(null)
  val snackbarMessages: StateFlow<SnackbarMessage?> = _messages.asStateFlow()

  // Показать текстовое сообщение (например, из ошибки API)
  fun showMessage(message: String) {
    Log.d("YkisLog", "[$className.showMessage]: Text -> $message")
    _messages.update { SnackbarMessage.Text(message) }
  }

  // Показать сообщение из ресурсов (например, Res.string.error_800)
  fun showMessage(resourceId: StringResource) {
    Log.d("YkisLog", "[$className.showMessage]: ResourceId detected")
    _messages.update { SnackbarMessage.Resource(resourceId) }
  }

  fun clearMessage() {
    _messages.update { null }
  }
}

/**
 * Обертка для типов сообщений
 */
sealed class SnackbarMessage {
  data class Text(val message: String) : SnackbarMessage()
  data class Resource(val resId: StringResource) : SnackbarMessage()
}

