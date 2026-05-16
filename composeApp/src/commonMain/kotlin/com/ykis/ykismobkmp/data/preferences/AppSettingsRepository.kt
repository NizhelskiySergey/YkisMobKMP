package com.ykis.ykismobkmp.data.preferences

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
  fun observeTheme(): Flow<String>
  suspend fun saveTheme(themeValue: String)

}
