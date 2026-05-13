package com.ykis.ykismobkmp.di

import android.content.Context
import com.ykis.ykismobkmp.domain.ai.GeminiAiManager
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.services.AndroidAiManager
import com.ykis.ykismobkmp.services.LocalAiEngine
import com.ykis.ykismobkmp.services.LogServiceImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * [androidModule] — Платформенные зависимости для операционной системы Android.
 * Передает контекст приложения в конструктор нативного ИИ-менеджера.
 */
val androidModule = module {
  // 1. Нативная реализация логгера (с Firebase Analytics для Android)
  single<LogService> { LogServiceImpl() }

  // 2. Нативная реализация ИИ-менеджера (с Gemini Nano)
  single<GeminiAiManager> {
    // get() автоматически достанет Context, переданный через androidContext(context)
    AndroidAiManager(
      context = get(),
      localEngine = LocalAiEngine()
    )
  }
}

/**
 * [initAndroidKoin] — Точка старта DI-графа при запуске Android-приложения.
 * Вызывается внутри Вашего класса Application (например, в BaseApplication.onCreate).
 */
fun initAndroidKoin(context: Context) {
  startKoin {
    // КРИТИЧНО ДЛЯ ANDROID: Регистрирует Context в графе Koin, чтобы его мог взять AndroidAiManager
    androidContext(context)

    modules(
      commonModule,
      databaseModule,
      domainModule,
      androidModule
    )
  }
}

