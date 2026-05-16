package com.ykis.ykismobkmp.di

import android.content.Context
import androidx.preference.PreferenceManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepository
import com.ykis.ykismobkmp.data.preferences.AppSettingsRepositoryImpl
import com.ykis.ykismobkmp.db.DatabaseDriverFactory
import com.ykis.ykismobkmp.domain.ai.LocalAiEngine
import org.koin.core.module.Module
import org.koin.dsl.module

val androidPlatformModule: Module = module {
   single<Settings> {SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(get()))}
  single<AppSettingsRepository> {AppSettingsRepositoryImpl(dataStore = get())}
  single { DatabaseDriverFactory(get()) }
  single { LocalAiEngine() }
  single {androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(produceFile = {
        val context = get<android.content.Context>()
        context.filesDir.resolve("APP_SETTINGS.preferences_pb")
      }
    )
  }
}

fun initAndroidKoin(context: Context) {
  initKoin(
    platformModule = module {
       single<Context> { context }
      includes(androidPlatformModule)
    }
  )
}
