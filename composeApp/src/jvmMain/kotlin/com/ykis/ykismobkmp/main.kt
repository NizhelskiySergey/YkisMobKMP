package com.ykis.ykismobkmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.di.initKoin
import org.koin.dsl.module

fun main() = application {
  // 1. Создаем платформенный модуль для Mac Desktop
  val desktopModule = module {
    // На Mac фабрика драйверов создается без контекста
    single { DatabaseDriverFactory() }
  }

  // 2. Запускаем кроссплатформенный Koin при старте программы на Mac
  initKoin(platformModule = desktopModule)

  // 3. Запуск Compose окна приложения
  Window(onCloseRequest = ::exitApplication, title = "Ykis KMP Admin") {
    App()
  }
}
