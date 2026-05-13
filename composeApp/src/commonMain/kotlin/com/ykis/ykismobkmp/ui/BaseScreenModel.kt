package com.ykis.ykismobkmp.ui


import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope

import com.ykis.ykismobkmp.core.utils.Log
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

/**
 * [BaseScreenModel] — базовая модель для всех экранов в KMP.
 * Аналог Android ViewModel, адаптированный под Voyager и мультиплатформенность.
 */
open class BaseScreenModel(
  protected val logService: LogService
) : ScreenModel {

  private val className = "BaseScreenModel"

  // Единое состояние экрана
  protected val _uiState = MutableStateFlow(BaseUIState())
  val uiState: StateFlow<BaseUIState> = _uiState.asStateFlow()

  /**
   * Безопасный запуск корутин с логированием.
   * Использует [screenModelScope] для автоматической отмены при закрытии экрана.
   */
  fun launchCatching(
    snackbar: Boolean = true,
    showLoader: Boolean = false,
    block: suspend CoroutineScope.() -> Unit
  ) = screenModelScope.launch(
    CoroutineExceptionHandler { _, throwable ->
      Log.e("YkisLog", "[$className.launchCatching]: FATAL -> ${throwable.message}")

      if (showLoader) hideProgress()

      if (snackbar) {
        // Передаем ошибку в наш мультиплатформенный SnackbarManager
        SnackbarManager.showMessage(throwable.message ?: "Unknown Error")
      }

      // Логируем в Firebase KMP Crashlytics через твой сервис
      logService.logNonFatalCrash(throwable)
    }
  ) {
    if (showLoader) showProgress()
    block()
    if (showLoader) hideProgress()
  }

  // --- Управление состоянием загрузки ---

  fun showProgress() {
    Log.d("YkisLog", "[$className.showProgress]: Loading started")
    _uiState.update { it.copy(isLoading = true) }
  }

  fun hideProgress() {
    Log.d("YkisLog", "[$className.hideProgress]: Loading finished")
    _uiState.update { it.copy(isLoading = false) }
  }

  /**
   * Вывод сообщений через ресурсы [Res.string]
   */
  fun showMessage(resource: StringResource) {
    SnackbarManager.showMessage(resource)
  }

  /**
   * Вывод текстовых сообщений (например, ошибки от PHP сервера)
   */
  fun showMessage(message: String) {
    SnackbarManager.showMessage(message)
  }

  override fun onDispose() {
    Log.d("YkisLog", "[$className.onDispose]: ScreenModel destroyed")
    super.onDispose()
  }
}
