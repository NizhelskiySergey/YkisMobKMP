package com.ykis.ykismobkmp.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.domain.services.LogService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

private const val className = "BaseScreenModel"

/**
 * [BaseScreenModel] — Базовая модель для всех экранов YkisMobKMP.
 */
open class BaseScreenModel(
  val logService: LogService
) : ScreenModel { // Наследуем нативное КМР-ядро Voyager

  // Глобальное состояние приложения (UID, Роль, Загрузка), общее для всех ЖКХ-модулей БТИ
  protected val _uiState = MutableStateFlow(BaseUIState())
  val uiState: StateFlow<BaseUIState> = _uiState.asStateFlow()

  /**
   * Безопасный запуск корутин с автоматическим логированием ошибок в Firebase Crashlytics KMP.
   * @param snackbar показывать ли сообщение об ошибке пользователю.
   * @param showLoader показывать ли индикатор загрузки во время выполнения.
   */
  fun launchCatching(
    snackbar: Boolean = true,
    showLoader: Boolean = false,
    block: suspend CoroutineScope.() -> Unit
  ) = screenModelScope.launch( // ИСПРАВЛЕНО: Область корутин переведена на screenModelScope
    CoroutineExceptionHandler { _, throwable ->
      println("[$className.launchCatching]: Перехвачено критическое исключение: ${throwable.message}")

      if (showLoader) hideProgress()

      if (snackbar) {
        val errorText = throwable.message ?: "Невідома помилка системи"
        SnackbarManager.showMessage(errorText)
      }

      // Логируем некритический краш в Firebase Crashlytics KMP
      logService.logNonFatalCrash(throwable)
    }
  ) {
    if (showLoader) showProgress()
    block()
    if (showLoader) hideProgress()
  }

  /**
   * Управление состоянием загрузки лицевых счетов и биллинга ГИОЦ
   */
  fun showProgress() {
    _uiState.update { it.copy(mainLoading = true) } // Синхронизировано с твоим стейтом mainLoading
  }

  fun hideProgress() {
    _uiState.update { it.copy(mainLoading = false) } // Синхронизировано с твоим стейтом mainLoading
  }

  /**
   * Хелпер для вывода быстрых локализованных сообщений из JetBrains мультиплатформенных ресурсов
   */
  fun showMessage(resourceId: StringResource) {
    println("[$className.showMessage]: Запрос на вывод локализованного сообщения")
    SnackbarManager.showMessage(resourceId)
  }

  /**
   * Перегрузка хелпера для вывода обычного динамического текста (например, ошибок PHP-бэкенда)
   */
  fun showMessage(message: String) {
    println("[$className.showMessage]: Запрос на вывод текстового сообщения -> $message")
    SnackbarManager.showMessage(message)
  }
}
