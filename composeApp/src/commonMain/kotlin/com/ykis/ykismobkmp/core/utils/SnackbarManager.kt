package com.ykis.ykismobkmp.core.utils


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

private const val className = "SnackbarManager"

/**
 * [SnackbarMessage] — Обертка для типов сообщений ЮКИС г. Южный.
 */
sealed class SnackbarMessage {
  data class Text(val message: String) : SnackbarMessage()
  data class Resource(val resId: StringResource) : SnackbarMessage()
}

/**
 * [SnackbarManager] — Единая точка управления уведомлениями в KMP.
 * Работает через StateFlow, который слушает YkisPamAppState.
 * ИСПРАВЛЕНО: Нативные Android логи заменены на кроссплатформенный println().
 */
object SnackbarManager {

  private val _messages = MutableStateFlow<SnackbarMessage?>(null)
  val snackbarMessages: StateFlow<SnackbarMessage?> = _messages.asStateFlow()

  /**
   * Показать текстовое сообщение (например, из ошибки API Ktor)
   */
  fun showMessage(message: String) {
    // ИСПРАВЛЕНО: Платформенный Log.d заменен универсальной КМР-функцией println()
    println("[$className.showMessage]: Text -> $message")
    _messages.update { SnackbarMessage.Text(message) }
  }

  /**
   * Показать сообщение из ресурсов (например, Res.string.error_800)
   */
  fun showMessage(resourceId: StringResource) {
    println("[$className.showMessage]: ResourceId detected")
    _messages.update { SnackbarMessage.Resource(resourceId) }
  }

  /**
   * Очистить текущее сообщение из очереди обработки
   */
  fun clearMessage() {
    _messages.update { null }
  }
}
