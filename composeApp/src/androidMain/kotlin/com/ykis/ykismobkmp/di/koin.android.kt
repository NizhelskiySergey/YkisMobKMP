package com.ykis.ykismobkmp.di
import android.content.Context
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import org.koin.core.module.Module
import org.koin.dsl.module


private const val className = "AndroidModules"

/**
 * [androidPlatformModule] — Специфический платформенный Android-модуль инжекции контекстов ОС.
 * имя аргумента AppSettingsRepositoryImpl выровнено в строгий стандарт settings = get().
 */
val androidPlatformModule: Module = module {

  // 1. ИСПРАВЛЕНО: Аргумент конструктора переведен на зафиксированный нами КМР-стандарт settings = get()
  single<AppSettingsRepository> {
    AppSettingsRepositoryImpl(settings = get())
  }

  // 2. Нативный драйвер локального кэша СУБД SQLDelight для Android-устройств
  single {
    DatabaseDriverFactory(context = get())
  }

  // 3. Локальный оффлайн-движок искусственного интеллекта
  single {
    LocalAiEngine()
  }
}

/**
 * [initAndroidKoin] — Точка нативного пускового старта DI-графа на уровне MainActivity.kt.
 * ИСПРАВЛЕНО: Контекст приложения регистрируется в первую очередь, предотвращая NoDefinitionFoundException.
 */
fun initAndroidKoin(context: Context) {
  println("[$className.initAndroidKoin]: Сборка платформенного Android-модуля и инкапсуляция контекстов ОС")

  initKoin(
    platformModule = module {
      // Регистрируем нативный контекст операционной системы Android
      single<Context> { context }

      // Безопасно включаем дочерний андроид-модуль репозиториев
      includes(androidPlatformModule)
    }
  )
}

