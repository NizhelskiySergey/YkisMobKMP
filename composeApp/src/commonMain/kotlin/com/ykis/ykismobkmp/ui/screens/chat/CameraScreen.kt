package com.ykis.ykismobkmp.ui.screens.chat

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
// РЕШЕНИЕ: Импортируем expect-функцию из изолированного пакета компонентов
import com.ykis.ykismobkmp.ui.components.CameraView

/**
 * [CameraScreen] — Экран камеры Voyager.
 * Использует кроссплатформенный мост CameraView для съемки показаний счетчиков.
 */
class CameraScreen : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow

    // Убедись, что имя класса совпадает с тем, что зарегистрировано в Koin.kt
    val chatViewModel = koinInject< ChatViewModelModel>()

    CameraView(
      onImageCaptured = { path ->
        // Сохраняем путь к фото в стейт модели чата
        chatViewModel.setSelectedImagePath(path)
        // Переходим на экран подтверждения отправки фото счетчика
        navigator.push(SendImageScreen(imagePath = path, address = ""))
      },
      onBack = { navigator.pop() }
    )
  }
}
