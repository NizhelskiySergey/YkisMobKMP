/*
 * Copyright 2022-2024 Google LLC
 * Адаптировано для проекта YkisPam
 */

package com.ykis.ykismobkmp.ui

/**
 * [YkisPamAppState] управляет состоянием UI, включая показ снэкбаров,
 * работая одинаково на Mac, Android и в браузере.
 */

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import com.ykis.ykismobkmp.core.utils.Log
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.SnackbarMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * [YkisPamAppState] управляет состоянием UI, включая показ снэкбаров.
 * Адаптирован для обработки Sealed Class из SnackbarManager.
 */
@Stable
class YkisPamAppState(
  val snackbarHostState: SnackbarHostState,
  private val snackbarManager: SnackbarManager,
  val coroutineScope: CoroutineScope
) {
  private val className = "YkisPamAppState"

  init {
    coroutineScope.launch {
      Log.d("YkisLog", "[$className.init]: Запуск слушателя Snackbar сообщений")

      snackbarManager.snackbarMessages
        .filterNotNull()
        .collect { snackbarMessage ->
          // 1. Формируем текст в зависимости от типа сообщения
          val text = try {
            when (snackbarMessage) {
              is SnackbarMessage.Resource -> {
                // Загружаем строку из мультиплатформенных ресурсов Res
                getString(snackbarMessage.resId)
              }
              is SnackbarMessage.Text -> {
                // Используем готовый текст (например, от сервера)
                snackbarMessage.message
              }
            }
          } catch (e: Exception) {
            Log.e("YkisLog", "[$className.init]: Ошибка получения текста: ${e.message}")
            "Ошибка отображения уведомления"
          }

          if (text.isNotBlank()) {
            Log.d("YkisLog", "[$className.init]: Показ Snackbar: $text")

            // 2. Отображение (suspend функция, ждет завершения показа)
            snackbarHostState.showSnackbar(
              message = text,
              withDismissAction = true
            )

            // 3. Очистка очереди в менеджере
            snackbarManager.clearMessage()
            Log.d("YkisLog", "[$className.init]: Сообщение обработано и удалено")
          }
        }
    }
  }
}

