package com.ykis.ykismobkmp.data.preferences // Совпадает с интерфейсом

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AppSettingsRepositoryImpl(
  private val dataStore: DataStore<Preferences>
) : AppSettingsRepository { // Бесшовно реализует интерфейс из commonMain

  companion object {
    private val THEME_KEY = stringPreferencesKey("theme")
  }

  override fun observeTheme(): Flow<String> =
    dataStore.data.map { preferences -> preferences[THEME_KEY] ?: "system" }
      .distinctUntilChanged().flowOn(Dispatchers.Default)

  override suspend fun saveTheme(themeValue: String) {
    dataStore.edit { preferences -> preferences[THEME_KEY] = themeValue }
  }


}
